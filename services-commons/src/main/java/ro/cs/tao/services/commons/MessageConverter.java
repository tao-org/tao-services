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
        Message message = new Message(value.getTimestamp().getTimeInMillis(),
                                      value.getUserId(),
                                      value.getSource(),
                                      value.getData());
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
        ServiceMessage serviceMessage = new ServiceMessage(calendar, value.getUserId(), value.getSource(), value.getData());
        serviceMessage.setRead(value.isRead());
        return serviceMessage;
    }
}
