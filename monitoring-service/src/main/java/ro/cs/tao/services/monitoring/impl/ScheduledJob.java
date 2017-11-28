package ro.cs.tao.services.monitoring.impl;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ro.cs.tao.messaging.MessageBus;

/**
 * @author Cosmin Cara
 */
@Component
public class ScheduledJob {

    @Scheduled(fixedRate = 10000)
    public void dummyMessages() {
        MessageBus.send(1, MessageBus.INFORMATION, this, "Another dummy message");
    }
}
