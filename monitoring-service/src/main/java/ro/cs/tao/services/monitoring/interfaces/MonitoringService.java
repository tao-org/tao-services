package ro.cs.tao.services.monitoring.interfaces;

import ro.cs.tao.messaging.Message;
import ro.cs.tao.services.monitoring.model.Snapshot;

import java.util.List;

/**
 * @author Cosmin Cara
 */
public interface MonitoringService {

    Snapshot getMasterSnapshot();

    List<Message> getLiveNotifications();

    List<Message> getNotifications(int userId, int page);

    List<Message> acknowledgeNotification(List<Message> notifications);

}
