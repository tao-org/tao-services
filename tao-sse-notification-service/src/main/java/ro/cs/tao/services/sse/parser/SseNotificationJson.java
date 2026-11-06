package ro.cs.tao.services.sse.parser;

import ro.cs.tao.services.model.event.ExternalEventNotification;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class SseNotificationJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SseNotificationJson() {
    }

    /**
     * Obtain a Notification object from Redis stream fields
     * @param id - message ID
     * @param json - message json content
     * @return Notification object
     */
    public static ExternalEventNotification parse(String id, String json) {
        try {
            JsonNode root = MAPPER.readTree(json);

            String collection = root.path("collection").asText();
            String feature = root.path("feature").asText();
            JsonNode geometry = root.get("geometry");

            if (geometry == null || geometry.isNull()) {
                throw new IllegalArgumentException("Notification has no geometry");
            }

            return new ExternalEventNotification(id, collection, feature, geometry.toString());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Notifier notification", e);
        }
    }
}
