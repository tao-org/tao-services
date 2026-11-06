package ro.cs.tao.services.sse.controller;

import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.services.commons.BaseController;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import ro.cs.tao.services.model.event.ExternalEventNotification;
import ro.cs.tao.services.sse.impl.ExternalEventNotificationServiceImpl;

import java.net.URI;
import java.util.logging.Logger;

@RestController
@RequestMapping({ "/events", "/events/" })
@Tag(name = "Event notification Endpoint", description = "Endpoint for event notifications")
public class ExternalEventNotificationController extends BaseController {

    private volatile ExternalEventNotification latestNotification;

    protected Logger logger = Logger.getLogger(getClass().getName());

    @RequestMapping(value = { "/consume", "/consume/" }, method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getEvents(@RequestBody String requestBody) {

        try{
            // SSE endpoint for receiving notifications
            URI endpoint = URI.create(getPropertyValue("sse.notifications.endpoint.url"));

            // SSE endpoint http method
            String httpMethod = getPropertyValue("sse.notifications.endpoint.http.method");

            ExternalEventNotificationServiceImpl notifier = new ExternalEventNotificationServiceImpl(endpoint, httpMethod, requestBody);
            // registerHandler() is asynchronous, the callback runs later, when an SSE event arrives.
            notifier.registerHandler(notification -> {
                logger.info("Notification received: " + notification);
                latestNotification = notification;

                // TODO: Trigger processing
                triggerWorkflow(notification);
            });

            notifier.start();
        }
        catch (Exception e) {
            //return ResponseEntity.internalServerError().body(new Exception(e.getMessage(), e, HttpStatus.INTERNAL_SERVER_ERROR.value()));
            return handleException(e);
        }

        return ResponseEntity.ok(latestNotification);
    }

    public ExternalEventNotification getLatestNotification() {
        return latestNotification;
    }

    private void triggerWorkflow(ExternalEventNotification notification) {

        String collection = notification.getCollection();
        String feature = notification.getFeature();
        String geometry = notification.getGeometry();

        // Build workflow parameters...
        // TODO: Trigger processing
    }

    private String getPropertyValue(String name) {
        return ConfigurationManager.getInstance().getValue(name);
    }

}
