package ro.cs.tao.services.sse.impl;

import ro.cs.tao.services.interfaces.ExternalEventNotificationHandler;
import ro.cs.tao.services.interfaces.ExternalEventNotificationService;
import ro.cs.tao.services.model.event.ExternalEventNotification;
import ro.cs.tao.services.sse.parser.SseNotificationJson;
import ro.cs.tao.services.sse.parser.SseParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Implements the SSE listening mechanism
 * @author Oana H.
 */
public class ExternalEventNotificationServiceImpl implements ExternalEventNotificationService {

    private final HttpClient httpClient;
    private final URI endpoint;
    private final String httpMethod;
    private final String requestBody;

    private final List<ExternalEventNotificationHandler> handlers =
            new CopyOnWriteArrayList<>();

    private final AtomicBoolean running = new AtomicBoolean(false);

    protected Logger logger = Logger.getLogger(getClass().getName());

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r, "tao-notifier-sse");
                thread.setDaemon(true);
                return thread;
            });

    public ExternalEventNotificationServiceImpl(
            URI endpoint,
            String httpMethod,
            String requestBody) {

        this.endpoint = endpoint;
        this.httpMethod = httpMethod;
        this.requestBody = requestBody;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public void registerHandler(ExternalEventNotificationHandler handler) {
        handlers.add(handler);
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        executor.submit(this::run);
    }

    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        executor.shutdownNow();
    }

    private void run() {

        long reconnectDelay = 1_000;
        int attempt = 0;

        while (running.get()) {
            attempt++;

            logger.info("SSE connection attempt #" + attempt);

            try {
                connectAndConsume();

                // If the connection ended normally, don't reconnect immediately in a tight loop.
                if (running.get()) {
                    logger.info("SSE stream ended. Reconnecting in " + reconnectDelay + " ms.");
                    sleep(reconnectDelay);
                }

            } catch (Exception e) {

                if (!running.get()) {
                    break;
                }

                logger.severe("Notifier SSE connection failed: " + e.getMessage());
                logger.info("Retrying in " + reconnectDelay + " ms.");

                sleep(reconnectDelay);

                reconnectDelay = Math.min(reconnectDelay * 2, 30_000);
            }
        }
        logger.info("SSE listener stopped.");
    }

    /**
     * Connect to SSE Notifier and consume events notifications
     * @throws Exception
     */
    private void connectAndConsume() throws Exception {

        logger.info("Connecting to SSE endpoint: " + endpoint);

        // long-lived SSE connection
        /*HttpRequest request = HttpRequest.newBuilder()
                .uri(endpoint)
                //.timeout(Duration.ofDays(1)) // The server itself controls how long the stream remains open, so we'd actually omit the request timeout and rely on HttpClient's connect timeout:
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();*/

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(endpoint)
                .header("Accept", "text/event-stream");

        final String method = httpMethod.toUpperCase(Locale.ROOT);
        switch (method) {
            case "GET" ->
                    requestBuilder.GET();

            case "POST" ->
                    requestBuilder
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(requestBody));

            case "PUT" ->
                    requestBuilder
                            .header("Content-Type", "application/json")
                            .PUT(HttpRequest.BodyPublishers.ofString(requestBody));

            case "DELETE" ->
                    requestBuilder.DELETE();

            default ->
                    throw new IllegalArgumentException("Unsupported HTTP method for SSE: " + method);
        }

        HttpRequest request = requestBuilder.build();

        HttpResponse<java.io.InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        logger.info("SSE HTTP response status: " + response.statusCode());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            response.body().close();
            throw new IllegalStateException("Notifier returned HTTP " + response.statusCode());
        }

        logger.info("SSE connection established successfully.");

        try (var input = response.body()) {
            SseParser parser = new SseParser(this::handleEvent);
            parser.parse(input);

            logger.info("SSE stream ended.");
        }
    }

    private void handleEvent(SseParser.SseEvent event) {

        if (!"message".equals(event.event())) {
            return;
        }

        ExternalEventNotification notification = SseNotificationJson.parse(event.id(), event.data());

        for (ExternalEventNotificationHandler handler : handlers) {
            try {
                handler.onNotification(notification);

            } catch (RuntimeException e) {
                logger.severe("Notification handler failed: " + e.getMessage());
            }
        }
    }

    private void sleep(long milliseconds) {

        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
