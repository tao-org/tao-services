package ro.cs.tao.services.commons;

import java.util.Calendar;

/**
 * @author Cosmin Cara
 */
public class ServiceMessage {
    private Calendar timestamp;
    private int userId;
    private boolean read;
    private String source;
    private String data;

    public ServiceMessage() { }

    public ServiceMessage(Calendar timestamp, int userId, String source, String data) {
        this.timestamp = timestamp;
        this.source = source;
        this.data = data;
        this.userId = userId;
        this.read = false;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public Calendar getTimestamp() { return timestamp; }
    public void setTimestamp(Calendar timestamp) { this.timestamp = timestamp; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
}
