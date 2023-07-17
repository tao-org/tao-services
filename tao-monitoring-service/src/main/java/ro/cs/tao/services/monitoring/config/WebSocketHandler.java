package ro.cs.tao.services.monitoring.config;

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
import ro.cs.tao.services.commons.MessageConverter;
import ro.cs.tao.services.commons.Notification;
import ro.cs.tao.services.interfaces.AdministrationService;
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
    private static final Set<String> subscriptions;
    private static AdministrationService administrationService;

    private static final Object lock;

    private final Logger logger = Logger.getLogger(WebSocketHandler.class.getName());

    static {
        lock = new Object();
        sessions = Collections.synchronizedList(new ArrayList<>());
        admins = new HashSet<>();
        subscriptions = new HashSet<>();
        adminRefreshTimer = new Timer();
        adminRefreshTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (lock) {
                    admins.clear();
                    admins.addAll(getAdminService().getAdministrators().stream().map(User::getUsername).collect(Collectors.toSet()));
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
            admins.addAll(getAdminService().getAdministrators().stream().map(User::getUsername).collect(Collectors.toSet()));
        }
        sessions.add(session);
        logger.fine(() -> String.format("Websocket connection established for principal [%s]", principal.getName()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        sessions.remove(session);
        final Principal principal = session.getPrincipal();
        if (principal != null) {
            subscriptions.remove(principal.getName());
            logger.fine(() -> String.format("Websocket connection closed for principal [%s]", principal.getName()));
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
                subscriptions.add(topic);
                logger.fine(() -> String.format("Websocket subscription created [principal: %s, topic: %s]",
                                                principal.getName(), topic));
                break;
            case "UNSUBSCRIBE":
                destination = Arrays.stream(lines).filter(l -> l.toLowerCase().startsWith("destination")).findFirst().orElse(null);
                if (destination == null) {
                    throw new Exception("Destination not set");
                }
                topic = destination.split(":")[1].trim();
                subscriptions.remove(topic);
                logger.fine(() -> String.format("Websocket subscription removed [principal: %s, topic: %s]",
                                                principal.getName(), topic));
                break;
            default:
                super.handleTextMessage(session, message);
                break;
        }
    }

    void send(Notification message) {
        String user = message.getUser();
        try {
            String strMessage = new ObjectMapper().writeValueAsString(message);
            WebSocketSession session = findSession(user);
            final TextMessage txtMsg = new TextMessage(strMessage);
            if (session != null && subscriptions.contains("/queue/" + user)) {
                session.sendMessage(txtMsg);
            }
            synchronized (admins) {
                for (String u : admins) {
                    if (subscriptions.contains("/queue/" + u)
                            && !user.equals(u) && (session = findSession(u)) != null) {
                        session.sendMessage(txtMsg);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private synchronized WebSocketSession findSession(String user) {
        return sessions.stream()
                       .filter(s -> s.getPrincipal() != null &&
                                    s.getPrincipal().getName().equals(user)).findFirst()
                       .orElse(null);
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
            this.handler.send(converter.to(message));
        }
    }
}
