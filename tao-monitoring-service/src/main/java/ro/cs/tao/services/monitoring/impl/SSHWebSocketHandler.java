package ro.cs.tao.services.monitoring.impl;

import org.json.JSONObject;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import ro.cs.tao.services.monitoring.controller.SSHController;

import java.util.logging.Logger;

public class SSHWebSocketHandler extends AbstractWebSocketHandler {

    private final SSHController controller;
    private final Logger logger;

    public SSHWebSocketHandler(SSHController controller) {
        super();
        this.logger = Logger.getLogger(SSHWebSocketHandler.class.getName());
        this.controller = controller;
    }

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
                    this.controller.handleMessage(session, message, o.get("d"));
                    return;
                }
            }
            this.controller.sendMessage(session, "{ \"error\": \"No handler for message\"}");
        } catch (Exception e) {
            this.logger.severe(e.getMessage());
            this.controller.sendMessage(session, "{ \"error\": \"Internal error\"}");
        }
    }
}
