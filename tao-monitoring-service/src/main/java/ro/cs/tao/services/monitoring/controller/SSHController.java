package ro.cs.tao.services.monitoring.controller;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.json.JSONObject;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import ro.cs.tao.persistence.NodeDBProvider;
import ro.cs.tao.services.monitoring.impl.SSHSession;
import ro.cs.tao.services.monitoring.impl.SSHWebSocketHandler;
import ro.cs.tao.services.monitoring.impl.UpdateListener;
import ro.cs.tao.topology.NodeDescription;

import java.io.IOException;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Controller responsible for managing SSH sessions and communicating via WebSocket.
 * This class facilitates establishing new SSH sessions, sending and receiving messages,
 * resizing sessions, and closing sessions. It also integrates with WebSocketHandler for
 * managing text messages from WebSocket connections.
 *
 * Implements {@link DisposableBean} for lifecycle management, ensuring all resources are
 * cleaned up when the bean is destroyed.
 */
@RestController
@Tag(name ="SSH", description = "Endpoint for SSH terminal")
public class SSHController implements DisposableBean {

    private static final String SESSION_KEY_TERM_UUID = "TERM-UUID";

    @Autowired
    private NodeDBProvider nodeDBProvider;
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

    /**
     * Handles the resizing of a session's terminal window based on the provided dimensions.
     *
     * @param id the unique identifier of the session to be resized
     * @param payload the JSON-encoded string containing the new dimensions (e.g., columns and rows)
     * @return a JSON-formatted string indicating the request details or an error message if the session is not found
     * @throws IOException if an input/output error occurs during resizing
     * @throws InterruptedException if the thread handling the request is interrupted
     */
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

    /**
     * Closes an active SSH session associated with the given session identifier.
     * If a session matching the provided identifier is found, it is terminated.
     *
     * @param id the unique identifier of the session to be closed
     * @throws IOException if an input/output error occurs during the session termination
     * @throws InterruptedException if the operation is interrupted during the execution
     */
    @PostMapping(path = "/session/{id}/close")
    public void close(@PathVariable("id") String id) throws IOException, InterruptedException {
        SSHSession s = sessions.getIfPresent(UUID.fromString(id));
        if (s != null) {
            s.destroy();
            log.fine("SSH session " + id + " closed");
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

    /**
     * Handles incoming WebSocket messages for managing SSH sessions and communications.
     * Processes both textual data and JSON objects to execute specific operations such as
     * initiating new sessions, connecting to a target, or sending input to an active session.
     *
     * @param wss the WebSocket session associated with the client
     * @param message the incoming message received by the WebSocket
     * @param data the parsed content of the message, which can be a String or JSONObject
     *             to represent commands or event data
     * @throws IOException if an input/output error occurs while sending responses or processing the session
     */
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
                log.fine("SSH session " + s.getUUID() + " created");
                sendMessage(wss, "{ \"cause\": \"new-session\", \"sessionId\": \"" + s.getUUID() + "\"}");
            }
        } else if (data instanceof JSONObject) {
            JSONObject o = (JSONObject) data;
            String event = o.optString("event");
            if (event != null) {
                if (event.equals("connect")) {
                    NodeDescription node = nodeDBProvider.get(o.getString("target"));
                    if (node != null) {
                        ts.connect(node);
                    } else {
                        ts.connect(o.getString("target"));
                    }
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

    public WebSocketHandler getWsHandler() {
        return wsHandler;
    }

    /**
     * Sends a text-based message through a WebSocket session.
     *
     * @param wss the WebSocket session to which the message will be sent
     * @param msg the message content to be sent, specified as a character sequence
     * @throws IOException if an input/output error occurs during the message sending process
     */
    public void sendMessage(WebSocketSession wss, CharSequence msg) throws IOException {
        synchronized (wss) {
            wss.sendMessage(new TextMessage(msg));
        }
    }

}