package ro.cs.tao.services.monitoring.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ro.cs.tao.messaging.Message;
import ro.cs.tao.messaging.Notifiable;
import ro.cs.tao.messaging.Topic;
import ro.cs.tao.services.bridge.spring.SpringContextBridge;
import ro.cs.tao.services.interfaces.AdministrationService;
import ro.cs.tao.services.monitoring.beans.MessageConverter;
import ro.cs.tao.services.monitoring.beans.WSNotification;
import ro.cs.tao.user.User;

import javax.inject.Singleton;
import java.io.IOException;
import java.security.Principal;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Singleton
@Component
public class WebSocketHandler extends TextWebSocketHandler {
    private static final List<WebSocketSession> sessions;
    private static final Set<String> admins;
    private static final Timer adminRefreshTimer;
    private static final Map<String, List<WebSocketSession>> subscriptions;
    private static final Set<Long> handledMessages;
    private static AdministrationService administrationService;

    private static final Object lock;

    private final Logger logger = Logger.getLogger(WebSocketHandler.class.getName());

    static {
        lock = new Object();
        sessions = Collections.synchronizedList(new ArrayList<>());
        admins = new HashSet<>();
        subscriptions = new HashMap<>();
        handledMessages = new HashSet<>();
        adminRefreshTimer = new Timer();
        adminRefreshTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (lock) {
                    handledMessages.clear();
                    admins.clear();
                    admins.addAll(getAdminService().getAdministrators().stream().map(User::getId).collect(Collectors.toSet()));
                }
            }
        }, 30000, 30000);
    }

    private static AdministrationService getAdminService() {
        if (administrationService == null) {
            administrationService = SpringContextBridge.services().getService(AdministrationService.class);
        }
        return administrationService;
    }

    public WebSocketHandler() {
        MessageReceiver receiver = new MessageReceiver(this);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        final Principal principal = session.getPrincipal();
        if (principal == null) {
            throw new Exception("Not authorized");
        }
        super.afterConnectionEstablished(session);
        if (sessions.isEmpty()) {
            admins.addAll(getAdminService().getAdministrators().stream().map(User::getId).collect(Collectors.toSet()));
        }
        sessions.add(session);
        logger.fine(() -> String.format("Websocket connection %s established for principal [%s]",
                                        session.getId(),
                                        principal.getName()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        final Principal principal = session.getPrincipal();
        if (principal != null) {
            final String topic = "/queue/" + principal.getName();
            removeSession(session.getId(), topic);
            logger.fine(() -> String.format("Websocket connection %s closed for principal [%s]",
                                            session.getId(),
                                            principal.getName()));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        final String payload = message.getPayload();
        final String[] lines = payload.split("\n");
        final Principal principal = session.getPrincipal();
        if (principal == null) {
            throw new Exception("Not authorized");
        }
        String destination, topic;
        switch (lines[0]) {
            case "SUBSCRIBE":
                destination = Arrays.stream(lines).filter(l -> l.toLowerCase().startsWith("destination")).findFirst().orElse(null);
                if (destination == null) {
                    throw new Exception("Destination not set");
                }
                topic = destination.split(":")[1].trim();
                if (!subscriptions.containsKey(topic)) {
                    subscriptions.put(topic, new ArrayList<>());
                }
                subscriptions.get(topic).add(session);
                logger.fine(() -> String.format("Websocket subscription created [principal: %s, topic: %s]",
                                                principal.getName(), topic));
                break;
            case "UNSUBSCRIBE":
                destination = Arrays.stream(lines).filter(l -> l.toLowerCase().startsWith("destination")).findFirst().orElse(null);
                if (destination == null) {
                    throw new Exception("Destination not set");
                }
                topic = destination.split(":")[1].trim();
                removeSession(session.getId(), topic);
                logger.fine(() -> String.format("Websocket subscription removed [principal: %s, topic: %s]",
                                                principal.getName(), topic));
                break;
            default:
                super.handleTextMessage(session, message);
                break;
        }
    }

    void send(WSNotification message) {
        if (subscriptions.isEmpty()) {
            return;
        }
        String user = message.getUser();
        try {
            String topic = "/queue/" + user;
            String strMessage = new ObjectMapper().writeValueAsString(message);
            final TextMessage txtMsg = new TextMessage(strMessage);
            List<WebSocketSession> userSessions = subscriptions.get(topic);
            if (userSessions != null) {
                final Iterator<WebSocketSession> iterator = userSessions.iterator();
                while (iterator.hasNext()) {
                    WebSocketSession session = iterator.next();
                    if (session.isOpen()) {
                        session.sendMessage(txtMsg);
                    } else {
                        logger.warning("Session " + session.getId() + " is closed and it will be removed.");
                        iterator.remove();
                    }
                }
            }
            synchronized (admins) {
                for (String u : admins) {
                    topic = "/queue/" + u;
                    if ((userSessions = subscriptions.get(topic)) != null && !user.equals(u)) {
                        final Iterator<WebSocketSession> iterator = userSessions.iterator();
                        while (iterator.hasNext()) {
                            WebSocketSession session = iterator.next();
                            if (session.isOpen()) {
                                session.sendMessage(txtMsg);
                            } else {
                                logger.warning("Admin session " + session.getId() + " is closed and it will be removed.");
                                iterator.remove();
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void removeSession(String sessionId, String topic) {
        final List<WebSocketSession> userSessions = subscriptions.get(topic);
        if (userSessions != null) {
            userSessions.removeIf(s -> s.getId().equals(sessionId));
            if (userSessions.isEmpty()) {
                subscriptions.remove(topic);
            }
        }
        if (userSessions == null || userSessions.isEmpty()) {
            subscriptions.remove(topic);
        }
    }

    public static class MessageReceiver extends Notifiable {
        private final WebSocketHandler handler;
        private final MessageConverter converter;

        public MessageReceiver(WebSocketHandler handler) {
            super();
            this.handler = handler;
            this.converter = new MessageConverter();
            List<String> topics = Topic.listTopics();
            for (String topic : topics) {
                subscribe(topic);
            }
        }


        @Override
        protected void onMessageReceived(Message message) {
            final Long id = message.getId();
            if (id != null) {
                synchronized (lock) {
                    if (!handledMessages.contains(id)) {
                        handledMessages.add(id);
                        if (message.getMessage() != null) {
                            message.setMessage(message.getMessage().replace("\"", "\\\""));
                        }
                        this.handler.send(converter.to(message));
                    } else {
                        handledMessages.remove(id);
                    }
                }
            }
        }
    }
}
