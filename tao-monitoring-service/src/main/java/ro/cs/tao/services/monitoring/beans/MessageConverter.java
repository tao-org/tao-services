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
        String topic = value.getTopic();
        final long id = value.getId() != null && value.getId() != 0 ? value.getId() : calendar.getTimeInMillis();
        final Map<String, String> rawData = value.getRawData();
        rawData.keySet().removeAll(exclusions);
        Map<String, Object> data = new HashMap<>();
        for (Map.Entry<String, String> entry : rawData.entrySet()) {
            final String entryValue = entry.getValue();
            try {
                if (entryValue != null) {
                    Iterator<String> iterator;
                    data = JsonMapper.instance().readValue(entryValue, data.getClass());
                    iterator = data.keySet().iterator();
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
                }
            } catch (Exception ignored) {
                final String key = entry.getKey();
                data = new HashMap<>();
                data.put(key != null ? key : "unknown", entryValue);
            }
            if (value instanceof DownloadProgress) {
                data.put("speed", ((DownloadProgress) value).getTransferSpeedMB());
                data.put("remaining", String.valueOf(((DownloadProgress) value).getRemaining()));
            }
        }
        WSNotification notification = new WSNotification(id , calendar, user, value.getItem(Message.SOURCE_KEY), topic, data);
        notification.setRead(value.isRead());
        return notification;
    }
}
