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
package ro.cs.tao.services.monitoring.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.execution.monitor.NodeManager;
import ro.cs.tao.execution.monitor.OSRuntimeInfo;
import ro.cs.tao.execution.monitor.RuntimeInfo;
import ro.cs.tao.messaging.Message;
import ro.cs.tao.messaging.NotifiableComponent;
import ro.cs.tao.messaging.Topic;
import ro.cs.tao.persistence.MessageProvider;
import ro.cs.tao.persistence.NodeDBProvider;
import ro.cs.tao.services.commons.MessageConverter;
import ro.cs.tao.services.commons.Notification;
import ro.cs.tao.services.interfaces.MonitoringService;
import ro.cs.tao.topology.NodeDescription;
import ro.cs.tao.topology.TopologyManager;
import ro.cs.tao.utils.executors.AuthenticationType;
import ro.cs.tao.utils.executors.Executor;
import ro.cs.tao.utils.executors.ExecutorType;

import java.net.InetAddress;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple monitoring service for getting information about notifications issued in the system and about
 * topology nodes.
 *
 * @author Cosmin Cara
 */
@Service("monitoringService")
public class MonitoringServiceImpl extends NotifiableComponent implements MonitoringService<Notification> {

    @Autowired
    private MessageProvider messageProvider;
    @Autowired
    private NodeDBProvider nodeDBProvider;

    private String[] topics;

    public MonitoringServiceImpl() {
        super();
    }

    @Override
    protected String[] topics() {
        if (topics == null) {
            final String value = ConfigurationManager.getInstance().getValue("monitoring.topics");
            if (value != null) {
                topics = value.split(",");
            } else {
                topics = new String[] { Topic.INFORMATION.value(), Topic.WARNING.value(), Topic.ERROR.value(),
                                        Topic.PROGRESS.value(), Topic.TOPOLOGY.value(), Topic.EXECUTION.value() };
            }
        }
        return topics;
    }

