package ro.cs.tao.services.monitoring.controller;

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

    @SendTo("/topic/system")
    public Notification sendSystem(final Message message) {
        return convert(message);
    }

    @SendTo("/topic/info")
    public Notification sendInformation(final Message message) {
        return convert(message);
    }

    @SendTo("/topic/warn")
    public Notification sendWarning(final Message message) {
        return convert(message);
    }

    @SendTo("/topic/error")
    public Notification sendError(final Message message) {
        return convert(message);
    }

    @SendTo("/topic/progress")
    public Notification sendProgress(final Message message) {
        return convert(message);
    }

    @SendTo("/topic/execution")
    public Notification sendExecutionStatus(final Message message) {
        return convert(message);
    }

    @SendTo("/topic/topology")
    public Notification sendTopology(final Message message) {
        return convert(message);
    }

    @SendTo("/topic/transfer")
    public Notification sendTransferProgress(final Message message) {
        return convert(message);
    }

    private Notification convert(Message message) {
        return converter.to(message);
    }

    private static class Receiver extends Notifiable {
        private PushController theController;

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
            switch (topic) {
                case "system":
                    theController.sendSystem(message);
                    break;
                case "info":
                    theController.sendInformation(message);
                    break;
                case "warn":
                    theController.sendWarning(message);
                    break;
                case "error":
                    theController.sendError(message);
                    break;
                case "progress":
                    theController.sendProgress(message);
                    break;
                case "execution.status.changed":
                    theController.sendExecutionStatus(message);
                    break;
                case "topology":
                    theController.sendTopology(message);
                    break;
                case "transfer":
                    theController.sendTransferProgress(message);
                    break;
            }
        }
    }
}
