package ro.cs.tao.services.commons;

import ro.cs.tao.messaging.Message;

import java.util.Calendar;

/**
 * @author Cosmin Cara
 */
public class MessageConverter implements Converter<Message, ServiceMessage> {
    @Override
    public Message from(ServiceMessage value) {
        if (value == null) {
            return null;
        }
        Message message = Message.create(value.getUser(), value.getSource(), value.getData());
        message.setTimestamp(value.getTimestamp().getTimeInMillis());
        message.setRead(value.isRead());
        return message;
    }

    @Override
    public ServiceMessage to(Message value) {
        if (value == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(value.getTimestamp());
        String user = value.getUser();
        String msg = value.getData();
        ServiceMessage serviceMessage = new ServiceMessage(calendar, user, value.getItem(Message.SOURCE_KEY), msg);
        serviceMessage.setRead(value.isRead());
        return serviceMessage;
    }
}
