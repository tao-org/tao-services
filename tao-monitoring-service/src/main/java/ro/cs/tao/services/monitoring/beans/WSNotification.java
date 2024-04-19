package ro.cs.tao.services.monitoring.beans;

import java.util.Calendar;

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
