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

package ro.cs.tao.services.sse;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.EventListener;
import ro.cs.tao.services.commons.ServiceLauncher;
import ro.cs.tao.services.sse.impl.ExternalEventNotificationServiceImpl;

import java.net.URI;
import java.util.logging.Logger;

/**
 * @author Oana H.
 */
@SpringBootApplication()
@ImportResource({"classpath:tao-sse-notification-service-context.xml"})
@PropertySource("classpath:config/tao-sse.properties")
public class SSEServiceLauncher extends ServiceLauncher {

    private ExternalEventNotificationServiceImpl notifier;

    @Value("${sse.notifications.enabled:false}")
    private boolean eventsEnabled;

    @Value("${sse.notifications.endpoint.url}")
    private String endpointUrl;

    @Value("${sse.notifications.endpoint.http.method:POST}")
    private String httpMethod;

    @Value("${sse.notifications.request.body}")
    private String requestBody;

    protected Logger logger = Logger.getLogger(getClass().getName());

    public static void main(String[] args) {
        SpringApplication.run(SSEServiceLauncher.class, args);
    }

    public SSEServiceLauncher() {
        super();
    }

    @Override
    public String serviceName() { return "SSE Notification Service"; }

    @EventListener(ApplicationReadyEvent.class)
    public void startSseListener() {

        if (!eventsEnabled) {
            System.out.println(
                    "SSE notifications are disabled by configuration."
            );
            return;
        }

        logger.info("Starting SSE notification listener...");
        URI endpoint = URI.create(endpointUrl);
        notifier = new ExternalEventNotificationServiceImpl(endpoint, httpMethod, requestBody);
        notifier.registerHandler(notification -> {
            logger.info("Notification received: " + notification.toString());
            // TODO : Process the notification here.
        });

        notifier.start();
        logger.info("SSE listener started; connection attempt running asynchronously.");
    }

    @PreDestroy
    public void stopSseListener() {
        if (notifier != null) {
            notifier.stop();
            logger.info("SSE notification listener stopped.");
        }
    }
}
