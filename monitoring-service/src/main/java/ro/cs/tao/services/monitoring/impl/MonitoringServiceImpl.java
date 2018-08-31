/*
 * Copyright (C) 2017 CS ROMANIA
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
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import ro.cs.tao.messaging.Message;
import ro.cs.tao.messaging.Notifiable;
import ro.cs.tao.messaging.Topics;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.services.commons.MessageConverter;
import ro.cs.tao.services.commons.Notification;
import ro.cs.tao.services.interfaces.MonitoringService;
import ro.cs.tao.services.model.monitoring.OSRuntimeInfo;
import ro.cs.tao.services.model.monitoring.RuntimeInfo;
import ro.cs.tao.topology.NodeDescription;
import ro.cs.tao.topology.TopologyManager;
import ro.cs.tao.utils.executors.Executor;
import ro.cs.tao.utils.executors.ExecutorType;

import java.net.InetAddress;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Cosmin Cara
 */
@Service("monitoringService")
public class MonitoringServiceImpl extends Notifiable implements MonitoringService<Notification> {

    private static final int MAX_QUEUE_SIZE = 100;
    private final Queue<Message> messageQueue;
    @Autowired
    private PersistenceManager persistenceManager;

    public MonitoringServiceImpl() {
        super();
        this.messageQueue = new LinkedList<>();
        subscribe(Topics.INFORMATION, Topics.WARNING, Topics.ERROR, Topics.PROGRESS);
    }

    @Override
    public RuntimeInfo getMasterSnapshot() {
        RuntimeInfo runtimeInfo = null;
        try {
            OSRuntimeInfo inspector = OSRuntimeInfo.createInspector(TopologyManager.getInstance().getMasterNodeInfo());
            runtimeInfo = inspector.getInfo();
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
        return runtimeInfo;
    }

    @Override
    public RuntimeInfo getNodeSnapshot(String hostName) {
        RuntimeInfo runtimeInfo = null;
        try {
            NodeDescription node = persistenceManager.getNodeByHostName(hostName);
            runtimeInfo = OSRuntimeInfo.createInspector(node).getSnapshot();
        } catch (Throwable ex) {
            logger.warning(ex.getMessage());
        }
        return runtimeInfo;
    }

    @Override
    public List<Notification> getLiveNotifications() {
        List<Notification> messages;
        synchronized (this.messageQueue) {
            messages = this.messageQueue.stream()
                    .map(m -> new MessageConverter().to(m)).collect(Collectors.toList());
            this.messageQueue.clear();
        }
        return messages;
    }

    @Override
    public List<Notification> getNotifications(String user, int page) {
        final Page<Message> userMessages = persistenceManager.getUserMessages(user, page);
        return userMessages != null ?
                userMessages.getContent().stream()
                        .map(m -> new MessageConverter().to(m))
                        .collect(Collectors.toList()) : new ArrayList<>();
    }

    @Override
    public List<Notification> acknowledgeNotification(List<Notification> notifications) {
        if (notifications != null) {
            MessageConverter converter = new MessageConverter();
            notifications.forEach(message -> {
                message.setRead(true);
                try {
                    persistenceManager.saveMessage(converter.from(message));
                } catch (PersistenceException e) {
                    logger.severe(e.getMessage());
                }
            });
        }
        return notifications;
    }

    @Override
    public Map<String, Boolean> getNodesOnlineStatus() {
        Map<String, Boolean> statuses = new HashMap<>();
        List<NodeDescription> nodes = TopologyManager.getInstance().list();
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
                        master.setProcessorCount(node.getProcessorCount());
                        master.setDiskSpaceSizeGB(node.getDiskSpaceSizeGB());
                        master.setMemorySizeGB(node.getMemorySizeGB());
                        master.setActive(true);
                        master = persistenceManager.saveExecutionNode(master);
                        persistenceManager.deleteExecutionNode(node.getId());
                        node = master;
                        logger.info(String.format("Node [localhost] has been renamed to [%s]", masterHost));
                    }
                    String hostName = node.getId();
                    Executor executor;
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

    @Override
    protected void onMessageReceived(Message message) {
        synchronized (this.messageQueue) {
            if (this.messageQueue.size() == MAX_QUEUE_SIZE) {
                this.messageQueue.poll();
            }
            this.messageQueue.offer(message);
        }
    }
}