    @Override
    public Map<String, RuntimeInfo> getNodesSnapshot() {
        Map<String, RuntimeInfo> runtimeInfo = null;
        try {
            if (NodeManager.isAvailable()) {
                runtimeInfo = NodeManager.getInstance().getNodesSnapshot();
            } else {
                runtimeInfo = new HashMap<>();
            }
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
        return runtimeInfo;
    }

    @Override
    public RuntimeInfo getMasterSnapshot() {
        RuntimeInfo runtimeInfo = null;
        try {
            NodeDescription nodeInfo = TopologyManager.getInstance().getMasterNodeInfo();
            if (NodeManager.isAvailable()) {
                runtimeInfo = NodeManager.getInstance().getNodeSnapshot(nodeInfo.getId());
            } else {
                OSRuntimeInfo<?> inspector = OSRuntimeInfo.createInspector(nodeInfo.getId(), nodeInfo.getUserName(), nodeInfo.getUserPass(),
                                                                           AuthenticationType.PASSWORD, RuntimeInfo.class);
                runtimeInfo = inspector.getInfo();
            }
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
        return runtimeInfo;
    }

    @Override
    public RuntimeInfo getNodeSnapshot(String hostName) {
        RuntimeInfo runtimeInfo = null;
        try {
            if (NodeManager.isAvailable()) {
                runtimeInfo = NodeManager.getInstance().getNodeSnapshot(hostName);
            } else {
                NodeDescription node = TopologyManager.getInstance().getNode(hostName);
                AuthenticationType type = node.getSshKey() != null ? AuthenticationType.CERTIFICATE : AuthenticationType.PASSWORD;
                runtimeInfo = OSRuntimeInfo.createInspector(node.getId(), node.getUserName(),
                                                            type == AuthenticationType.PASSWORD ? node.getUserPass() : node.getSshKey(),
                                                            type,
                                                            RuntimeInfo.class).getSnapshot();
            }
        } catch (Throwable ex) {
            logger.warning(ex.getMessage());
        }
        return runtimeInfo;
    }

    @Override
    public List<Notification> getLiveNotifications(String userId) {
        final MessageConverter converter = new MessageConverter();
        List<Notification> notifications = new ArrayList<>();
        try {
            notifications = getLastMessages().stream().filter(m -> userId == null || m.getUserId().equals(userId)).map(converter::to).collect(Collectors.toList());
        } catch (Exception e){
        }
        return notifications;
    }

    @Override
    public Map<String, List<Notification>> getUnreadNotifications(String userId) {
        Map<String, List<Notification>>  messages = new LinkedHashMap<>();
        List<Message> unread = messageProvider.getUnreadMessages(userId);
        final MessageConverter converter = new MessageConverter();
        if (unread != null) {
            messages.put(Topic.INFORMATION.value(), new ArrayList<>());
            messages.put(Topic.ERROR.value(), new ArrayList<>());
            messages.put(Topic.EXECUTION.value(), new ArrayList<>());
            unread.stream().map(converter::to).forEach(n -> {
                final String topic = n.getTopic();
                if (Topic.WARNING.isParentOf(topic) || Topic.ERROR.isParentOf(topic)) {
                    messages.get(Topic.ERROR.value()).add(n);
                } else if (Topic.EXECUTION.isParentOf(topic)) {
                    messages.get(Topic.EXECUTION.value()).add(n);
                } else { //if (topic.endsWith(Topic.INFORMATION.value()) || topic.endsWith(Topic.PROGRESS.value())) {
                    messages.get(Topic.INFORMATION.value()).add(n);
                }
            });
        }
        return messages;
    }

    @Override
    public List<Notification> getNotifications(String userId, int page) {
        final List<Message> userMessages = messageProvider.getUserMessages(userId, page);
        return userMessages != null ?
                userMessages.stream()
                        .map(m -> new MessageConverter().to(m))
                        .collect(Collectors.toList()) : new ArrayList<>();
    }

    @Override
    public List<Long> acknowledgeNotification(List<Long> ids, String userId) {
        if (ids != null) {
            messageProvider.acknowledge(ids, userId);
        }
        return ids;
    }

    @Override
    public void deleteAll(String userId) {
        messageProvider.clear(userId);
    }

    @Override
    public Map<String, Boolean> getNodesOnlineStatus() {
        Map<String, Boolean> statuses = new HashMap<>();
        List<NodeDescription> nodes = TopologyManager.getInstance().listNodes();
        if (nodes != null) {
            try {
                String masterHost = InetAddress.getLocalHost().getHostName();
                for (NodeDescription node : nodes) {
                    if ("localhost".equals(node.getId())) {
                        NodeDescription master = new NodeDescription();
                        master.setId(masterHost);
                        master.setUserName(node.getUserName());
                        master.setUserPass(node.getUserPass());
                        master.setDescription(node.getDescription());
                        master.setServicesStatus(node.getServicesStatus());
                        master.setFlavor(node.getFlavor());
                        master.setActive(true);
                        master = nodeDBProvider.save(master);
                        nodeDBProvider.delete(node.getId());
                        node = master;
                        logger.fine(String.format("Node [localhost] has been renamed to [%s]", masterHost));
                    }
                    String hostName = node.getId();
                    Executor<?> executor;
                    if (hostName.equals(masterHost)) {
                        executor = Executor.create(ExecutorType.PROCESS, hostName, null);
                    } else {
                        executor = Executor.create(ExecutorType.SSH2, hostName, null);
                        executor.setUser(node.getUserName());
                        executor.setPassword(node.getUserPass());
                    }
                    boolean canConnect = false;
                    try {
                        canConnect = executor.canConnect();
                    } catch (Exception ignored) {
                    }
                    statuses.put(hostName, canConnect);
                }
            } catch (Exception ex) {
                logger.severe(ex.getMessage());
            }
        }
        return statuses;
    }
}
