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
package ro.cs.tao.services.admin.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.docker.Container;
import ro.cs.tao.docker.ContainerVisibility;
import ro.cs.tao.execution.monitor.NodeManager;
import ro.cs.tao.messaging.Message;
import ro.cs.tao.messaging.Messaging;
import ro.cs.tao.messaging.Topic;
import ro.cs.tao.persistence.NodeDBProvider;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.ResourceSubscriptionProvider;
import ro.cs.tao.persistence.UserProvider;
import ro.cs.tao.persistence.repository.NodeFlavorRepository;
import ro.cs.tao.security.SystemPrincipal;
import ro.cs.tao.services.factory.StorageServiceFactory;
import ro.cs.tao.services.interfaces.*;
import ro.cs.tao.services.model.component.ProcessingComponentInfo;
import ro.cs.tao.services.model.user.DisableUserInfo;
import ro.cs.tao.services.model.user.UserMapping;
import ro.cs.tao.services.model.user.UserUnicityInfo;
import ro.cs.tao.services.model.workflow.WorkflowInfo;
import ro.cs.tao.subscription.FlavorSubscription;
import ro.cs.tao.subscription.ResourceSubscription;
import ro.cs.tao.subscription.SubscriptionType;
import ro.cs.tao.topology.NodeDescription;
import ro.cs.tao.topology.NodeFlavor;
import ro.cs.tao.topology.TopologyException;
import ro.cs.tao.topology.TopologyManager;
import ro.cs.tao.topology.docker.DockerManager;
import ro.cs.tao.user.Group;
import ro.cs.tao.user.User;
import ro.cs.tao.user.UserPreference;
import ro.cs.tao.user.UserStatus;
import ro.cs.tao.utils.FileUtilities;
import ro.cs.tao.workflow.enums.Status;
import ro.cs.tao.workspaces.Repository;
import ro.cs.tao.workspaces.RepositoryType;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Oana H.
 */
@Service("adminService")
public class AdministrationServiceImpl implements AdministrationService {

    @Autowired
    private UserProvider userProvider;
    @Autowired
    private NodeDBProvider nodeDBProvider;
    @Autowired
    @Qualifier("nodeFlavorRepository")
    private NodeFlavorRepository nodeFlavorRepository;
    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private ComponentService componentService;
    @Autowired
    private ContainerService containerService;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private UserService userService;
    @Autowired
    private ResourceSubscriptionProvider resourceSubscriptionProvider;

    @Override
    public User addNewUser(User newUserInfo) throws PersistenceException {
        return userProvider.save(newUserInfo);
    }

    @Override
    public List<UserUnicityInfo> getAllUsersUnicityInfo() {
        final Map<String, String[]> unicityInfo = userProvider.listUnicityInfo();
        final List<UserUnicityInfo> results = new ArrayList<>();
        for (String username: unicityInfo.keySet()) {
            results.add(new UserUnicityInfo(username, unicityInfo.get(username)[0], unicityInfo.get(username)[1]));
        }
        return results;
    }

    @Override
    public List<UserMapping> getAllUserNames() {
        final Map<String, String> mappings = userProvider.listNames();
        return mappings.entrySet().stream().map(e -> new UserMapping(e.getKey(), e.getValue())).collect(Collectors.toList());
    }

    @Override
    public List<User> findUsersByStatus(UserStatus userStatus) {
        return userProvider.list(userStatus);
    }

    @Override
    public List<Group> getGroups() {
        return userProvider.listGroups();
    }

    @Override
    public List<User> getAdministrators() {
        return userProvider.listAdministrators();
    }

    @Override
    public List<User> getUsers(Set<String> userIds) {
        return userProvider.listUsers(userIds);
    }

    @Override
    public User getUserInfo(String userId) {
        return userProvider.get(userId);
    }

    @Override
    public User updateUserInfo(User updatedInfo) throws PersistenceException {
        return userProvider.update(updatedInfo, true);
    }

