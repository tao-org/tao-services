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
package ro.cs.tao.services.entity.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.xml.sax.SAXException;
import ro.cs.tao.Tag;
import ro.cs.tao.component.*;
import ro.cs.tao.component.converters.ConverterFactory;
import ro.cs.tao.component.converters.ParameterConverter;
import ro.cs.tao.component.enums.ComponentCategory;
import ro.cs.tao.component.enums.TagType;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.datasource.DataSourceManager;
import ro.cs.tao.datasource.beans.Parameter;
import ro.cs.tao.datasource.beans.Query;
import ro.cs.tao.datasource.converters.ConversionException;
import ro.cs.tao.datasource.param.CommonParameterNames;
import ro.cs.tao.datasource.param.DataSourceParameter;
import ro.cs.tao.datasource.param.JavaType;
import ro.cs.tao.datasource.persistence.DataSourceComponentProvider;
import ro.cs.tao.datasource.persistence.QueryProvider;
import ro.cs.tao.eodata.enums.Visibility;
import ro.cs.tao.execution.model.ExecutionJob;
import ro.cs.tao.execution.model.ExecutionTask;
import ro.cs.tao.execution.persistence.ExecutionJobProvider;
import ro.cs.tao.execution.persistence.ExecutionTaskProvider;
import ro.cs.tao.execution.util.TaskUtilities;
import ro.cs.tao.persistence.*;
import ro.cs.tao.persistence.repository.SourceDescriptorRepository;
import ro.cs.tao.persistence.repository.TargetDescriptorRepository;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.serialization.BaseSerializer;
import ro.cs.tao.serialization.MediaType;
import ro.cs.tao.serialization.SerializationException;
import ro.cs.tao.serialization.SerializerFactory;
import ro.cs.tao.services.commons.dev.MockData;
import ro.cs.tao.services.entity.util.ServiceTransformUtils;
import ro.cs.tao.services.interfaces.ComponentService;
import ro.cs.tao.services.interfaces.GroupComponentService;
import ro.cs.tao.services.interfaces.WorkflowService;
import ro.cs.tao.services.model.execution.ExecutionJobInfo;
import ro.cs.tao.services.model.execution.ExecutionTaskInfo;
import ro.cs.tao.services.model.workflow.WorkflowInfo;
import ro.cs.tao.services.utils.WorkflowUtilities;
import ro.cs.tao.snap.xml.GraphParser;
import ro.cs.tao.subscription.WorkflowSubscription;
import ro.cs.tao.utils.ExceptionUtils;
import ro.cs.tao.utils.StringUtilities;
import ro.cs.tao.utils.Tuple;
import ro.cs.tao.utils.executors.MemoryUnit;
import ro.cs.tao.workflow.ParameterValue;
import ro.cs.tao.workflow.WorkflowDescriptor;
import ro.cs.tao.workflow.WorkflowNodeDescriptor;
import ro.cs.tao.workflow.WorkflowNodeGroupDescriptor;
import ro.cs.tao.workflow.enums.ComponentType;
import ro.cs.tao.workflow.enums.Status;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Implementation for Workflow entity service.
 *
 * @author Cosmin Cara
 */
