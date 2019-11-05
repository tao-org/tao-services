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

import ro.cs.tao.messaging.Message;

import java.util.Calendar;

/**
 * @author Cosmin Cara
 */
public class MessageConverter implements Converter<Message, Notification> {
    @Override
    public Message from(Notification value) {
        if (value == null) {
            return null;
        }
        Message message = Message.create(value.getUser(), value.getSource(), value.getData());
        message.setTimestamp(value.getTimestamp().getTimeInMillis());
        message.setRead(value.isRead());
        message.setTopic(value.getTopic());
        return message;
    }

    @Override
    public Notification to(Message value) {
        if (value == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(value.getTimestamp());
        String user = value.getUser();
        String msg = value.getData();
        String topic = value.getTopic();
        Notification notification = new Notification(calendar, user, value.getItem(Message.SOURCE_KEY), topic, msg);
        notification.setRead(value.isRead());
        return notification;
    }
}