    @Override
    public void disableUser(String userId, DisableUserInfo additionalDisableActions) throws PersistenceException {
        final Logger logger = Logger.getLogger(AdministrationService.class.getName());
        userProvider.disable(userId);
        if (additionalDisableActions != null && Boolean.TRUE.equals(additionalDisableActions.getDeletePrivateResources())) {
            // 1. Delete user private workflows
            logger.fine("Deleting private workflows of user: " + userId + "");
            try {
                List<WorkflowInfo> workflows = workflowService.getUserWorkflowsByStatus(userId, Status.DRAFT);
                for (WorkflowInfo workflow : workflows) {
                    workflowService.deleteWorkflowImage(workflow.getId());
                    workflowService.delete(workflow.getId());
                }
            } catch (PersistenceException e) {
                logger.warning(e.getMessage());
            }
            // 2. Delete user private components
            logger.fine("Deleting private components of user: " + userId + "");
            try {
                List<ProcessingComponentInfo> components = componentService.getUserScriptComponents(userId);
                components.addAll(componentService.getUserProcessingComponents(userId));
                for (ProcessingComponentInfo component : components) {
                    componentService.delete(component.getId());
                }
            } catch (PersistenceException e) {
                logger.warning(e.getMessage());
            }
            // 3. Delete user private containers
            try {
                logger.fine("Deleting private containers of user: " + userId + "");
                List<Container> containers = containerService.listUserContainers(userId);
                for (Container container : containers) {
                    if (!ContainerVisibility.PUBLIC.equals(container.getVisibility())) {
                        containerService.delete(container.getId());
                        if (!DockerManager.removeImage(container.getId())) {
                            logger.warning("Failed to remove docker image " + container.getId());
                        }
                    } else {
                        container.setOwnerId(SystemPrincipal.instance().getName());
                        containerService.update(container);
                    }
                }
            } catch (Exception e) {
                logger.warning(e.getMessage());
            }
            // 4. Delete user subscriptions
            logger.fine("Deleting user subscriptions of user: " + userId + "");
            try {
                ResourceSubscription subscription = resourceSubscriptionProvider.getUserOpenSubscription(userId);
                if (subscription != null) {
                    subscription.setEnded(LocalDateTime.now());
                    resourceSubscriptionProvider.update(subscription);
                    for (Map.Entry<String, FlavorSubscription> entry : subscription.getFlavors().entrySet()) {
                        try {
                            if (!subscription.getType().equals(SubscriptionType.FIXED_RESOURCES)) {
                                deleteFlavorNodesForSubscription(subscription, entry.getValue(), entry.getValue().getQuantity());
                            }
                        } catch (PersistenceException | TopologyException e) {
                            logger.warning(e.getMessage());
                        }
                    }
                }
            } catch (PersistenceException e) {
                logger.warning(e.getMessage());
            }
            // 5. Delete other user nodes
            logger.fine("Deleting other user nodes of user: " + userId + "");
            try {
                List<NodeDescription> allNodes = nodeDBProvider.list();
                allNodes.removeIf(n -> !userId.equals(n.getOwner()));
                for (NodeDescription node : allNodes) {
                    TopologyManager.getInstance().removeNode(node.getId());
                }
            } catch (TopologyException e) {
                logger.warning(e.getMessage());
            }
            // 6. Delete user preferences
            logger.fine("Deleting user preferences of user: " + userId + "");
            try {
                List<UserPreference> preferences = userProvider.listPreferences(userId);
                if (preferences != null) {
                    userService.removeUserPreferences(userId,
                                                      preferences.stream()
                                                                 .map(UserPreference::getKey)
                                                                 .collect(Collectors.toList()));
                }
            } catch (PersistenceException e) {
                logger.warning(e.getMessage());
            }
            // 7. Delete user repositories
            logger.fine("Deleting user repositories of user: " + userId + "");
            List<Repository> repositories = repositoryService.getByUser(userId);
            for (Repository repository : repositories) {
                try {
                    if (repository.isEditable() && !repository.isReadOnly()) {
                        StorageService storageService = StorageServiceFactory.getInstance(repository);
                        storageService.associate(repository);
                        try {
                            if (repository.getType() == RepositoryType.LOCAL) {
                                FileUtilities.deleteTree(Paths.get(SystemVariable.ROOT.value()).resolve(userId));
                            } else {
                                storageService.remove("/");
                            }
                        } catch (IOException e) {
                            logger.warning(e.getMessage());
                        }
                    }
                    repositoryService.delete(repository.getId());
                } catch (Exception e) {
                    logger.warning(e.getMessage());
                }
            }
        }
    }

