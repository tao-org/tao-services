package ro.cs.tao.services.monitoring.controller;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.auth.password.PasswordIdentityProvider;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.common.session.SessionContext;
import org.json.JSONObject;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.configuration.ConfigurationProvider;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@RestController
public class SSHController implements DisposableBean {

    private static final String SESSION_KEY_TERM_UUID = "TERM-UUID";

    private Logger log = Logger.getLogger(SSHController.class.getName());

    public final WebSocketHandler wsHandler = new AbstractWebSocketHandler() {

        @Override
        public boolean supportsPartialMessages() {
            return false;
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
            try {
                String s = message.getPayload();
                if (s.startsWith("{")) {
                    JSONObject o = new JSONObject(s);
                    String to = o.getString("to");
                    if (to.equals("tm")) {
                        SSHController.this.handleMessage(session, message, o.get("d"));
                        return;
                    }
                }
                sendMessage(session, "{ \"error\": \"No handler for message\"}");
            } catch (Exception e) {
                log.severe(e.getMessage());
                sendMessage(session, "{ \"error\": \"Internal error\"}");
            }
        }
    };

    public interface UpdateListener {
        public class AskForPassword extends Event {
            public AskForPassword() {
                super(null);
            }
        }
        public enum Stream { STDOUT, STDERR };
        public static abstract class Event {
            final Stream stream;
            public Event(Stream stream) {
                this.stream = stream;
            }
        };
        public static class BytesEvent extends Event {
            public final byte[] bytes;
            public BytesEvent(Stream stream, byte[] bytes) {
                super(stream);
                this.bytes = bytes;
            }
        }
        public static class EofEvent extends Event {
            public EofEvent(Stream stream) {
                super(stream);
            }
        }
        public static class ErrorEvent extends Event {
            private Exception error;
            public ErrorEvent(Exception error) {
                super(null);
                this.error = error;
            }
        }
        public void update(Event bytesEvent);
    }

    public class TermSession {
        private class StreamOutputStream extends OutputStream {

            private UpdateListener.Stream streamId;

            public StreamOutputStream(UpdateListener.Stream streamId) {
                this.streamId = streamId;
            }

            @Override
            public void write(int b) throws IOException {
                this.write(new byte[] { (byte) b });
            }

            @Override
            public void write(byte[] b) throws IOException {
                this.write(b, 0, b.length);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                byte[] bytes = new byte[len];
                System.arraycopy(b, off, bytes, 0, len);
                updateListener.update(new UpdateListener.BytesEvent(streamId, bytes));
            }

            @Override
            public void flush() throws IOException {
            }

            @Override
            public void close() throws IOException {
                updateListener.update(new UpdateListener.EofEvent(streamId));
            }
        }

        private final UUID uuid;
        private final SshClient client;
        private final UpdateListener updateListener;
        private final WebSocketSession wss;
        private volatile CompletableFuture<ClientSession> sshSession = null;
        private volatile ChannelShell shellChannel;

        public TermSession(UUID uuid, UpdateListener updateConsumer, WebSocketSession wss) throws IOException {
            this.uuid = uuid;
            this.client = SshClient.setUpDefaultClient();
            this.updateListener = updateConsumer;
            this.wss = wss;
            this.client.start();
        }

