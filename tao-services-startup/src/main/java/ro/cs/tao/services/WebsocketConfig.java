package ro.cs.tao.services;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import ro.cs.tao.messaging.Topic;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebsocketConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        final List<String> topics = Topic.listTopics();
        for (String topic : topics) {
            registry.enableSimpleBroker("/topic/" + topic.toLowerCase());
        }
    }

}