    @Override
    public void deleteUser(String userId) throws PersistenceException {
        userProvider.delete(userId);
    }

    @Override
    public void messageUser(String userId, String message) {
        final Message msg = Message.create(userId, "Administrator", message);
        msg.setId(System.currentTimeMillis());
        msg.setPersistent(false);
        msg.setTopic(Topic.INFORMATION.value());
        msg.setTimestamp(System.currentTimeMillis());
        Messaging.send(msg);
    }

    @Override
    public void initializeSubscription(ResourceSubscription subscription) throws TopologyException, PersistenceException {
        final TopologyManager topologyManager = TopologyManager.getInstance();
        if (!topologyManager.isExternalProviderAvailable()) {
            return;
        }
        final int existing = nodeDBProvider.countUsableNodes(subscription.getUserId());
        final Map<String, FlavorSubscription> flavors = subscription.getFlavors();
        for (FlavorSubscription value : flavors.values()) {
            final Optional<NodeFlavor> flavor = nodeFlavorRepository.findById(value.getFlavorId());
            if(flavor.isEmpty())
                throw new RuntimeException("Node flavor not found");
            for(int i = 0; i < value.getQuantity(); i++) {
                final NodeDescription node = NodeManager.getInstance().getNewNodeDescription(existing, subscription.getUserId(), flavor.get());
                node.setVolatile(false);
                topologyManager.addNode(node);
            }
        }
    }

    @Override
    public void createFlavorNodesForSubscription(ResourceSubscription subscription, FlavorSubscription flavor, int nodesToCreate){
        final TopologyManager topologyManager = TopologyManager.getInstance();
        if (!topologyManager.isExternalProviderAvailable()) {
            return;
        }
        final int existing = nodeDBProvider.countUsableNodes(subscription.getUserId());
        final Optional<NodeFlavor> nodeFlavorOptional = nodeFlavorRepository.findById(flavor.getFlavorId());
        if(nodeFlavorOptional.isEmpty())
            throw new RuntimeException("Node flavor not found");
        for (int i = 0; i < nodesToCreate; i++) {
            final NodeDescription node = NodeManager.getInstance().getNewNodeDescription(existing, subscription.getUserId(), nodeFlavorOptional.get());
            node.setVolatile(false);
            topologyManager.addNode(node);
        }
    }

    @Override
    public void deleteFlavorNodesForSubscription(ResourceSubscription subscription, FlavorSubscription flavor, int nodesToDelete) throws TopologyException, PersistenceException {
        final TopologyManager topologyManager = TopologyManager.getInstance();
        if (!topologyManager.isExternalProviderAvailable()) {
            return;
        }
        int existing = nodeDBProvider.countUsableNodes(subscription.getUserId());
        if(existing <= 0)
            return;
        List<NodeDescription> topologyNodes = nodeDBProvider.getByUserWithFlavor(subscription.getUserId(), flavor.getFlavorId());
        if(topologyNodes.isEmpty() || topologyNodes.size() < nodesToDelete){
            throw new TopologyException("There are not enough nodes to delete ("
                    + topologyNodes.size() + ") as per required amount: " + nodesToDelete);
        }
        for (int i = 0; i < nodesToDelete; i++) {
            String nodeToDeleteId = topologyNodes.get(i).getId();
            topologyManager.removeNode(nodeToDeleteId);
        }
    }
}
