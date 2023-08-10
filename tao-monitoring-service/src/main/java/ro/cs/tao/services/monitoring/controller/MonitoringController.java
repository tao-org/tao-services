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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.datasource.DataSourceTopic;
import ro.cs.tao.execution.model.ExecutionStatus;
import ro.cs.tao.execution.monitor.NodeManager;
import ro.cs.tao.execution.monitor.RuntimeInfo;
import ro.cs.tao.messaging.Message;
import ro.cs.tao.messaging.Messaging;
import ro.cs.tao.messaging.Topic;
import ro.cs.tao.messaging.progress.ActivityProgress;
import ro.cs.tao.messaging.progress.DownloadProgress;
import ro.cs.tao.security.SystemPrincipal;
import ro.cs.tao.security.UserPrincipal;
import ro.cs.tao.serialization.JsonMapper;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.Notification;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.MonitoringService;
import ro.cs.tao.services.monitoring.beans.Metric;
import ro.cs.tao.utils.StringUtilities;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
        if (isCurrentUserAdmin()) {
            return prepareResult(monitoringService.getNodesSnapshot(),
                                 LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss")));
        } else {
            return prepareResult("Not authorized", ResponseStatus.FAILED, HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Returns a snapshot of the resources of the primary node
     */
    @RequestMapping(value = "/monitor/master", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getMasterSnapshot() {
        if (isCurrentUserAdmin()) {
            final RuntimeInfo snapshot = monitoringService.getMasterSnapshot();
            if (snapshot == null) {
                return prepareResult("No information available for master node", ResponseStatus.FAILED);
            }
            return prepareResult(snapshot, LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss")));
        } else {
            return prepareResult("Not authorized", ResponseStatus.FAILED, HttpStatus.FORBIDDEN);
        }
    }
    /**
     * Returns a snapshot of the resources of a secondary node
     */
    @RequestMapping(value = "/monitor/{host:.+}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getNodeSnapshot(@PathVariable("host") String host) {
        if (isCurrentUserAdmin()) {
            final RuntimeInfo snapshot = monitoringService.getNodeSnapshot(host);
            if (snapshot == null) {
                return prepareResult("No information available for node ['" + host + "']", ResponseStatus.FAILED);
            }
            return prepareResult(snapshot);
        } else {
            return prepareResult("Not authorized", ResponseStatus.FAILED, HttpStatus.FORBIDDEN);
        }
    }
    /**
     * Returns the active (i.e. not read) system notifications
     */
    @RequestMapping(value = "/monitor/notification/", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getLiveNotifications() {
        if (isCurrentUserAdmin()) {
            return prepareResult(monitoringService.getLiveNotifications(null));
        } else {
            return prepareResult(monitoringService.getLiveNotifications(currentUser()));
        }
    }
    /**
     * Returns the active (i.e. not read) notifications for a user
     */
    @RequestMapping(value = "/monitor/notification/unread", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getUnreadNotifications() {
        return prepareResult(monitoringService.getUnreadNotifications(currentUser()));
    }
    /**
     * Returns a page of notifications for a user
     */
    @RequestMapping(value = "/monitor/notification/{page}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getNotifications(@PathVariable("page") int page) {
        return prepareResult(monitoringService.getNotifications(currentUser(), page));
    }

    @RequestMapping(value = "/monitor/notification/{user}/{page}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getUserNotifications(@PathVariable("user") String userId,
                                                                   @PathVariable("page") int page) {
        if (isCurrentUserAdmin() || currentUser().equals(userId)) {
            return prepareResult(monitoringService.getNotifications(userId, page));
        } else {
            return prepareResult("Not authorized", ResponseStatus.FAILED, HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Marks the given notifications as read
     * @param notificationIds The list of notification identifiers
     */
    @RequestMapping(value = "/monitor/notification/ack", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> acknowledgeNotifications(@RequestBody List<Long> notificationIds) {
        return prepareResult(monitoringService.acknowledgeNotification(notificationIds, currentUser()));
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
        if (isCurrentUserAdmin()) {
            return prepareResult(monitoringService.getNodesOnlineStatus());
        } else {
            return prepareResult("Not authorized", ResponseStatus.FAILED, HttpStatus.FORBIDDEN);
        }
    }

    @RequestMapping(value = "/emit", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> emit(@RequestParam(name = "principal", required = false) String principal,
                                                   @RequestParam("topic") String topic) {
        try {
            if (StringUtilities.isNullOrEmpty(topic)) {
                throw new IllegalArgumentException("[topic] is empty");
            }
            Principal user = StringUtilities.isNullOrEmpty(principal) ? currentPrincipal() : new UserPrincipal(principal);
            final Message message = createMessage(user, topic);
            if (message == null) {
                throw new IllegalArgumentException("Topic '" + topic + "' not known");
            }
            Messaging.send(user, topic, message, false);
            return prepareResult(message);
        } catch (Exception e) {
            return handleException(e);
        }
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

    private Message createMessage(Principal principal, String topic) throws JsonProcessingException {
        Message message = null;
        if (Topic.INFORMATION.value().equals(topic)) {
            message = Message.create(principal.getName(), "Test", "Info " + System.currentTimeMillis(), false);
        } else if (Topic.WARNING.value().equals(topic)) {
            message = Message.create(principal.getName(), "Test", "Warn " + System.currentTimeMillis(), false);
        } else if (Topic.ERROR.value().equals(topic)) {
            message = Message.create(principal.getName(), "Test", "Error " + System.currentTimeMillis(), false);
        } else if (Topic.SYSTEM.value().equals(topic)) {
            message = Message.create(SystemPrincipal.instance().getName(), "Test", "System " + System.currentTimeMillis(), false);
        } else if (Topic.RESOURCES.value().equals(topic)) {
            final String master = NodeManager.getInstance().getMasterNode().getNode().getId();
            final RuntimeInfo nodeSnapshot = NodeManager.getInstance().getNodeSnapshot(master);
            message = Message.create(SystemPrincipal.instance().getName(), master, JsonMapper.instance().writeValueAsString(nodeSnapshot), false);
        } else if (Topic.EXECUTION.value().equals(topic)) {
            message = Message.create(principal.getName(), "task id", ExecutionStatus.RUNNING.name(), false);
        } else if (Topic.TOPOLOGY.value().equals(topic)) {
            message = new Message();
            message.setTopic(Topic.TOPOLOGY.value());
            message.setPersistent(false);
            message.addItem("node", "node_name");
            message.addItem("operation", "added");
            message.addItem("user", "node_user");
            message.addItem("password", "node_pwd");
        } else if (Topic.TRANSFER_PROGRESS.value().equals(topic)) {
            message = new ActivityProgress("Test " + System.currentTimeMillis(), new Random().nextDouble());
        } else if (DataSourceTopic.PRODUCT_PROGRESS.value().equals(topic)) {
            Random random = new Random();
            message = new DownloadProgress("name", random.nextDouble(), random.nextDouble() * 10.0, 1);
        }
        if (message != null) {
            message.setId(System.currentTimeMillis());
            message.setTopic(topic);
            message.setUserId(principal.getName());
        }
        return message;
    }
}