        public void connect(String target) throws IOException {
            sendTextToTerminal(UpdateListener.Stream.STDERR, "Connecting to " + target + "...\r\n");
            sshSession = new CompletableFuture<>();
            String[] targetSplit = target.split(":", 2);
            String hostname = targetSplit[0];
            Integer port = Optional.of(targetSplit).filter(a->a.length > 1).map(a->Integer.valueOf(a[1])).orElse(22);
            final ConfigurationProvider configurationProvider = ConfigurationManager.getInstance();
            final String user = configurationProvider.getValue("topology.master.user");
            final String keyPath = configurationProvider.getValue("topology.node.ssh.key");
            client.setKeyIdentityProvider(new FileKeyPairProvider(Path.of(keyPath)));
            client.connect(user, hostname, port).addListener(cf->{
                try {
                    cf.verify();
                } catch (IOException e) {
                    this.sshSession.completeExceptionally(e);
                    this.client.stop();
                    this.updateListener.update(new UpdateListener.ErrorEvent(e));
                }
                ClientSession session = cf.getSession();
                this.sshSession.complete(session);
                CompletableFuture<Void> authenticaded = new CompletableFuture<Void>();
                try {
                    session.auth().addListener(authFuture->{
                        try {
                            authFuture.verify();
                            this.shellChannel = session.createShellChannel();
                            shellChannel.setPtyType("xterm-256color");
                            shellChannel.setOut(new StreamOutputStream(UpdateListener.Stream.STDOUT));
                            shellChannel.setErr(new StreamOutputStream(UpdateListener.Stream.STDERR));
                            shellChannel.open().addListener(o->{
                                try {
                                    if (o.getException() != null) {
                                        throw o.getException();
                                    }
                                    o.verify();
                                    if (o.isOpened()) {
                                        log.info("Channel opened");
                                    } else {
                                        throw new IOException("Channel not opened");
                                    }
                                } catch (Throwable e) {
                                    authenticaded.completeExceptionally(e);
                                }
                                authenticaded.complete(null);
                            });
                        } catch (Exception e) {
                            authenticaded.completeExceptionally(e);
                        }
                    });
                } catch (IOException e) {
                    authenticaded.completeExceptionally(e);
                }
                authenticaded.handle((r, e) -> {
                    if (e == null) {
                        log.info("Authenticated!");
                        sendTextToTerminal(UpdateListener.Stream.STDERR, "Authenticated successfuly.\r\n");
                    } else {
                        log.severe("Not authenticated: " + e.getMessage());
                        sendTextToTerminal(UpdateListener.Stream.STDERR, "Error authenticating.\r\n");
                    }
                    return null;
                });
                //askForPassword();
            });
        }

        public void enteredPassword(String password) {
            PasswordIdentityProvider provider = new PasswordIdentityProvider() {

                @Override
                public Iterable<String> loadPasswords(SessionContext session) throws IOException, GeneralSecurityException {
                    return Arrays.asList(password);
                }
            };
            CompletableFuture<Void> authenticaded = new CompletableFuture<Void>();
            this.sshSession.thenAccept(s -> {
                s.setPasswordIdentityProvider(provider);
                try {
                    s.auth().addListener(authFuture->{
                        try {
                            authFuture.verify();
                            this.shellChannel = s.createShellChannel();
                            shellChannel.setPtyType("xterm-256color");
                            shellChannel.setOut(new StreamOutputStream(UpdateListener.Stream.STDOUT));
                            shellChannel.setErr(new StreamOutputStream(UpdateListener.Stream.STDERR));
                            shellChannel.open().addListener(o->{
                                try {
                                    if (o.getException() != null) {
                                        throw o.getException();
                                    }
                                    o.verify();
                                    if (o.isOpened()) {
                                        log.info("Channel opened");
                                    } else {
                                        throw new IOException("Channel not opened");
                                    }
                                } catch (Throwable e) {
                                    authenticaded.completeExceptionally(e);
                                }
                                authenticaded.complete(null);
                            });
                        } catch (Exception e) {
                            authenticaded.completeExceptionally(e);
                        }
                    });
                } catch (IOException e) {
                    authenticaded.completeExceptionally(e);
                }
                authenticaded.handle((r, e) -> {
                    if (e == null) {
                        log.info("Authenticated!");
                        sendTextToTerminal(UpdateListener.Stream.STDERR, "Authenticated successfuly.\r\n");
                    } else {
                        log.severe("Not authenticated: " + e.getMessage());
                        sendTextToTerminal(UpdateListener.Stream.STDERR, "Error authenticating.\r\n");
                    }
                    return null;
                });
            });
        }

        private void sendTextToTerminal(UpdateListener.Stream stderr, String text) {
            updateListener.update(new UpdateListener.BytesEvent(stderr, text.getBytes(StandardCharsets.UTF_8)));
        }

        public UUID getUUID() {
            return uuid;
        }

        public void write(String string) throws IOException {
            Optional.ofNullable(shellChannel).ifPresent(x->{
                try {
                    byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
                    OutputStream invertedIn = x.getInvertedIn();
                    invertedIn.write(bytes);
                    invertedIn.flush();
                } catch (IOException e) {
                    log.severe("Error resizing pty: " + e.getMessage());
                }
            });
        }

        public void resized(int cols, int rows) {
            Optional.ofNullable(shellChannel).ifPresent(x->{
                try {
                    x.sendWindowChange(cols, rows);
                } catch (IOException e) {
                    log.severe("Error resizing pty" + e.getMessage());
                }
            });
        }

        public boolean isClosed() {
            return Optional.ofNullable(shellChannel).map(x->x.isClosed()).orElse(false);
        }

