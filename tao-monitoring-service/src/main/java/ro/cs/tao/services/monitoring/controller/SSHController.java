package ro.cs.tao.services.monitoring.controller;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import org.json.JSONObject;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import ro.cs.tao.services.monitoring.impl.SSHSession;
import ro.cs.tao.services.monitoring.impl.SSHWebSocketHandler;
import ro.cs.tao.services.monitoring.impl.UpdateListener;

import java.io.IOException;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@RestController
public class SSHController implements DisposableBean {

    private static final String SESSION_KEY_TERM_UUID = "TERM-UUID";

    private Logger log = Logger.getLogger(SSHController.class.getName());

    public final WebSocketHandler wsHandler = new SSHWebSocketHandler(this);

    private RemovalListener<UUID, SSHSession> removalListener() {
        return new RemovalListener<UUID, SSHSession>() {

            @Override
            public void onRemoval(UUID key, SSHSession value, RemovalCause cause) {
                value.destroy();
            }
        };
    }

    private Cache<UUID, SSHSession> sessions = (Cache<UUID, SSHSession>) Caffeine.newBuilder()
                                                                                 .expireAfterAccess(60, TimeUnit.MINUTES)
                                                                                 .removalListener(removalListener()).build();

    @PostMapping(path = "/session/{id}/resized")
    public @ResponseBody String resized(@PathVariable("id") String id, @RequestBody String payload) throws IOException, InterruptedException {
        SSHSession s = sessions.getIfPresent(UUID.fromString(id));
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

    @PostMapping(path = "/session/{id}/close")
    public void close(@PathVariable("id") String id) throws IOException, InterruptedException {
        SSHSession s = sessions.getIfPresent(UUID.fromString(id));
        if (s != null) {
            s.destroy();
        }

    }

    @Override
    public void destroy() throws Exception {
        sessions.invalidateAll();
        sessions.cleanUp();
    }

    private SSHSession newSession(UpdateListener updateListener, WebSocketSession wss) throws IOException {
        SSHSession session = new SSHSession(UUID.randomUUID(), updateListener, wss);
        sessions.put(session.getUUID(), session);
        return session;
    }

    public void handleMessage(WebSocketSession wss, TextMessage message, Object data) throws IOException {
        UUID uuid = (UUID) wss.getAttributes().get(SESSION_KEY_TERM_UUID);
        SSHSession ts = uuid == null ? null : sessions.getIfPresent(uuid);

        if (data instanceof String) {
            if (data.equals("new-session")) {
                Encoder encoder = Base64.getEncoder();
                SSHSession s = newSession(event->{
                    JSONObject o = new JSONObject();
                    if (event instanceof UpdateListener.BytesEvent) {
                        o.put("cause", "update");
                        o.put("b64", encoder.encodeToString(((UpdateListener.BytesEvent)event).bytes));
                    } else if (event instanceof UpdateListener.EofEvent) {
                        o.put("cause", "EOF");
                    } else if (event instanceof UpdateListener.ErrorEvent) {
                        log.severe("Error" +  ((UpdateListener.ErrorEvent) event).getError().getMessage());
                        o.put("cause", "error");
                    } else if (event instanceof UpdateListener.AskForPassword) {
                        o.put("cause", "ask-for-password");
                    }
                    o.put("stream", event.getStream());
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