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
 * A utility class responsible for converting objects of type {@link Message} to objects of
 * type {@link Notification} and vice versa.
 *
 * This class implements the {@link Converter} interface and provides functionality to
 * transfer data between the {@link Message} and {@link Notification} objects, preserving
 * their data integrity while adapting their format as per the requirements of each type.
 *
 * The {@code MessageConverter} ensures the mapping of properties between the two types,
 * such as timestamps, user details, topic, and message data. It also handles null values
 * gracefully, ensuring that neither type faces NullPointerException during conversions.
 *
 * @author Cosmin Cara
 */
public class MessageConverter implements Converter<Message, Notification> {
    @Override
    public Message from(Notification value) {
        if (value == null) {
            return null;
        }
        Message message = Message.create(value.getUser(), value.getSource(), value.getData());
        message.setId(value.getId());
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
        String user = value.getUserId();
        String msg = value.getData();
        String topic = value.getTopic();
        final long id = value.getId() != null ? value.getId() : calendar.getTimeInMillis();
        Notification notification = new Notification(id ,calendar, user, value.getItem(Message.SOURCE_KEY), topic, msg);
        notification.setRead(value.isRead());
        return notification;
    }
}