        public WebSocketSession getWss() {
            return wss;
        }

        private void askForPassword() {
            updateListener.update(new UpdateListener.AskForPassword());
        }

        public void destroy() {
            Optional.ofNullable(shellChannel).ifPresent(x->x.close(true));
        }
    }

    private RemovalListener<UUID, TermSession> removalListener() {
        return new RemovalListener<UUID, SSHController.TermSession>() {

            @Override
            public void onRemoval(UUID key, TermSession value, RemovalCause cause) {
                value.destroy();
            }
        };
    }

    private Cache<UUID, TermSession> sessions = (Cache<UUID, TermSession>) Caffeine.newBuilder()
                                                                                   .expireAfterAccess(60, TimeUnit.MINUTES).removalListener(removalListener()).build();

    @PostMapping(path = "/session/{id}/resized")
    public @ResponseBody String resized(@PathVariable("id") String id, @RequestBody String payload) throws IOException, InterruptedException {
        TermSession s = sessions.getIfPresent(UUID.fromString(id));
        if (s == null) {
            JSONObject res = new JSONObject();
            res.put("error", "Session not found: " + id);
            return res.toString(2);
        } else {
            JSONObject req = new JSONObject(payload);
            JSONObject res = new JSONObject();
            res.put("req", req);
            s.resized(req.getInt("cols"), req.getInt("rows"));
            return res.toString(2);
        }

    }

    @Override
    public void destroy() throws Exception {
        sessions.invalidateAll();
        sessions.cleanUp();
    }

    private TermSession newSession(UpdateListener updateListener, WebSocketSession wss) throws IOException {
        TermSession session = new TermSession(UUID.randomUUID(), updateListener, wss);
        sessions.put(session.getUUID(), session);
        return session;
    }

    public void handleMessage(WebSocketSession wss, TextMessage message, Object data) throws IOException {
        UUID uuid = (UUID) wss.getAttributes().get(SESSION_KEY_TERM_UUID);
        TermSession ts = uuid == null ? null : sessions.getIfPresent(uuid);

        if (data instanceof String) {
            if (data.equals("new-session")) {
                Encoder encoder = Base64.getEncoder();
                TermSession s = newSession(event->{
                    JSONObject o = new JSONObject();
                    if (event instanceof UpdateListener.BytesEvent) {
                        o.put("cause", "update");
                        o.put("b64", encoder.encodeToString(((UpdateListener.BytesEvent)event).bytes));
                    } else if (event instanceof UpdateListener.EofEvent) {
                        o.put("cause", "EOF");
                    } else if (event instanceof UpdateListener.ErrorEvent) {
                        log.severe("Error" +  ((UpdateListener.ErrorEvent) event).error.getMessage());
                        o.put("cause", "error");
                    } else if (event instanceof UpdateListener.AskForPassword) {
                        o.put("cause", "ask-for-password");
                    }
                    o.put("stream", event.stream);
                    try {
                        sendMessage(wss, o.toString());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, wss);
                wss.getAttributes().put(SESSION_KEY_TERM_UUID, s.getUUID());
                sendMessage(wss, "{ \"cause\": \"new-session\", \"sessionId\": \"" + s.getUUID().toString() + "\"}");
            }
        } else if (data instanceof JSONObject) {
            JSONObject o = (JSONObject) data;
            String event = o.optString("event");
            if (event != null) {
                if (event.equals("connect")) {
                    ts.connect(o.getString("target"));
                } else if (event.equals("type")) {
                    if (ts == null) {
                        synchronized (wss) {
                            sendMessage(wss, "{ \"error\": \"Session needed to type\"}");
                        }
                    } else {
                        if (ts.isClosed()) {
                            synchronized (wss) {
                                sendMessage(wss, "{ \"error\": \"Session closed\"}");
                            }
                        } else {
                            String text = o.getString("text");
                            ts.write(text);
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("unused")
    private String debugBytes(byte[] d) {
        StringBuilder sb = new StringBuilder(d.length * 3);
        for (byte b: d) {
            sb.append(String.format("%02x ", b & 0xFF));
        }
        return sb.toString();
    }

    public WebSocketHandler getWsHandler() {
        return wsHandler;
    }

    public void sendMessage(WebSocketSession wss, CharSequence msg) throws IOException {
        synchronized (wss) {
            wss.sendMessage(new TextMessage(msg));
        }
    }

}