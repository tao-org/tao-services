package ro.cs.tao.services.monitoring.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import ro.cs.tao.services.monitoring.controller.SSHController;
import ro.cs.tao.services.monitoring.impl.TaskOutputWebSocketHandler;
import ro.cs.tao.services.monitoring.impl.WebSocketHandler;

/**
 * Configuration class for setting up WebSocket functionality in the application.
 * This class enables WebSocket features and defines the mappings and handlers
 * necessary to manage WebSocket communication.
 *
 * This configuration uses SockJS as a fallback option for clients that do not support
 * native WebSocket connections. It also integrates with SSHController to handle
 * WebSocket interactions specific to SSH sessions.
 *
 * Implements the {@link WebSocketConfigurer} interface to customize the WebSocket
 * endpoint registration.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private SSHController sshController;
    @Autowired
    private TaskOutputWebSocketHandler taskOutputHandler;

    private static final String socksJsUrl = "https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js";

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new WebSocketHandler(), "/queue").setAllowedOriginPatterns("*").withSockJS().setClientLibraryUrl(socksJsUrl);
        registry.addHandler(sshController.getWsHandler(), "/ws-terminal").setAllowedOriginPatterns("*").withSockJS().setClientLibraryUrl(socksJsUrl);
        registry.addHandler(taskOutputHandler, "/ws-task-output").setAllowedOriginPatterns("*").withSockJS().setClientLibraryUrl(socksJsUrl);
    }
}
