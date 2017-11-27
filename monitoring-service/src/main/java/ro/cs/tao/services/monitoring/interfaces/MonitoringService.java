package ro.cs.tao.services.monitoring.interfaces;

import ro.cs.tao.messaging.Message;
import ro.cs.tao.services.monitoring.model.Snapshot;

/**
 * @author Cosmin Cara
 */
public interface MonitoringService {

    Snapshot getMasterSnapshot();

    Message[] getNotifications();

}
