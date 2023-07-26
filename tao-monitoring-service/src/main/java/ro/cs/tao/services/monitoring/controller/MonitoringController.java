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
package ro.cs.tao.services.monitoring.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.execution.monitor.RuntimeInfo;
import ro.cs.tao.messaging.Messaging;
import ro.cs.tao.messaging.Topic;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.Notification;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.MonitoringService;
import ro.cs.tao.services.monitoring.beans.Metric;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Cosmin Cara
 */
@RestController
@Tag(name ="Monitoring", description = "Endpoint for monitoring node resources and system notifications")
public class MonitoringController extends BaseController {

    private static List<String> metricNames;
    private static final Object lock = new Object();

    @Autowired
    private MonitoringService<Notification> monitoringService;
    @Autowired
    private MetricsEndpoint metricsEndpoint;

    /**
     * Returns a list of all available nodes resources snapshots
     */
    @RequestMapping(value = "/monitor/", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getAllSnapshot() {
        return prepareResult(monitoringService.getNodesSnapshot(),
                             LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss")));
    }

    /**
     * Returns a snapshot of the resources of the primary node
     */
    @RequestMapping(value = "/monitor/master", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getMasterSnapshot() {
        final RuntimeInfo snapshot = monitoringService.getMasterSnapshot();
        if (snapshot == null) {
            return prepareResult("No information available for master node", ResponseStatus.FAILED);
        }
        return prepareResult(snapshot, LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss")));
    }
    /**
     * Returns a snapshot of the resources of a secondary node
     */
    @RequestMapping(value = "/monitor/{host:.+}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getNodeSnapshot(@PathVariable("host") String host) {
        final RuntimeInfo snapshot = monitoringService.getNodeSnapshot(host);
        if (snapshot == null) {
            return prepareResult("No information available for node ['" + host + "']", ResponseStatus.FAILED);
        }
        return prepareResult(snapshot);
    }
    /**
     * Returns the active (i.e. not read) system notifications
     */
    @RequestMapping(value = "/monitor/notification/", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getLiveNotifications() {
        return prepareResult(monitoringService.getLiveNotifications());
    }
    /**
     * Returns the active (i.e. not read) notifications for a user
     */
    @RequestMapping(value = "/monitor/notification/unread", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getUnreadNotifications() {
        return prepareResult(monitoringService.getUnreadNotifications(SessionStore.currentContext().getPrincipal().getName()));
    }
    /**
     * Returns a page of notifications for a user
     */
    @RequestMapping(value = "/monitor/notification/{page}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getNotifications(@RequestHeader(value = "user") String user,
                                              @PathVariable("page") int page) {
        return prepareResult(monitoringService.getNotifications(user, page));
    }

    /**
     * Marks the given notifications as read
     * @param notifications The list of notifications
     */
    @RequestMapping(value = "/monitor/notification/ack", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> acknowledgeNotifications(@RequestBody List<Notification> notifications) {
        return prepareResult(monitoringService.acknowledgeNotification(notifications));
    }

    /**
     * Clears the notifications for the current user
     */
    @RequestMapping(value = "/monitor/notification/clear", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> clearNotifications() {
        try {
            monitoringService.deleteAll(currentUser());
            return prepareResult("Operation succeeded", ResponseStatus.SUCCEEDED);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * Inspects the available nodes and returns their status (online or offline)
     */
    @RequestMapping(value = "/monitor/status", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getNodesOnlineStatus() {
        return prepareResult(monitoringService.getNodesOnlineStatus());
    }

    @RequestMapping(value = "/emit", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> emit() {
        final String message = "Test " + System.currentTimeMillis();
        Messaging.send(currentUser(), Topic.INFORMATION.getCategory(), message);
        return prepareResult(message);
    }

    @RequestMapping(value = "/metrics", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getMetrics() {
        if (metricNames == null) {
            synchronized (lock) {
                metricNames = new ArrayList<>(metricsEndpoint.listNames().getNames());
            }
        }
        return prepareResult(metricNames.stream()
                                        .filter(m -> !(m.startsWith("executor") || m.startsWith("jvm")))
                                        .map(m -> metricsEndpoint.metric(m, null))
                                        .map(Metric::toBean)
                                        .collect(Collectors.toList()));
    }

}
