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
import org.hsqldb.lib.HashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.datasource.DataSourceTopic;
import ro.cs.tao.execution.model.ExecutionStatus;
import ro.cs.tao.execution.model.ExecutionTaskSummary;
import ro.cs.tao.execution.monitor.NodeManager;
import ro.cs.tao.execution.monitor.RuntimeInfo;
import ro.cs.tao.messaging.Message;
import ro.cs.tao.messaging.Messaging;
import ro.cs.tao.messaging.Topic;
import ro.cs.tao.messaging.progress.ActivityProgress;
import ro.cs.tao.messaging.progress.DownloadProgress;
import ro.cs.tao.quota.QuotaTopic;
import ro.cs.tao.security.SystemPrincipal;
import ro.cs.tao.security.UserPrincipal;
import ro.cs.tao.serialization.JsonMapper;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.Notification;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.MonitoringService;
import ro.cs.tao.services.interfaces.RepositoryService;
import ro.cs.tao.services.monitoring.beans.Metric;
import ro.cs.tao.utils.FileUtilities;
import ro.cs.tao.utils.StringUtilities;
import ro.cs.tao.workspaces.Repository;
import ro.cs.tao.workspaces.RepositoryType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Cosmin Cara
 */
@RestController
@Tag(name ="Monitoring", description = "Endpoint for monitoring node resources and system notifications")
public class MonitoringController extends BaseController {

    private static List<String> metricNames;
    private static final Object lock = new Object();

    private static Timer collectTimer;
    private static Task collectTask = new Task();

    static {
        collectTimer = new Timer();
        collectTimer.scheduleAtFixedRate(collectTask, 60000, 60000);
    }

    @Autowired
    private MonitoringService<Notification> monitoringService;
    @Autowired
    private MetricsEndpoint metricsEndpoint;
    @Autowired
    private RepositoryService repositoryService;

