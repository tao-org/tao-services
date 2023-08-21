package ro.cs.tao.services.monitoring.beans;

import com.fasterxml.jackson.core.JsonProcessingException;
import ro.cs.tao.messaging.Message;
import ro.cs.tao.messaging.progress.DownloadProgress;
import ro.cs.tao.serialization.JsonMapper;
import ro.cs.tao.services.commons.Converter;

import java.util.*;

public class MessageConverter implements Converter<Message, WSNotification> {
    private final Set<String> exclusions = new HashSet<>() {{
       add(Message.SOURCE_KEY); add(Message.PRINCIPAL_KEY); add(Message.TOPIC_KEY);
    }};

    @Override
    public Message from(WSNotification value) {
        return null;
    }

    @Override
    public WSNotification to(Message value) {
        if (value == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(value.getTimestamp());
        String user = value.getUserId();
        String msg = value.getData();
        String topic = value.getTopic();
        final long id = value.getId() != null && value.getId() != 0 ? value.getId() : calendar.getTimeInMillis();
        Map<String, Object> data = new HashMap<>();
        try {
            data = JsonMapper.instance().readValue(msg, data.getClass());
            final Iterator<String> iterator = data.keySet().iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                if (exclusions.contains(key)) {
                    iterator.remove();

                } else {
                    Object val = data.get(key);
                    if (val instanceof String) {
                        try {
                            Map<String, Object> map = new HashMap<>();
                            map = JsonMapper.instance().readerFor(map.getClass()).readValue((String) val);
                            data.replace(key, map);
                        } catch (JsonProcessingException ignored) {
                        }
                    }
                }
            }
            if (value instanceof DownloadProgress) {
                data.put("speed", ((DownloadProgress) value).getTransferSpeedMB());
                data.put("remaining", String.valueOf(((DownloadProgress) value).getRemaining()));
            }
        } catch (Exception ignored) {
            data.put(Message.MESSAGE_KEY, msg);
        }
        WSNotification notification = new WSNotification(id , calendar, user, value.getItem(Message.SOURCE_KEY), topic, data);
        notification.setRead(value.isRead());
        return notification;
    }
}
