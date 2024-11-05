package ro.cs.tao.services.monitoring.impl;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.auth.password.PasswordIdentityProvider;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.common.session.SessionContext;
import org.springframework.web.socket.WebSocketSession;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.configuration.ConfigurationProvider;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class SSHSession {

    private final UUID uuid;
    private final SshClient client;
    private final UpdateListener updateListener;
    private final WebSocketSession wss;
    private final Logger log = Logger.getLogger(SSHSession.class.getName());
    private volatile CompletableFuture<ClientSession> sshSession = null;
    private volatile ChannelShell shellChannel;

    public SSHSession(UUID uuid, UpdateListener updateConsumer, WebSocketSession wss) throws IOException {
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
                                    log.finest("Channel opened");
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
                    log.finest("Authenticated!");
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
                                    log.finest("Channel opened");
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
                    log.finest("Authenticated!");
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
}