@Service("workflowService")
public class WorkflowServiceImpl
        extends EntityService<WorkflowDescriptor> implements WorkflowService {

    @Autowired
    private WorkflowProvider workflowProvider;
    @Autowired
    private WorkflowNodeProvider workflowNodeProvider;
    @Autowired
    private TagProvider tagProvider;
    @Autowired
    private QueryProvider queryProvider;
    @Autowired
    private ExecutionJobProvider jobProvider;
    @Autowired
    private ExecutionTaskProvider taskProvider;
    @Autowired
    private DataSourceComponentProvider dataSourceComponentProvider;
    @Autowired
    private ProcessingComponentProvider componentProvider;
    @Autowired
    private ComponentService componentService;
    @Autowired
    private GroupComponentService groupComponentService;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private SourceDescriptorRepository sourceDescriptorRepository;
    @Autowired
    private TargetDescriptorRepository targetDescriptorRepository;
    @Autowired
    private WorkflowSubscriptionProvider workflowSubscriptionProvider;

    private Logger logger = Logger.getLogger(WorkflowService.class.getName());
    //region Workflow
    @Override
    public WorkflowDescriptor findById(Long id) {
        return workflowProvider.get(id);
    }

    @Override
    public WorkflowDescriptor getFullDescriptor(Long id) {
        return workflowProvider.loadWorkflowDescriptor(id);
    }

    @Override
    public WorkflowDescriptor getDescriptor(String name) {
        return workflowProvider.getByName(name);
    }

    @Override
    public List<WorkflowDescriptor> list() {
        return workflowProvider.list();
    }

    @Override
    public List<WorkflowDescriptor> list(Iterable<Long> ids) {
        return workflowProvider.list(ids);
    }

    @Override
    public List<WorkflowInfo> getUserWorkflowsByStatus(String user, Status status) {
        final List<WorkflowDescriptor> list = workflowProvider.listUserWorkflowsByStatus(user, status.value());
        final Map<Long, String> images = workflowProvider.getWorkflowImages(list.stream()
                                                                                .map(WorkflowDescriptor::getId)
                                                                                .collect(Collectors.toSet()));
        return ServiceTransformUtils.toWorkflowInfos(list, images);
    }

    @Override
    public List<WorkflowInfo> getUserPublishedWorkflowsByVisibility(String user, Visibility visibility) {
        final List<WorkflowDescriptor> list = workflowProvider.listUserPublishedWorkflowsByVisibility(user, visibility.value());
        final Map<Long, String> images = workflowProvider.getWorkflowImages(list.stream()
                                                                                .map(WorkflowDescriptor::getId)
                                                                                .collect(Collectors.toSet()));
        return ServiceTransformUtils.toWorkflowInfos(list, images);
    }

    @Override
    public List<WorkflowInfo> getOtherPublicWorkflows(String user) {
        final List<WorkflowDescriptor> list = workflowProvider.listOtherPublicWorkflows(user);
        final Map<Long, String> images = workflowProvider.getWorkflowImages(list.stream()
                                                                                .map(WorkflowDescriptor::getId)
                                                                                .collect(Collectors.toSet()));
        return ServiceTransformUtils.toWorkflowInfos(list, images);
    }

    @Override
    public List<WorkflowInfo> getPublicWorkflows() {
        final List<WorkflowDescriptor> list;
        if (!isDevModeEnabled()) {
            list = workflowProvider.listPublic();
        } else {
            list = MockData.getMockWorkflows();
        }
        final Map<Long, String> images = workflowProvider.getWorkflowImages(list.stream()
                                                                                .map(WorkflowDescriptor::getId)
                                                                                .collect(Collectors.toSet()));
        return ServiceTransformUtils.toWorkflowInfos(list, images);
    }

    @Override
    public List<WorkflowInfo> getUserVisibleWorkflows(String userId) {
        final List<WorkflowDescriptor> list = workflowProvider.listUserVisible(userId);
        final Map<Long, String> images = workflowProvider.getWorkflowImages(list.stream()
                                                                                .map(WorkflowDescriptor::getId)
                                                                                .collect(Collectors.toSet()));
        return ServiceTransformUtils.toWorkflowInfos(list, images);
    }

    @Override
    public WorkflowInfo getWorkflowInfo(long workflowId) {
        return ServiceTransformUtils.toWorkflowInfo(workflowProvider.get(workflowId),
                                                    workflowProvider.getWorkflowImage(workflowId));
    }

    @Override
    public List<Tag> getWorkflowTags() {
        return tagProvider.list(TagType.WORKFLOW);
    }

    @Override
    public List<WorkflowInfo> getUserSubscribedWorkflows(String userId) {
        final List<WorkflowSubscription> subscriptions = workflowSubscriptionProvider.getByUser(userId);
        final List<WorkflowInfo> workflowInfos = new ArrayList<>();
        if (subscriptions != null && !subscriptions.isEmpty()) {
            final List<WorkflowDescriptor> list = workflowProvider.list(subscriptions.stream()
                                                                                     .map(WorkflowSubscription::getWorkflowId)
                                                                                     .collect(Collectors.toSet()));
            final Map<Long, String> images = workflowProvider.getWorkflowImages(list.stream().map(WorkflowDescriptor::getId).collect(Collectors.toSet()));
            workflowInfos.addAll(ServiceTransformUtils.toWorkflowInfos(list, images));
        }
        return workflowInfos;
    }

    @Override
    public long subscribeToWorkflow(String userId, long workflowId) {
        WorkflowSubscription subscription = workflowSubscriptionProvider.getByUserAndWorkflow(userId, workflowId);
        if (subscription == null) {
            subscription = new WorkflowSubscription();
            subscription.setUserId(userId);
            subscription.setWorkflowId(workflowId);
            subscription.setCreated(LocalDateTime.now());
            try {
                subscription = workflowSubscriptionProvider.save(subscription);
            } catch (PersistenceException e) {
                logger.severe(e.getMessage());
                return -1;
            }
        }
        return subscription.getId();
    }

    @Override
    public void unsubscribeFromWorkflow(String userId, long workflowId) {
        final WorkflowSubscription subscription = workflowSubscriptionProvider.getByUserAndWorkflow(userId, workflowId);
        if (subscription != null) {
            try {
                workflowSubscriptionProvider.delete(subscription.getId());
            } catch (PersistenceException e) {
                logger.severe(e.getMessage());
            }
        }
    }

    @Override
    public WorkflowDescriptor save(WorkflowDescriptor object) {
        if (object != null) {
            List<WorkflowNodeDescriptor> nodes = object.getNodes();
            if (nodes != null) {
                nodes.forEach(node -> node.setWorkflow(object));
                nodes.forEach(node -> WorkflowUtilities.ensureUniqueName(object, node));
            }
            validate(object);
            try {
                //addTagsIfNew(object);
                return workflowProvider.save(object);
            } catch (PersistenceException e) {
                logger.severe(e.getMessage());
                return null;
            }
        }
        return null;
    }

    @Override
    public WorkflowDescriptor update(WorkflowDescriptor object) throws PersistenceException {
        WorkflowDescriptor existing = workflowProvider.get(object.getId());
        existing.setName(object.getName());
        existing.setPath(object.getPath());
        existing.setStatus(object.getStatus());
        existing.setUserId(object.getUserId());
        existing.setVisibility(object.getVisibility());
        existing.setxCoord(object.getxCoord());
        existing.setyCoord(object.getyCoord());
        existing.setZoom(object.getZoom());
        existing.setTags(object.getTags());
        existing.setActive(object.isActive());
        existing.setDescription(object.getDescription());
        //addTagsIfNew(object);
        return workflowProvider.update(existing);
    }

    @Override
    public void delete(Long id) throws PersistenceException {
        WorkflowDescriptor descriptor = findById(id);
        if (descriptor != null) {
            descriptor.setActive(false);
            update(descriptor);
        }
    }

    @Override
    public WorkflowDescriptor tag(Long id, List<String> tags) throws PersistenceException {
        WorkflowDescriptor entity = findById(id);
        if (entity == null) {
            throw new PersistenceException(String.format("Workflow with id '%s' not found", id));
        }
        if (tags != null && !tags.isEmpty()) {
            Set<String> existingTags = tagProvider.list(TagType.WORKFLOW).stream()
                    .map(Tag::getText).collect(Collectors.toSet());
            tags.stream().filter(t -> !existingTags.contains(t)).forEach(t -> {
                try {
                    tagProvider.save(new Tag(TagType.WORKFLOW, t));
                } catch (PersistenceException e) {
                    logger.warning(ExceptionUtils.getStackTrace(logger, e));
                }
            });
            entity.setTags(tags);
            return update(entity);
        }
        return entity;
    }

    @Override
    public WorkflowDescriptor unTag(Long id, List<String> tags) throws PersistenceException {
        WorkflowDescriptor entity = findById(id);
        if (entity == null) {
            throw new PersistenceException(String.format("Workflow with id '%s' not found", id));
        }
        if (tags != null && !tags.isEmpty()) {
            List<String> entityTags = entity.getTags();
            if (entityTags != null) {
                entityTags.removeAll(tags);
                entity.setTags(entityTags);
                return update(entity);
            }
        }
        return entity;
    }
    //endregion
    //region Nodes
    @Override
    public WorkflowNodeDescriptor addNode(long workflowId, WorkflowNodeDescriptor nodeDescriptor) throws PersistenceException {
        if (nodeDescriptor == null) {
            throw new PersistenceException("Cannot add a null node");
        }
        if (nodeDescriptor.getId() != null && nodeDescriptor.getId() != 0) {
            throw new PersistenceException(String.format("Node [id:%s, name:%s] is not a new node",
                                                         nodeDescriptor.getId(), nodeDescriptor.getName()));
        }
        WorkflowDescriptor workflow = workflowProvider.get(workflowId);
        if (workflow == null) {
            throw new PersistenceException("Node is not attached to an existing workflow");
        }
        WorkflowUtilities.ensureUniqueName(workflow, nodeDescriptor);
        List<String> validationErrors = new ArrayList<>();
        validateNode(workflow, nodeDescriptor, validationErrors);
        if (!validationErrors.isEmpty()) {
            throw new PersistenceException(String.format("Node contains errors: [%s]",
                                                         String.join(",", validationErrors)));
        }
        return workflowNodeProvider.save(nodeDescriptor, workflow);
    }

    @Override
    public WorkflowNodeDescriptor updateNode(long workflowId, WorkflowNodeDescriptor nodeDescriptor) throws PersistenceException {
        if (nodeDescriptor == null) {
            throw new PersistenceException("Cannot update a null node");
        }
        if (nodeDescriptor.getId() == null) {
            throw new PersistenceException(String.format("Node [name:%s] is not an existing node",
                                                         nodeDescriptor.getName()));
        }
        WorkflowDescriptor workflow = workflowProvider.get(workflowId);
        if (workflow == null) {
            throw new PersistenceException("Node is not attached to an existing workflow");
        }
        nodeDescriptor.setWorkflow(workflow);
        WorkflowUtilities.ensureUniqueName(workflow, nodeDescriptor);
        List<String> validationErrors = new ArrayList<>();
        validateNode(workflow, nodeDescriptor, validationErrors);
        if (!validationErrors.isEmpty()) {
            throw new PersistenceException(String.format("Node contains errors: [%s]",
                                                         String.join(",", validationErrors)));
        }
        return workflowNodeProvider.update(nodeDescriptor);
    }

    @Override
    public List<WorkflowNodeDescriptor> updateNodes(long workflowId, List<WorkflowNodeDescriptor> nodeDescriptors) throws PersistenceException {
        if (nodeDescriptors == null || nodeDescriptors.isEmpty()) {
            throw new PersistenceException("Cannot update an empty list");
        }
        List<WorkflowNodeDescriptor> updatedNodes = new ArrayList<>();
        for (WorkflowNodeDescriptor node : nodeDescriptors) {
            updatedNodes.add(updateNode(workflowId, node));
        }
        return updatedNodes;
    }

    @Override
    public void updateNodesPositions(long workflowId, Map<Long, float[]> positions) throws PersistenceException {
        if (positions != null && !positions.isEmpty()) {
            /*List<WorkflowNodeDescriptor> nodes = workflowNodeProvider.list(positions.keySet());
            for (WorkflowNodeDescriptor node : nodes) {
                if (!node.getWorkflow().getId().equals(workflowId)) {
                    throw new PersistenceException(String.format("Node with id '%s' doesn't belong to workflow with id '%s'",
                                                                 node.getId(), workflowId));
                }
                float[] coordinates = positions.get(node.getId());
                if (coordinates == null || coordinates.length != 2) {
                    throw new PersistenceException(String.format("Invalid coordinates for node with id '%s'. Expected 2, found %s",
                                                                 node.getId(), coordinates == null ? "[null]" : Arrays.toString(coordinates)));
                }
                node.setxCoord(coordinates[0]);
                node.setyCoord(coordinates[1]);
                workflowNodeProvider.update(node);
            }*/
            for (Map.Entry<Long, float[]> entry : positions.entrySet()) {
                workflowNodeProvider.updatePosition(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public void removeNode(long workflowId, WorkflowNodeDescriptor nodeDescriptor) throws PersistenceException {
        if (nodeDescriptor == null) {
            throw new PersistenceException("Cannot remove a null node");
        }
        if (nodeDescriptor.getId() == null) {
            throw new PersistenceException(String.format("Node [name:%s] is not an existing node",
                                                         nodeDescriptor.getName()));
        }
        WorkflowDescriptor workflow = workflowProvider.get(workflowId);
        if (workflow == null) {
            throw new PersistenceException("Node is not attached to an existing workflow");
        }
        nodeDescriptor.setWorkflow(workflow);
        // delete any incoming links the node may have
        Set<ComponentLink> links = nodeDescriptor.getIncomingLinks();
        if (links != null && !links.isEmpty()) {
            ComponentLink[] linkArray = new ComponentLink[links.size()];
            linkArray = links.toArray(linkArray);
            for (ComponentLink link : linkArray) {
                nodeDescriptor.removeLink(link);
            }
        }
        // delete any outgoing links
        List<WorkflowNodeDescriptor> allNodes = workflow.getNodes();
        List<WorkflowNodeDescriptor> children = workflow.findChildren(allNodes, nodeDescriptor);
        if (children != null) {
            for (WorkflowNodeDescriptor child : children) {
                Set<ComponentLink> incomingLinks = child.getIncomingLinks();
                incomingLinks = incomingLinks.stream()
                                             .filter(l -> l.getSourceNodeId() == nodeDescriptor.getId())
                                             .collect(Collectors.toSet());
                for (ComponentLink link : incomingLinks) {
                    child.removeLink(link);
                }
                workflowNodeProvider.update(child);
            }
        }
        workflowNodeProvider.update(nodeDescriptor);

        if (ComponentType.DATASOURCE.equals(nodeDescriptor.getComponentType())) {
            try {
                DataSourceComponent component = (DataSourceComponent) WorkflowUtilities.findComponent(nodeDescriptor);
                Query query = queryProvider.get(SessionStore.currentContext().getPrincipal().getName(),
                                                component.getSensorName(), component.getDataSourceName(),
                                                nodeDescriptor.getId());
                if (query != null) {
                    queryProvider.delete(query);
                }
            } catch (PersistenceException pex) {
                logger.warning(String.format("Component [%s] not found. The workflow node will be removed",
                                             nodeDescriptor.getComponentId()));
            }
        }

        workflow.removeNode(nodeDescriptor);
        workflowProvider.update(workflow);
    }
    //endregion
    //region Links
    @Override
    public WorkflowNodeDescriptor addLink(long sourceNodeId, String sourceTargetId,
                                      long targetNodeId, String targetSourceId) throws PersistenceException {
        if (sourceTargetId == null || targetSourceId == null || sourceNodeId == 0 || targetNodeId == 0) {
            throw new PersistenceException("Invalid link data");
        }
        WorkflowNodeDescriptor sourceNode = workflowNodeProvider.get(sourceNodeId);
        WorkflowNodeDescriptor targetNode = workflowNodeProvider.get(targetNodeId);
        if (targetNode == null) {
            throw new PersistenceException("Target node does not exist");
        }
        TaoComponent sourceComponent = WorkflowUtilities.findComponent(sourceNode);
        if (sourceComponent == null) {
            throw new PersistenceException("Source component not found");
        }
        TaoComponent targetComponent = WorkflowUtilities.findComponent(targetNode);
        if (targetComponent == null) {
            throw new PersistenceException("Target component not found");
        }
        TargetDescriptor linkInput = sourceComponent.findDescriptor(sourceTargetId);
        SourceDescriptor linkOutput = targetComponent.findDescriptor(targetSourceId);
        if (linkInput == null) {
            throw new PersistenceException(String.format("Target descriptor [%s] not found in source component %s",
                                                         sourceTargetId, sourceComponent.getId()));
        }
        if (linkOutput == null) {
            throw new PersistenceException(String.format("Source descriptor [%s] not found in target component %s",
                                                         targetSourceId, targetComponent.getId()));
        }
        WorkflowNodeGroupDescriptor sourceGroupNode = workflowNodeProvider.getGroupNode(sourceNodeId);
        WorkflowNodeGroupDescriptor targetGroupNode = workflowNodeProvider.getGroupNode(targetNodeId);
        if (sourceGroupNode != null && targetGroupNode == null) {
            // link from a node inside a group to a node outside the group
            return addLinkToOutsideNode(targetNodeId, linkInput, linkOutput, sourceGroupNode, sourceNode);
        } else if (sourceGroupNode == null && targetGroupNode != null) {
            // link to a node inside a group from a node outside the group
            return addLinkFromOutsideNode(sourceNodeId, linkInput, linkOutput, targetGroupNode, targetNode);
        } else {
            try {
                ComponentLink link = new ComponentLink(sourceNodeId, linkInput, linkOutput);
                Set<ComponentLink> links = targetNode.getIncomingLinks();
                if (links == null) {
                    links = new HashSet<>();
                }
                links.add(link);
                targetNode.setIncomingLinks(links);
                return updateNode(targetNode.getWorkflow().getId(), targetNode);
            } catch (Exception e) {
                throw new PersistenceException(e);
            }
        }
    }

    @Override
    public WorkflowNodeDescriptor removeLink(long nodeId, ComponentLink link) throws PersistenceException {
        if (link == null) {
            throw new PersistenceException("Cannot remove a null link");
        }
        if (link.getSourceNodeId() == 0) {
            throw new PersistenceException("Link doesn't have a source node set");
        }
        if (nodeId == 0) {
            throw new PersistenceException("Link doesn't have a target node set");
        }
        if (link.getInput() == null) {
            throw new PersistenceException("Invalid link input");
        }
        if (link.getOutput() == null) {
            throw new PersistenceException("Invalid link output");
        }
        WorkflowNodeDescriptor node = workflowNodeProvider.get(nodeId);
        if (node == null) {
            throw new PersistenceException("Parent node does not exist");
        }
        node.getIncomingLinks().removeIf(l -> l.equals(link));
        return workflowNodeProvider.update(node);
    }
    //endregion
    //region Groups
    @Override
    public WorkflowNodeDescriptor group(long workflowId, String groupName, List<WorkflowNodeDescriptor> nodes) throws PersistenceException {
        WorkflowNodeGroupDescriptor grpNode = new WorkflowNodeGroupDescriptor();
        Rectangle2D.Float envelope = WorkflowUtilities.computeEnvelope(nodes);
        grpNode.setxCoord(envelope.x);
        grpNode.setyCoord(envelope.y);
        if (groupName != null) {
            grpNode.setName(groupName);
        } else {
            grpNode.setName("Group " + UUID.randomUUID().toString());
        }
        grpNode.setCreated(LocalDateTime.now());
        grpNode.setPreserveOutput(true);
        //grpNode = (WorkflowNodeGroupDescriptor) workflowNodeProvider.save(grpNode, workflowProvider.get(workflowId));
        return group(workflowId, grpNode, nodes);
    }

    @Override
    public WorkflowNodeDescriptor group(long workflowId, String groupName, Long[] nodeIds) throws PersistenceException {
        return group(workflowId, groupName, workflowNodeProvider.list(Arrays.asList(nodeIds)));
    }

    @Override
    public WorkflowNodeDescriptor group(long workflowId, WorkflowNodeGroupDescriptor groupDescriptor, List<WorkflowNodeDescriptor> nodes) throws PersistenceException {
        if (nodes == null || nodes.size() == 0) {
            throw new PersistenceException("Empty node group");
        }
        return TransactionalMethod.withExceptionType(PersistenceException.class).execute(() -> {
            // Step 1: determine nodes that don't have incoming links or have links only from nodes outside the node list
            final List<WorkflowNodeDescriptor> firstLevelNodes = WorkflowUtilities.findFirstLevel(nodes);
            final List<SourceDescriptor> sources = new ArrayList<>();
            for (WorkflowNodeDescriptor node : firstLevelNodes) {
                final ProcessingComponent component = componentService.findById(node.getComponentId());
                sources.addAll(component.getSources());
            }
            // Step 2: determine nodes that don't have outgoing links (i.e. not referred by other nodes) or have links only to nodes outside the node list
            final List<WorkflowNodeDescriptor> terminalNodes = WorkflowUtilities.findTerminals(nodes);
            final List<TargetDescriptor> targets = new ArrayList<>();
            for (WorkflowNodeDescriptor node : terminalNodes) {
                final ProcessingComponent component = componentService.findById(node.getComponentId());
                targets.addAll(component.getTargets());
            }
            // Step 3: create the group component and add nodes to the node group
            GroupComponent groupComponent = GroupComponent.create(sources, targets);
            groupComponent = groupComponentService.save(groupComponent);
            groupDescriptor.setComponentId(groupComponent.getId());
            groupDescriptor.setComponentType(ComponentType.GROUP);
            WorkflowDescriptor workflow = workflowProvider.get(workflowId);
            for (WorkflowNodeDescriptor node : nodes) {
                groupDescriptor.addNode(node);
            }
            final int level = firstLevelNodes.stream().min(Comparator.comparingInt(WorkflowNodeDescriptor::getLevel)).get().getLevel();
            groupDescriptor.setLevel(level);
            WorkflowNodeGroupDescriptor updated = (WorkflowNodeGroupDescriptor) workflowNodeProvider.save(groupDescriptor, workflow);

            // Step 4: transfer the incoming links of the first level nodes to the group
            for (WorkflowNodeDescriptor node : firstLevelNodes) {
                final Set<ComponentLink> incomingLinks = node.getIncomingLinks();
                if (incomingLinks != null) {
                    for (ComponentLink link : incomingLinks) {
                        SourceDescriptor originalSource = link.getOutput();
                        SourceDescriptor groupSource = groupComponent.findDescriptor(originalSource.getId());
                        link.setOutput(groupSource);
                        /*addLink(link.getSourceNodeId(), link.getInput().getId(),
                                updated.getId(), groupSource.getId());*/
                    }
                    workflowNodeProvider.update(node);
                }
            }

            // Step 5: transfer the outgoing links of the last level nodes to the group
            final Set<Tuple<Tuple<Long, String>, Tuple<Long, String>>> linksToAdd = new LinkedHashSet<>();
            final Set<WorkflowNodeDescriptor> outLinkedNodes = new LinkedHashSet<>();
            terminalNodes.forEach(n -> outLinkedNodes.addAll(WorkflowUtilities.findLinkedNodes(workflow, terminalNodes)));
            for (WorkflowNodeDescriptor outNode : outLinkedNodes) {
                final Set<ComponentLink> incomingLinks = new HashSet<>(outNode.getIncomingLinks());
                for (ComponentLink link : incomingLinks) {
                    TargetDescriptor originalTarget = link.getInput();
                    TargetDescriptor groupTarget = groupComponent.findDescriptor(originalTarget.getId());
                    link.setInput(groupTarget);
                    link.setSourceNodeId(updated.getId());
                }
                workflowNodeProvider.update(outNode);
            }
            return updated;
        });
    }

    @Override
    public WorkflowNodeDescriptor updateGroup(WorkflowNodeGroupDescriptor groupDescriptor) throws PersistenceException {
        return workflowNodeProvider.update(groupDescriptor);
    }

    @Override
    public void ungroup(WorkflowNodeGroupDescriptor groupDescriptor, boolean removeChildren) throws PersistenceException {
        if (groupDescriptor == null) {
            throw new PersistenceException("groupDescriptor [null]");
        }
        TransactionalMethod.withExceptionType(PersistenceException.class).execute(() -> {
            final List<WorkflowNodeDescriptor> nodes = Collections.unmodifiableList(groupDescriptor.getNodes());
            final WorkflowDescriptor workflow = groupDescriptor.getWorkflow();
            final Set<ComponentLink> linksToRemove = new HashSet<>();
            if (removeChildren) {
                if (workflow != null) {
                    for (WorkflowNodeDescriptor node : nodes) {
                        Set<ComponentLink> links = node.getIncomingLinks();
                        if (links != null) {
                            linksToRemove.addAll(links);
                        }
                        groupDescriptor.removeNode(node);
                        workflow.removeNode(node);
                    }
                    workflowNodeProvider.update(groupDescriptor);
                    workflow.removeNode(groupDescriptor);
                    workflowProvider.update(workflow);
                }
            } else {
                for (WorkflowNodeDescriptor node : nodes) {
                    groupDescriptor.removeNode(node);
                }
                // Transfer the incoming links of the group to the first level nodes
                final Set<ComponentLink> groupLinks = groupDescriptor.getIncomingLinks();
                if (groupLinks != null) {
                    for (ComponentLink link : groupLinks) {
                        SourceDescriptor groupSource = link.getOutput();
                        WorkflowNodeDescriptor sourceNode = WorkflowUtilities.findGroupSourceOwner(groupSource, groupDescriptor);
                        SourceDescriptor sourceDescriptor = WorkflowUtilities.findSourceById(groupSource.getReferencedSourceDescriptorId(), sourceNode);
                        if (sourceDescriptor == null) {
                            logger.warning(String.format("Cannot determine originating source descriptor [groupNode=%s, groupSource=%s, sourceNode=%s]",
                                                         groupDescriptor.getName(), groupSource.getName(), sourceNode.getName()));
                            continue;
                        }
                        link.setOutput(sourceDescriptor);
                        workflowNodeProvider.update(sourceNode);
                        /*addLink(link.getSourceNodeId(), link.getInput().getId(),
                                sourceNode.getId(), sourceDescriptor.getId());
                        linksToRemove.add(link);*/
                    }
                    groupDescriptor.setIncomingLinks(null);
                }
                /*for (ComponentLink link : linksToRemove) {
                    removeLink(groupDescriptor.getId(), link);
                }
                linksToRemove.clear();*/
                // Transfer the outgoing links of the group to the last level nodes
                final List<WorkflowNodeDescriptor> linkedNodes = WorkflowUtilities.findLinkedNodes(workflow, new ArrayList<WorkflowNodeDescriptor>() {{
                    add(groupDescriptor);
                }});
                Set<ComponentLink> incomingLinks;
                for (WorkflowNodeDescriptor outNode : linkedNodes) {
                    incomingLinks = outNode.getIncomingLinks();
                    for (ComponentLink link : incomingLinks) {
                        TargetDescriptor groupTarget = link.getInput();
                        WorkflowNodeDescriptor targetNode = WorkflowUtilities.findGroupTargetOwner(groupTarget, groupDescriptor);
                        TargetDescriptor targetDescriptor = WorkflowUtilities.findTargetById(groupTarget.getReferencedTargetDescriptorId(), targetNode);
                        if (targetDescriptor == null) {
                            logger.warning(String.format("Cannot determine originating target descriptor [groupNode=%s, groupTarget=%s, targetNode=%s]",
                                                         groupDescriptor.getName(), groupTarget.getName(), targetNode.getName()));
                            continue;
                        }
                        link.setInput(targetDescriptor);
                        link.setSourceNodeId(targetNode.getId());
                        targetNode.addLink(link);
                        workflowNodeProvider.update(targetNode);
                        /*addLink(targetNode.getId(), targetDescriptor.getId(),
                                outNode.getId(), link.getOutput().getId());*/
                        linksToRemove.add(link);
                    }
                    for (ComponentLink link : linksToRemove) {
                        removeLink(outNode.getId(), link);
                    }
                    workflowNodeProvider.update(outNode);
                }
                workflow.removeNode(groupDescriptor);
                workflowProvider.update(workflow);
                workflowNodeProvider.delete(groupDescriptor.getId());
            }
            return null;
        });
    }
    //endregion
    //region Clone and import
    @Override
    public WorkflowDescriptor clone(WorkflowDescriptor workflow, boolean temporary) throws PersistenceException {
        if (workflow == null) {
            return null;
        }
        return TransactionalMethod.withExceptionType(PersistenceException.class).execute(() -> {
            WorkflowDescriptor clone = new WorkflowDescriptor();
            clone.setName(workflow.getName() + " - " + System.currentTimeMillis());
            clone.setDescription(workflow.getDescription());
            clone.setStatus(Status.DRAFT);
            clone.setCreated(LocalDateTime.now());
            clone.setActive(workflow.isActive());
            clone.setUserId(SessionStore.currentContext().getPrincipal().getName());
            clone.setVisibility(Visibility.PRIVATE);
            clone.setCreatedFromWorkflowId(workflow.getId());
            clone.setTemporary(temporary);
            clone = workflowProvider.save(clone);
            // key: old id, value: new node
            final Map<Long, WorkflowNodeDescriptor> cloneMap = new HashMap<>();
            final List<WorkflowNodeDescriptor> workflowNodeDescriptors = workflow.orderNodes(workflow.getNodes(), null);
            for (WorkflowNodeDescriptor node : workflowNodeDescriptors) {
                WorkflowNodeDescriptor clonedNode = node instanceof WorkflowNodeGroupDescriptor
                                                    ? new WorkflowNodeGroupDescriptor()
                                                    : new WorkflowNodeDescriptor();
                clonedNode.setWorkflow(clone);
                clonedNode.setCreatedFromNodeId(node.getId());
                clonedNode.setName(node.getName());
                clonedNode.setDescription(node.getDescription());
                clonedNode.setxCoord(node.getxCoord());
                clonedNode.setyCoord(node.getyCoord());
                clonedNode.setComponentId(node.getComponentId());
                clonedNode.setComponentType(node.getComponentType());
                clonedNode.setCreated(LocalDateTime.now());
                clonedNode.setPreserveOutput(node.getPreserveOutput());
                clonedNode.setBehavior(node.getBehavior());
                clonedNode.setLevel(node.getLevel());
                List<ParameterValue> customValues = node.getCustomValues();
                if (customValues != null) {
                    clonedNode.setCustomValues(new ArrayList<>(customValues));
                }
                clonedNode = addNode(clone.getId(), clonedNode);
                cloneMap.put(node.getId(), clonedNode);
                //clonedNode = persistenceManager.updateWorkflowNodeDescriptor(clonedNode);
            }
            for (WorkflowNodeDescriptor node : workflowNodeDescriptors) {
                if (node instanceof WorkflowNodeGroupDescriptor) {
                    WorkflowNodeGroupDescriptor clonedNode = (WorkflowNodeGroupDescriptor) cloneMap.get(node.getId());
                    ((WorkflowNodeGroupDescriptor) node).getOrderedNodes()
                                                        .forEach(n -> clonedNode.addNode(cloneMap.get(n.getId())));
                    workflowNodeProvider.update(clonedNode);
                }
            }
            for (WorkflowNodeDescriptor node : workflowNodeDescriptors) {
                final Set<ComponentLink> links = node.getIncomingLinks();
                if (links != null) {
                    WorkflowNodeDescriptor clonedNode = cloneMap.get(node.getId());
                    for (ComponentLink link : links) {
                        WorkflowNodeDescriptor clonedSource = cloneMap.get(link.getSourceNodeId());
                        TargetDescriptor input = targetDescriptorRepository.findById(link.getInput().getId()).get(); //WorkflowUtilities.findTarget(link.getInput().getId(), clonedSource);
                        SourceDescriptor output = sourceDescriptorRepository.findById(link.getOutput().getId()).get(); //WorkflowUtilities.findSource(link.getOutput().getId(), clonedNode);
                        ComponentLink clonedLink = new ComponentLink(clonedSource.getId(), input, output);
                        Set<ComponentLink> clonedLinks = clonedNode.getIncomingLinks();
                        if (clonedLinks == null) {
                            clonedLinks = new HashSet<>();
                        }
                        clonedLinks.add(clonedLink);
                        clonedNode.setIncomingLinks(clonedLinks);
                    }
                    clonedNode = workflowNodeProvider.update(clonedNode);
                }
            }
            return workflowProvider.get(clone.getId());
        });
    }

    @Override
    public WorkflowDescriptor createTemporaryWorkflow(long fromWorkflowId, String name, Map<Long, String> linksToAdd) throws PersistenceException {
        WorkflowDescriptor clone = null;
        if (linksToAdd != null) {
            // These inputs are not yet connected to the level 1 nodes, we need to create the nodes and the links
            final WorkflowDescriptor workflow = findById(fromWorkflowId);
            // Create a cloned workflow
            clone = clone(workflow, true);
            clone.setName(name);
            final Set<Long> ids = new HashSet<>();
            for (Map.Entry<Long, String> entry : linksToAdd.entrySet()) {
                // For each input create a node
                final DataSourceComponent sourceComponent = dataSourceComponentProvider.get(entry.getValue());
                final String sourceTargetId = sourceComponent.getTargets().get(0).getId();
                final WorkflowNodeDescriptor targetNode = workflowNodeProvider.findClonedNode(clone.getId(), entry.getKey());
                final TaoComponent targetComponent = TaskUtilities.getComponentFor(targetNode);
                final String targetSourceId = targetComponent.getSources().get(0).getId();
                WorkflowNodeDescriptor node;
                final List<WorkflowNodeDescriptor> descriptors = workflowNodeProvider.listByComponentId(clone.getId(), entry.getValue());
                if (descriptors == null || descriptors.isEmpty()) {
                    node = new WorkflowNodeDescriptor();
                    node.setWorkflow(clone);
                    node.setName(sourceComponent.getDataSourceName());
                    node.setComponentId(entry.getValue());
                    node.setComponentType(ComponentType.DATASOURCE);
                    node.setPreserveOutput(true);
                    node.setCreated(LocalDateTime.now());
                    node.setLevel(0);
                    node = addNode(clone.getId(), node);
                } else {
                    node = descriptors.get(0);
                }
                // and add a link from this new node to the given target source
                addLink(node.getId(), sourceTargetId, targetNode.getId(), targetSourceId);
                ids.add(node.getId());
            }
            // Increment the level of the other nodes
            clone.getNodes().forEach(n -> {
                if (!ids.contains(n.getId())) {
                    n.setLevel(n.getLevel() + 1);
                }
            });
            clone = update(clone);
        }
        return clone;
    }

    @Override
    public void deleteTemporaryWorkflow(long workflowId) throws PersistenceException {
        WorkflowDescriptor workflow = findById(workflowId);
        if (workflow != null) {
            final List<ExecutionJob> jobs = jobProvider.listByWorkflow(workflowId);
            if (jobs != null) {
                final List<WorkflowNodeDescriptor> nodes = workflow.getNodes();
                final Map<Long, Long> idMap = new HashMap<>();
                long originalWorkflowId = 0;
                for (WorkflowNodeDescriptor node : nodes) {
                    if (originalWorkflowId == 0 && node.getCreatedFromNodeId() != null) {
                        final WorkflowDescriptor original = workflowProvider.getByNodeId(node.getCreatedFromNodeId());
                        if (original == null) {
                            throw new PersistenceException("Cannot find the original workflow");
                        }
                        originalWorkflowId = original.getId();
                    }
                    idMap.put(node.getId(), node.getCreatedFromNodeId());
                }
                for (ExecutionJob job : jobs) {
                    final List<ExecutionTask> tasks = job.getTasks();
                    for (ExecutionTask task : tasks) {
                        final Long originalId = idMap.get(task.getWorkflowNodeId());
                        if (originalId != null) {
                            task.setWorkflowNodeId(originalId);
                            taskProvider.update(task);
                        }
                    }
                    job.setWorkflowId(originalWorkflowId);
                    jobProvider.update(job);
                }
            }
            workflowProvider.delete(workflow);
        }
    }

    @Override
    public WorkflowDescriptor importWorkflowNodes(WorkflowDescriptor master,
                                                  WorkflowDescriptor subWorkflow,
                                                  boolean keepDataSources) throws PersistenceException {
        return TransactionalMethod.withExceptionType(PersistenceException.class).execute(() -> {
            final Map<Long, WorkflowNodeDescriptor> cloneMap = new HashMap<>();
            final List<WorkflowNodeDescriptor> nodesToExport = subWorkflow.getOrderedNodes();
            final Set<Long> excludedIds = new HashSet<>();
            for (WorkflowNodeDescriptor node : nodesToExport) {
                // Data sources are not imported
                if (!keepDataSources && ComponentType.DATASOURCE.equals(node.getComponentType())) {
                    excludedIds.add(node.getId());
                    continue;
                }
                // create a clone of each source node
                WorkflowNodeDescriptor clonedNode = node instanceof WorkflowNodeGroupDescriptor ?
                                                    new WorkflowNodeGroupDescriptor() : new WorkflowNodeDescriptor();
                clonedNode.setWorkflow(master);
                clonedNode.setName(node.getName());
                clonedNode.setxCoord(node.getxCoord());
                clonedNode.setyCoord(node.getyCoord());
                clonedNode.setComponentId(node.getComponentId());
                clonedNode.setComponentType(node.getComponentType());
                clonedNode.setCreated(LocalDateTime.now());
                clonedNode.setPreserveOutput(node.getPreserveOutput());
                clonedNode.setBehavior(node.getBehavior());
                clonedNode.setLevel(node.getLevel());
                clonedNode.setCreatedFromNodeId(node.getId());
                List<ParameterValue> customValues = node.getCustomValues();
                if (customValues != null) {
                    clonedNode.setCustomValues(new ArrayList<>(customValues));
                }
                clonedNode = addNode(master.getId(), clonedNode);
                cloneMap.put(node.getId(), clonedNode);
            }
            // regroup target nodes according to source groups, if any
            for (WorkflowNodeDescriptor node : nodesToExport) {
                if (node instanceof WorkflowNodeGroupDescriptor) {
                    WorkflowNodeGroupDescriptor clonedNode = (WorkflowNodeGroupDescriptor) cloneMap.get(node.getId());
                    ((WorkflowNodeGroupDescriptor) node).getOrderedNodes()
                                                        .forEach(n -> clonedNode.addNode(cloneMap.get(n.getId())));
                    workflowNodeProvider.update(clonedNode);
                }
            }
            // re-create links for target nodes based on source node links
            for (WorkflowNodeDescriptor node : nodesToExport) {
                final Set<ComponentLink> links = node.getIncomingLinks();
                if (links != null) {
                    WorkflowNodeDescriptor clonedTarget = cloneMap.get(node.getId());
                    for (ComponentLink link : links) {
                        if (!excludedIds.contains(link.getSourceNodeId())) {
                            WorkflowNodeDescriptor clonedSource = cloneMap.get(link.getSourceNodeId());
                            TargetDescriptor input = WorkflowUtilities.findTarget(link.getInput().getId(), clonedSource);
                            SourceDescriptor output = WorkflowUtilities.findSource(link.getOutput().getId(), clonedTarget);
                            ComponentLink clonedLink = new ComponentLink(clonedSource.getId(), input, output);
                            Set<ComponentLink> clonedLinks = clonedTarget.getIncomingLinks();
                            if (clonedLinks == null) {
                                clonedLinks = new HashSet<>();
                            }
                            clonedLinks.add(clonedLink);
                            clonedTarget.setIncomingLinks(clonedLinks);
                        }
                    }
                    workflowNodeProvider.update(clonedTarget);
                }
            }
            // finally, create a group to hold the cloned sub-workflow
            group(master.getId(), subWorkflow.getName(), cloneMap.values().stream().map(WorkflowNodeDescriptor::getId).toArray(Long[]::new));
            return workflowProvider.get(master.getId());
        });
    }
    //endregion
    @Override
    protected void validateFields(WorkflowDescriptor entity, List<String> errors) {
        String value = entity.getName();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[name] cannot be empty");
        }
        List<WorkflowNodeDescriptor> nodes = entity.getNodes();
        if (nodes != null) {
            for (WorkflowNodeDescriptor node : nodes) {
                validateNode(entity, node, errors);
            }
        }
    }

    @Override
    public List<ExecutionJobInfo> getWorkflowExecutions(long workflowId) throws PersistenceException {
        final WorkflowDescriptor workflow = workflowProvider.get(workflowId);
        if (workflow == null) {
            throw new PersistenceException("There is no workflow having the given identifier " + String.valueOf(workflowId));
        }
        return ServiceTransformUtils.toJobInfos(jobProvider.listByWorkflow(workflowId));
    }

    @Override
    public List<ExecutionTaskInfo> getWorkflowExecutionTasks(long executionJobId) throws PersistenceException {
        final ExecutionJob workflowExecution = jobProvider.get(executionJobId);
        if (workflowExecution == null) {
            throw new PersistenceException("There is no workflow execution having the given identifier " + String.valueOf(executionJobId));
        }
        return ServiceTransformUtils.toTaskInfos(workflowExecution.orderedTasks());
    }

    @Override
    public Map<String, List<Parameter>> getWorkflowParameters(long workflowId) throws PersistenceException {
        Map<String, List<Parameter>> parameters = new LinkedHashMap<>();
        WorkflowDescriptor workflow = workflowProvider.get(workflowId);
        if (workflow == null) {
            throw new IllegalArgumentException(String.format("Non-existent workflow with id '%s'", workflowId));
        }
        final List<WorkflowNodeDescriptor> nodes = workflow.getOrderedNodes();
        final long distinct = nodes.stream().distinct().count();
        final boolean prefixWithId = nodes.size() != distinct;
        for (WorkflowNodeDescriptor node : nodes) {
            if (node instanceof WorkflowNodeGroupDescriptor) {
                continue;
            }
            ComponentType componentType = node.getComponentType();
            TaoComponent component = WorkflowUtilities.findComponent(node);
            List<Parameter> componentParams = new ArrayList<>();
            Class<?> paramType;
            switch (componentType) {
                case DATASOURCE:
                    DataSourceComponent dataSourceComponent = (DataSourceComponent) component;
                    Map<String, DataSourceParameter> params =
                            DataSourceManager.getInstance().getSupportedParameters(dataSourceComponent.getSensorName(),
                                                                                   dataSourceComponent.getDataSourceName());
                    if (params != null) {
                        final SourceDescriptor querySource = dataSourceComponent.getSources()
                                                                                .stream()
                                                                                .filter(s -> s.getName().equals(DataSourceComponent.QUERY_PARAMETER))
                                                                                .findFirst().orElse(null);
                        for (Map.Entry<String, DataSourceParameter> entry : params.entrySet()) {
                            DataSourceParameter descriptor = entry.getValue();
                            paramType = descriptor.getType();
                            final String paramName = entry.getKey();
                            if (!(CommonParameterNames.PRODUCT.equals(paramName) && querySource != null)) {
                                componentParams.add(new Parameter(paramName,
                                                                  descriptor.typeFriendlyName(),
                                                                  descriptor.getDefaultValue() != null ? String.valueOf(descriptor.getDefaultValue()) : null,
                                                                  paramType != null && paramType.isEnum() ?
                                                                  Parameter.stringValueSet(paramType.getEnumConstants()) :
                                                                  Parameter.stringValueSet(descriptor.getValueSet()),
                                                                  descriptor.isRequired()));

                            } else {
                                final String location = querySource.getDataDescriptor().getLocation();
                                String values = location != null ?
                                                location.startsWith("[") ?
                                                location : "[" + location + "]" : null;
                                componentParams.add(new Parameter(paramName,
                                                                  descriptor.typeFriendlyName(),
                                                                  values,
                                                                  paramType != null && paramType.isEnum() ?
                                                                  Parameter.stringValueSet(paramType.getEnumConstants()) :
                                                                  Parameter.stringValueSet(descriptor.getValueSet()),
                                                                  descriptor.isRequired()));

                            }
                        }
                    }
                    if (dataSourceComponent.getId().startsWith("product-set")) {
                        final SourceDescriptor querySource = dataSourceComponent.getSources()
                                                                                .stream()
                                                                                .filter(s -> s.getName().equals(DataSourceComponent.QUERY_PARAMETER))
                                                                                .findFirst().orElse(null);
                        DataDescriptor dataDescriptor = querySource.getDataDescriptor();
                        if (dataDescriptor != null) {
                            componentParams.add(new Parameter(DataSourceComponent.QUERY_PARAMETER, JavaType.STRING_ARRAY.friendlyName(), dataDescriptor.getLocation()));
                        }
                    } else {
                        Query query = queryProvider.get(SessionStore.currentContext().getPrincipal().getName(),
                                                        dataSourceComponent.getSensorName(),
                                                        dataSourceComponent.getDataSourceName(),
                                                        node.getId());
                        if (query != null) {
                            for (Map.Entry<String, String> e : query.getValues().entrySet()) {
                                componentParams.stream()
                                               .filter(cp -> cp.getName().equals(e.getKey()))
                                               .forEach(cp -> cp.setValue(e.getValue()));
                            }
                            componentParams.add(new Parameter("pageSize", JavaType.INT.friendlyName(), String.valueOf(query.getPageSize())));
                            componentParams.add(new Parameter("pageNumber", JavaType.INT.friendlyName(), String.valueOf(query.getPageNumber())));
                            componentParams.add(new Parameter("limit", JavaType.INT.friendlyName(), String.valueOf(query.getLimit())));
                        } else {
                            componentParams.add(new Parameter("pageSize", JavaType.INT.friendlyName(), "25"));
                            componentParams.add(new Parameter("pageNumber", JavaType.INT.friendlyName(), "1"));
                            componentParams.add(new Parameter("limit", JavaType.INT.friendlyName(), "25"));
                        }
                    }
                    break;
                case PROCESSING:
                    ProcessingComponent processingComponent = (ProcessingComponent) component;
                    List<ParameterDescriptor> descriptors = processingComponent.getParameterDescriptors();
                    for (ParameterDescriptor descriptor : descriptors) {
                        paramType = descriptor.getDataType();
                        componentParams.add(new Parameter(descriptor.getName(),
                                                          //paramType.getName(),
                                                          descriptor.typeFriendlyName(),
                                                          !"null".equals(descriptor.getDefaultValue()) ? descriptor.getDefaultValue() : null,
                                                          paramType.isEnum() ?
                                                                  Parameter.stringValueSet(paramType.getEnumConstants()) :
                                                                  Parameter.stringValueSet(descriptor.getValueSet()),
                                                          descriptor.isNotNull()));
                    }
                    break;
                case GROUP:
                    continue;
            }
            List<ParameterValue> customValues = node.getCustomValues();
            if (customValues != null) {
                for (ParameterValue value : customValues) {
                    componentParams.stream().filter(cp -> cp.getName().equals(value.getParameterName()))
                            .forEach(cp -> cp.setValue(value.getParameterValue()));
                }
            }
            parameters.put((prefixWithId ? node.getId() + ":" : "") + node.getName(), componentParams);
        }
        return parameters;
    }

    @Override
    public List<TargetDescriptor> getWorkflowOutputs(long workflowId) throws PersistenceException {
        WorkflowDescriptor workflow = workflowProvider.get(workflowId);
        if (workflow == null) {
            throw new IllegalArgumentException(String.format("Non-existent workflow with id '%s'", workflowId));
        }
        final List<TargetDescriptor> targetDescriptors = new ArrayList<>();
        final List<WorkflowNodeDescriptor> nodes = workflow.findLeaves(workflow.getOrderedNodes());
        if (nodes != null) {
            for (WorkflowNodeDescriptor node : nodes) {
                TaoComponent component = WorkflowUtilities.findComponent(node);
                if (component != null) {
                    targetDescriptors.addAll(component.getTargets());
                }
            }
        }

        return targetDescriptors;
    }

    @Override
    public WorkflowDescriptor snapGraphToWorkflow(String graphXml) throws SAXException {
        if (graphXml == null || graphXml.isEmpty()) {
            return null;
        }
        try {
            return GraphParser.parse(componentProvider, this, graphXml);
        } catch (Exception e) {
            throw new SAXException(e);
        }
    }

    @Override
    public String workflowToSnapGraph(WorkflowDescriptor workflowDescriptor) throws Exception {
        return null;
    }

    @Override
    public String getWorkflowImage(long id) {
        return workflowProvider.getWorkflowImage(id);
    }

    @Override
    public void addWorkflowImage(long id, String image) {
        workflowProvider.addWorkflowImage(id, image);
    }

    @Override
    public void updateWorkflowImage(long id, String newImage) {
        workflowProvider.updateWorkflowImage(id, newImage);
    }

    @Override
    public void deleteWorkflowImage(long id) {
        workflowProvider.deleteWorkflowImage(id);
    }

    @Override
    public WorkflowDescriptor importWorkflow(InputStream source) throws IOException, SerializationException, PersistenceException {
        final BaseSerializer<WorkflowDescriptor> serializer = SerializerFactory.create(WorkflowDescriptor.class, MediaType.JSON);
        final char[] buffer = new char[MemoryUnit.KB.value().intValue()];
        final StringBuilder builder = new StringBuilder();
        try (Reader reader = new InputStreamReader(source)) {
            for (int numRead; (numRead = reader.read(buffer, 0, buffer.length)) > 0;) {
                builder.append(buffer, 0, numRead);
            }
        }
        WorkflowDescriptor descriptor = serializer.deserialize(builder.toString());
        WorkflowDescriptor existing;
        if (descriptor.getId() != null) {
            existing = getFullDescriptor(descriptor.getId());
            // If the same id exists, let the persistence assign a new ID
            if (existing != null) {
                descriptor.setId(null);
            }
        }
        existing = workflowProvider.getByName(descriptor.getName());
        // Slightly modify the name because a workflow with the same name exists
        if (existing != null) {
            descriptor.setName(descriptor.getName() + " (imported " + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + ")");
        }
        // Verify that all the components from nodes are registered with this instance
        final List<WorkflowNodeDescriptor> nodes = descriptor.getNodes();
        for (WorkflowNodeDescriptor node : nodes) {
            final String componentId = node.getComponentId();
            if (StringUtilities.isNullOrEmpty(componentId) || componentService.findById(componentId) == null) {
                throw new IOException("Component for node [" + node.getName() + "] is missing or not registered");
            }
        }
        return workflowProvider.save(descriptor);
    }

    @Override
    public String exportWorkflow(WorkflowDescriptor descriptor) throws SerializationException {
        final BaseSerializer<WorkflowDescriptor> serializer = SerializerFactory.create(WorkflowDescriptor.class, MediaType.JSON);
        return serializer.serialize(descriptor);
    }

    /**
     * Processes the creation of a link between a node outside a group and a node inside a group.
     * The actual link is made between the source node and the group node.
     *
     * @param sourceNodeId     The source node identifier
     * @param linkInput         The input of the link
     * @param linkOutput        The output of the link
     * @param groupNode         The group node
     * @param targetNode              The target node
     * @return  The updated group node
     */
    private WorkflowNodeDescriptor addLinkFromOutsideNode(long sourceNodeId, TargetDescriptor linkInput, SourceDescriptor linkOutput,
                                     WorkflowNodeGroupDescriptor groupNode,
                                     WorkflowNodeDescriptor targetNode) throws PersistenceException {
        if (sourceNodeId <= 0) {
            throw new IllegalArgumentException("Invalid source node");
        }
        if (linkInput == null || linkOutput == null) {
            throw new IllegalArgumentException("Invalid descriptors for creating a link");
        }
        if (groupNode == null || targetNode == null) {
            throw new IllegalArgumentException("Invalid target node or node not part of a group");
        }
        WorkflowNodeDescriptor outsideNode = workflowNodeProvider.get(sourceNodeId);
        TaoComponent component = WorkflowUtilities.findComponent(outsideNode);
        if (!component.hasDescriptor(linkInput.getId())) {
        //if (component.getTargets().stream().noneMatch(t -> t.getId().equals(linkInput.getId()))) {
            throw new IllegalArgumentException(String.format("Target descriptor %s not found in node %d",
                                                             linkInput.getId(), sourceNodeId));
        }
        component = WorkflowUtilities.findComponent(targetNode);
        if (!component.hasDescriptor(linkOutput.getId())) {
        //if (component.getSources().stream().noneMatch(t -> t.getId().equals(linkOutput.getId()))) {
            throw new IllegalArgumentException(String.format("Source descriptor %s not found in node %d",
                                                             linkOutput.getId(), targetNode.getId()));
        }
        component = WorkflowUtilities.findComponent(groupNode);
        if (!component.hasDescriptor(linkOutput.getId())) {
        /*List<SourceDescriptor> sources = component.getSources();
        if (sources == null || sources.stream().noneMatch(s -> s.getId().equals(linkOutput.getId()))) {*/
            component.addSource(linkOutput);
        }
        groupComponentService.save((GroupComponent) component);
        groupNode.addLink(new ComponentLink(sourceNodeId, linkInput, linkOutput));
        return workflowNodeProvider.update(groupNode);
    }
    /**
     * Processes the creation of a link between a node inside a group and a node outside the group.
     * The actual link is made between the group node and the outside node.
     *
     * @param targetNodeId     The target node identifier
     * @param linkInput         The input of the link
     * @param linkOutput        The output of the link
     * @param groupNode         The group node
     * @param sourceNode              The target node
     * @return  The updated outside node
     */
    private WorkflowNodeDescriptor addLinkToOutsideNode(long targetNodeId, TargetDescriptor linkInput, SourceDescriptor linkOutput,
                                      WorkflowNodeGroupDescriptor groupNode,
                                      WorkflowNodeDescriptor sourceNode) throws PersistenceException {
        if (targetNodeId <= 0) {
            throw new IllegalArgumentException("Invalid target node");
        }
        if (linkInput == null || linkOutput == null) {
            throw new IllegalArgumentException("Invalid descriptors for creating a link");
        }
        if (groupNode == null || sourceNode == null) {
            throw new IllegalArgumentException("Invalid source node or node not part of a group");
        }
        WorkflowNodeDescriptor outsideNode = workflowNodeProvider.get(targetNodeId);
        TaoComponent component = WorkflowUtilities.findComponent(outsideNode);
        if (!component.hasDescriptor(linkOutput.getId())) {
        //if (component.getSources().stream().noneMatch(s -> s.getId().equals(linkOutput.getId()))) {
            throw new IllegalArgumentException(String.format("Source descriptor %s not found in node %d",
                                                             linkOutput.getId(), targetNodeId));
        }
        component = WorkflowUtilities.findComponent(sourceNode);
        if (!component.hasDescriptor(linkInput.getId())) {
            throw new IllegalArgumentException(String.format("Target descriptor %s not found in node %d",
                                                             linkInput.getId(), sourceNode.getId()));
        }
        component = WorkflowUtilities.findComponent(groupNode);
        if (!component.hasDescriptor(linkInput.getId())) {
            component.addTarget(linkInput);
        }
        groupComponentService.save((GroupComponent) component);
        outsideNode.addLink(new ComponentLink(targetNodeId, linkInput, linkOutput));
        return workflowNodeProvider.update(outsideNode);
    }

    private void validateNode(WorkflowDescriptor workflow, WorkflowNodeDescriptor node, List<String> errors) {
        // Validate simple fields
        String value = node.getName();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[node.name] cannot be empty");
        }
        final List<WorkflowNodeDescriptor> workflowNodes = workflow.getNodes();
        if (workflowNodes.stream().anyMatch(n -> n.getName().equals(node.getName()) && !n.getId().equals(node.getId()))) {
            errors.add("[node.name] there is another node with the same name");
        }
        final Set<ComponentLink> incomingLinks = node.getIncomingLinks();
        if (incomingLinks != null && !incomingLinks.isEmpty()) {
            if (incomingLinks.stream()
                    .noneMatch(l -> l.getOutput().getParentId().equals(node.getComponentId()))) {
                errors.add(String.format("[%s.incomingLinks] contains some invalid node identifiers",
                                         node.getId()));
            }
            incomingLinks.forEach(n -> {
                if (workflowNodes.stream()
                        .noneMatch(nd -> nd.getComponentId().equals(n.getInput().getParentId()))) {
                    errors.add(String.format("[%s.incomingLinks] contains one or more invalid node identifiers",
                                             node.getId()));
                }
            });
        }
        // Validate the attached processing component
        if (node.getComponentType() == null) {
            errors.add("[node.componentType] is empty");
        } else {
            value = node.getComponentId();
            if (value == null || value.trim().isEmpty()) {
                errors.add("[node] is not linked to a processing component");
            } else {
                TaoComponent component = null;
                try {
                    component = WorkflowUtilities.findComponent(node);
                } catch (PersistenceException e) {
                    logger.warning(e.getMessage());
                }
                if (component == null) {
                    errors.add("[node.componentId] component does not exist");
                } else {
                    List<ParameterValue> customValues = node.getCustomValues();
                    // Validate custom parameter values for the attached component
                    if (customValues != null && !customValues.isEmpty()) {
                        List<ParameterDescriptor> descriptors = null;
                        if (component instanceof ProcessingComponent) {
                            descriptors = ((ProcessingComponent) component).getParameterDescriptors();
                        }
                        if (descriptors != null && !descriptors.isEmpty()) {
                            final List<ParameterDescriptor> descriptorList = new ArrayList<>(descriptors);
                            component.getTargets().forEach(t -> {
                                descriptorList.addAll(t.toParameter());
                            });
                            customValues.forEach(v -> {
                                // NodeAffinity is not a parameter having a descriptor, but a property of the TaoComponent class
                                if (!"nodeAffinity".equals(v.getParameterName())) {
                                    ParameterDescriptor descriptor = descriptorList.stream()
                                            .filter(d -> d.getName().equals(v.getParameterName()))
                                            .findFirst().orElse(null);
                                    if (descriptor == null) {
                                        errors.add(String.format("[node.customValues.parameterName] invalid parameter name '%s'",
                                                v.getParameterName()));
                                    } else {
                                        ParameterConverter converter = ConverterFactory.getInstance().create(descriptor);
                                        try {
                                            converter.fromString(v.getParameterValue());
                                        } catch (ConversionException e) {
                                            errors.add(String.format("[node.customValues.parameterValue] invalid value for parameter '%s'",
                                                    v.getParameterName()));
                                        }
                                    }
                                }
                            });
                        }
                    }
                    // Validate the compatibilities of the attached component with the declared incoming components
                    if (incomingLinks != null && !incomingLinks.isEmpty()) {
                        List<TaoComponent> linkedComponents = new ArrayList<>();
                        WorkflowNodeDescriptor nodeBefore;
                        for (ComponentLink link : incomingLinks) {
                            String parentId = link.getInput().getParentId();
                            nodeBefore = workflowNodes.stream()
                                    .filter(n -> link.getSourceNodeId() == n.getId())
                                    .findFirst().orElse(null);
                            if (nodeBefore != null) {
                                try {
                                    TaoComponent parentComponent = WorkflowUtilities.findComponent(nodeBefore);
                                    if (parentComponent == null) {
                                        throw new PersistenceException();
                                    }
                                    linkedComponents.add(parentComponent);
                                } catch (PersistenceException e) {
                                    errors.add(String.format("[node.componentId] cannot retrieve component with id = %s",
                                                             parentId));
                                }
                            } else {
                                errors.add(String.format("[node.incomingLinks] source node %s not found",
                                                         link.getSourceNodeId()));
                            }
                        }
                        List<SourceDescriptor> sources = component.getSources();
                        for (TaoComponent linkedComponent : linkedComponents) {
                            List<TargetDescriptor> targets = linkedComponent.getTargets();
                            if (targets.stream()
                                    .noneMatch(t -> sources.stream()
                                            .anyMatch(s -> s.isCompatibleWith(t)))) {
                                errors.add(String.format("[node.incomingLinks] the input format of '%s' is not compatible with the output format of '%s'",
                                                         component.getLabel(), linkedComponent.getLabel()));
                            }
                            if (linkedComponent instanceof ProcessingComponent && component instanceof ProcessingComponent) {
                                if (!ComponentCategory.isCompatibleWith(((ProcessingComponent) component).getCategory(),
                                                                        ((ProcessingComponent) linkedComponent).getCategory())) {
                                    errors.add(String.format("[node.incomingLinks] component '%s' is not compatible with component '%s'",
                                                             component.getLabel(), linkedComponent.getLabel()));
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
