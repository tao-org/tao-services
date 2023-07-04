package ro.cs.tao.services.monitoring.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.RestController;
import ro.cs.tao.messaging.Message;
import ro.cs.tao.messaging.Notifiable;
import ro.cs.tao.messaging.Topic;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.MessageConverter;
import ro.cs.tao.services.commons.Notification;

import java.util.List;

@RestController
public class PushController extends BaseController {
    private final Receiver receiver = new Receiver(this);
    private final MessageConverter converter = new MessageConverter();

    @MessageMapping("/messages/{topic}")
    @SendTo("/topic/messages")
    public Notification send(@DestinationVariable("topic") String topic, final Message message) {
        return convert(message);
    }

    private Notification convert(Message message) {
        return converter.to(message);
    }

    private static class Receiver extends Notifiable {
        private final PushController theController;

        public Receiver(PushController controller) {
            super();
            this.theController = controller;
            List<String> topics = Topic.listTopics();
            for (String topic : topics) {
                subscribe(topic);
            }
        }

        @Override
        protected void onMessageReceived(Message message) {
            String topic = message.getTopic().toLowerCase();
            theController.send(topic, message);
        }
    }
}
