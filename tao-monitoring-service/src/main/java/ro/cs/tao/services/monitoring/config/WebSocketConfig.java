package ro.cs.tao.services.monitoring.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import ro.cs.tao.services.monitoring.controller.SSHController;
import ro.cs.tao.services.monitoring.impl.WebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private SSHController sshController;

    private static final String socksJsUrl = "https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js";

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new WebSocketHandler(), "/queue").setAllowedOriginPatterns("*").withSockJS().setClientLibraryUrl(socksJsUrl);
        registry.addHandler(sshController.getWsHandler(), "/ws-terminal").setAllowedOriginPatterns("*").withSockJS().setClientLibraryUrl(socksJsUrl);
    }
}