    /**
     * Returns a list of all available nodes resources snapshots
     */
    @RequestMapping(value = "/monitor/", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
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
    @RequestMapping(value = "/monitor/master", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
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
    @RequestMapping(value = "/monitor/{host:.+}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
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
    @RequestMapping(value = "/monitor/notification/", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getLiveNotifications() {
        try {
            if (isCurrentUserAdmin()) {
                return prepareResult(monitoringService.getLiveNotifications(null));
            } else {
                return prepareResult(monitoringService.getLiveNotifications(currentUser()));
            }
        } catch (Throwable t) {
            return prepareResult(new HashMap<>());
        }
    }
    /**
     * Returns the active (i.e. not read) notifications for a user
     */
    @RequestMapping(value = "/monitor/notification/unread", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getUnreadNotifications() {
        try {
            return prepareResult(monitoringService.getUnreadNotifications(currentUser()));
        } catch (Throwable t) {
            return prepareResult(new HashMap<>());
        }
    }
    /**
     * Returns a page of notifications for a user
     */
    @RequestMapping(value = "/monitor/notification/{page}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getNotifications(@PathVariable("page") int page) {
        try {
            return prepareResult(monitoringService.getNotifications(currentUser(), page));
        } catch (Throwable t) {
            return prepareResult(new ArrayList<>());
        }
    }

    @RequestMapping(value = "/monitor/notification/{user}/{page}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getUserNotifications(@PathVariable("user") String userId,
                                                                   @PathVariable("page") int page) {
        if (isCurrentUserAdmin() || currentUser().equals(userId)) {
            try{
                return prepareResult(monitoringService.getNotifications(userId, page));
            } catch (Throwable t) {
                return prepareResult(new ArrayList<>());
            }
        } else {
            return prepareResult("Not authorized", ResponseStatus.FAILED, HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Marks the given notifications as read
     * @param notificationIds The list of notification identifiers
     */
    @RequestMapping(value = "/monitor/notification/ack", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> acknowledgeNotifications(@RequestBody List<Long> notificationIds) {
        return prepareResult(monitoringService.acknowledgeNotification(notificationIds, currentUser()));
    }

    /**
     * Clears the notifications for the current user
     */
    @RequestMapping(value = "/monitor/notification/clear", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
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
    @RequestMapping(value = "/monitor/status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getNodesOnlineStatus() {
        if (isCurrentUserAdmin()) {
            return prepareResult(monitoringService.getNodesOnlineStatus());
        } else {
            return prepareResult("Not authorized", ResponseStatus.FAILED, HttpStatus.FORBIDDEN);
        }
    }

    @RequestMapping(value = "/emit", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
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

    @RequestMapping(value = "/metrics", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getMetrics() {
        return prepareResult(collectMetrics());
    }

    @RequestMapping(value = "/metrics/enable", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> enableMetrics() {
        try {
            collectTimer.cancel();
            collectTask.cancel();
            collectTimer = new Timer();
            collectTask = new Task();
            collectTask.endpoint = metricsEndpoint;
            collectTimer.scheduleAtFixedRate(collectTask, 1000, 60000);
            return prepareResult("Metrics collection enabled");
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/metrics/disable", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> disableMetrics() {
        try {
            collectTimer.cancel();
            return prepareResult("Metrics collection disabled");
        } catch (Exception e) {
            return handleException(e);
        }
    }

    private List<Metric> collectMetrics() {
        if (collectTask.endpoint == null) {
            collectTask.endpoint = metricsEndpoint;
        }
        if (metricNames == null) {
            synchronized (lock) {
                metricNames = new ArrayList<>(metricsEndpoint.listNames().getNames());
            }
        }
        return metricNames.stream()
                          .filter(m -> !(m.startsWith("executor") || m.startsWith("jvm")))
                          .map(m -> metricsEndpoint.metric(m, null))
                          .map(Metric::toBean)
                          .collect(Collectors.toList());
    }

    private Message createMessage(Principal principal, String topic) throws JsonProcessingException {
        Message message = null;
        final String master = NodeManager.getInstance().getMasterNode().getNode().getId();
        if (Topic.INFORMATION.value().equals(topic)) {
            message = Message.create(principal.getName(), "Test", "Info " + System.currentTimeMillis(), false);
        } else if (Topic.WARNING.value().equals(topic)) {
            message = Message.create(principal.getName(), "Test", "Warn " + System.currentTimeMillis(), false);
        } else if (Topic.ERROR.value().equals(topic)) {
            message = Message.create(principal.getName(), "Test", "Error " + System.currentTimeMillis(), false);
        } else if (Topic.SYSTEM.value().equals(topic)) {
            message = Message.create(SystemPrincipal.instance().getName(), "Test", "System " + System.currentTimeMillis(), false);
        } else if (Topic.RESOURCES.value().equals(topic)) {
            final RuntimeInfo nodeSnapshot = NodeManager.getInstance().getNodeSnapshot(master);
            message = Message.create(SystemPrincipal.instance().getName(), master, JsonMapper.instance().writeValueAsString(nodeSnapshot), false);
        } else if (QuotaTopic.USER_STORAGE_USAGE.value().equals(topic)) {
            message = Message.create(SystemPrincipal.instance().getName(), "QuotaManager", "User storage updated", "1024", false);
        } else if (QuotaTopic.USER_CPU_USAGE.value().equals(topic)) {
            message = Message.create(SystemPrincipal.instance().getName(), "QuotaManager", "CPU usage updated", "4", false);
        } else if (Topic.RESOURCES.value().equals(topic)) {
            final RuntimeInfo nodeSnapshot = NodeManager.getInstance().getNodeSnapshot(master);
            message = Message.create(SystemPrincipal.instance().getName(), master, JsonMapper.instance().writeValueAsString(nodeSnapshot), false);
        } else if (Topic.EXECUTION.value().equals(topic)) {
            message = Message.create(principal.getName(), "1", ExecutionStatus.RUNNING.name(), false);
            //message.addItem("host", master);
            ExecutionTaskSummary summary = new ExecutionTaskSummary();
            summary.setTaskId(1);
            summary.setCommand("docker run");
            summary.setJobName("job name");
            summary.setHost(master);
            summary.setComponentType("exec");
            summary.setWorkflowName("workflow");
            summary.setOutput("");
            summary.setComponentName("ndviOp");
            summary.setLastUpdated(LocalDateTime.now());
            summary.setTaskStart(LocalDateTime.now().minusMinutes(1));
            summary.setPercentComplete(0.57);
            summary.setTaskStatus(ExecutionStatus.RUNNING);
            summary.setUsedCPU(8);
            summary.setUsedRAM(8192);
            summary.setUserId(principal.getName());
            message.setPayload(JsonMapper.instance().writeValueAsString(summary));
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
            final Repository repository = repositoryService.getByUser(currentUser()).stream()
                                                           .filter(r -> r.getType() == RepositoryType.LOCAL)
                                                           .findFirst().get();
            message.addItem("Repository", repository.getId());
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

    private static class Task extends TimerTask {
        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        private MetricsEndpoint endpoint;

        @Override
        public void run() {
            try {
                final Path logFile = Paths.get(ConfigurationManager.getInstance().getValue("site.path")).getParent().resolve("logs").resolve("metrics.log");
                if (endpoint != null) {
                    if (metricNames == null) {
                        synchronized (lock) {
                            metricNames = new ArrayList<>(endpoint.listNames().getNames());
                        }
                    }
                    if (!Files.exists(logFile)) {
                        FileUtilities.createDirectories(logFile.getParent());
                        Files.writeString(logFile, "datetime;" + String.join(";", metricNames) + "\n");
                    }
                    final String values = metricNames.stream().map(m -> endpoint.metric(m, null))
                                                     .map(m -> m.getMeasurements().stream()
                                                                .map(s -> String.valueOf(s.getValue())).collect(Collectors.joining(",")))
                                                     .collect(Collectors.joining(";"));
                    Files.writeString(logFile, LocalDateTime.now().format(formatter) + ";" + values + "\n", StandardOpenOption.APPEND);

                }
            } catch (Throwable t) {
                Logger.getLogger(MonitoringController.class.getName()).warning(t.getMessage());
            }
        }
    }
}
