package ro.cs.tao.services.monitoring.beans;

import com.fasterxml.jackson.core.JsonProcessingException;
import ro.cs.tao.messaging.Message;
import ro.cs.tao.messaging.progress.DownloadProgress;
import ro.cs.tao.serialization.JsonMapper;
import ro.cs.tao.services.commons.Converter;

import java.util.*;

/**
 * Converts between {@link Message} and {@link WSNotification} instances.
 * <p>
 * This class is responsible for translating a {@link Message} object into
 * a {@link WSNotification} and vice versa. The conversion process ensures
 * proper handling and filtering of relevant data fields.
 * <p>
 * During the transformation of a {@link Message} to a {@link WSNotification},
 * certain keys, defined in the {@code exclusions} set, will be excluded from
 * the final notification data. Additional processing is performed to ensure
 * nested JSON strings are deserialized into valid objects.
 */
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
        Map<String, Object> finalData = new HashMap<>();
        for (Map.Entry<String, String> entry : rawData.entrySet()) {
            final String entryValue = entry.getValue();
            try {
                if (entryValue != null) {
                    Iterator<String> iterator;
                    Map<String, Object> data = JsonMapper.instance().readValue(entryValue, finalData.getClass());
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
                    finalData.putAll(data);
                }
            } catch (Exception ignored) {
                final String key = entry.getKey();
                finalData.put(key != null ? key : "unknown", entryValue);
            }
            if (value instanceof DownloadProgress) {
                finalData.put("speed", ((DownloadProgress) value).getTransferSpeedMB());
                finalData.put("remaining", String.valueOf(((DownloadProgress) value).getRemaining()));
            }
        }
        WSNotification notification = new WSNotification(id , calendar, user, value.getItem(Message.SOURCE_KEY), topic, finalData);
        notification.setRead(value.isRead());
        return notification;
    }
}
