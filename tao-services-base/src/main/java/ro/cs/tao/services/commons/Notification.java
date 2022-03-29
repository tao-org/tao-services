/*
 * Copyright (C) 2018 CS ROMANIA
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package ro.cs.tao.services.commons;

import java.util.Calendar;

/**
 * @author Cosmin Cara
 */
public class Notification {
    private long id;
    private Calendar timestamp;
    private String user;
    private boolean read;
    private String source;
    private String topic;
    private String data;

    public Notification() { }

    public Notification(long id, Calendar timestamp, String user, String source, String topic, String data) {
        this.id = id;
        this.timestamp = timestamp;
        this.source = source;
        this.data = data;
        this.user = user;
        this.topic = topic;
        this.read = false;
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

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
}
