package ro.cs.tao.services.monitoring.impl;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ro.cs.tao.messaging.Messaging;
import ro.cs.tao.messaging.Topics;
import ro.cs.tao.security.SystemPrincipal;

/**
 * @author Cosmin Cara
 */
@Component
public class ScheduledJob {

    @Scheduled(fixedRate = 10000)
    public void dummyMessages() {
        Messaging.send(SystemPrincipal.instance(), Topics.INFORMATION, this, "Another dummy message");
    }
}
