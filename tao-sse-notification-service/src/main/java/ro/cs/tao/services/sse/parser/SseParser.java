package ro.cs.tao.services.sse.parser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class SseParser {

    private final Consumer<SseEvent> eventConsumer;

    public SseParser(Consumer<SseEvent> eventConsumer) {
        this.eventConsumer = eventConsumer;
    }

    /**
     * Parse Redis stream
     *
     * @param input - redis stream message
     * @throws Exception
     */
    public void parse(InputStream input) throws Exception {

        String event = null;
        String id = null;
        StringBuilder data = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) {
                if (event != null || data.length() > 0) {
                    eventConsumer.accept(new SseEvent(event, id, data.toString()));
                }

                event = null;
                id = null;
                data.setLength(0);

                continue;
            }

            if (line.startsWith(":")) {
                // SSE comment / keepalive.
                continue;
            }

            int separator = line.indexOf(':');

            String field;
            String value;

            if (separator < 0) {
                field = line;
                value = "";
            } else {
                field = line.substring(0, separator);
                value = line.substring(separator + 1);

                if (value.startsWith(" ")) {
                    value = value.substring(1);
                }
            }

            switch (field) {
                case "event" -> event = value;
                case "id" -> id = value;
                case "data" -> {
                    if (data.length() > 0) {
                        data.append('\n');
                    }
                    data.append(value);
                }
                default -> {
                    // ignore fields we don't currently use
                }
            }
        }
    }

    public record SseEvent(
            String event,
            String id,
            String data
    ) {
    }
}
