package ro.cs.tao.services.monitoring.beans;

import java.util.Calendar;

/**
 * Represents a WebSocket notification containing various attributes such as
 * its unique identifier, timestamp, user information, read status, source,
 * topic, and associated data.
 *
 * This class is used to encapsulate notifications sent or received
 * via WebSocket connections. The notifications can pertain to specific
 * topics and are linked to a user and source. Additional structured or raw
 * data relevant to the notification can also be stored.
 *
 * Attributes:
 * - id: A unique identifier for the notification.
 * - timestamp: Timestamp indicating when the notification was created.
 * - user: The user associated with the notification.
 * - read: Boolean flag indicating whether the notification has been marked as read.
 * - source: The origin or source of the notification.
 * - topic: The topic corresponding to the notification content.
 * - data: Additional data or payload contained within the notification.
 *
 * Constructors:
 * - Default constructor initializes an empty notification object.
 * - Parameterized constructor initializes a notification with specified fields, defaulting
 *   the "read" status to false.
 *
 * Methods:
 * - Accessor and mutator methods for all attributes which include id, user, read, timestamp, source, topic,
 *   and data, allowing for retrieval and modification of notification properties.
 */
public class WSNotification {
    private long id;
    private Calendar timestamp;
    private String user;
    private boolean read;
    private String source;
    private String topic;
    private Object data;

    public WSNotification() { }

    public WSNotification(long id, Calendar timestamp, String user, String source, String topic, Object data) {
        this.id = id;
        this.timestamp = timestamp;
        this.user = user;
        this.read = false;
        this.source = source;
        this.topic = topic;
        this.data = data;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUser() { return user; }
    public void setUserId(String user) { this.user = user; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public Calendar getTimestamp() { return timestamp; }
    public void setTimestamp(Calendar timestamp) { this.timestamp = timestamp; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
}
