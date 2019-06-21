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
import org.xml.sax.SAXException;
import ro.cs.tao.Tag;
import ro.cs.tao.component.*;
import ro.cs.tao.component.converters.ConverterFactory;
import ro.cs.tao.component.converters.ParameterConverter;
import ro.cs.tao.component.enums.TagType;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.datasource.DataSourceManager;
import ro.cs.tao.datasource.beans.Parameter;
import ro.cs.tao.datasource.beans.Query;
import ro.cs.tao.datasource.converters.ConversionException;
import ro.cs.tao.datasource.param.DataSourceParameter;
import ro.cs.tao.eodata.enums.Visibility;
import ro.cs.tao.execution.model.ExecutionJob;
import ro.cs.tao.orchestration.util.TaskUtilities;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.security.SessionStore;
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
import ro.cs.tao.workflow.ParameterValue;
import ro.cs.tao.workflow.WorkflowDescriptor;
import ro.cs.tao.workflow.WorkflowNodeDescriptor;
import ro.cs.tao.workflow.WorkflowNodeGroupDescriptor;
import ro.cs.tao.workflow.enums.ComponentType;
import ro.cs.tao.workflow.enums.Status;

import java.awt.geom.Rectangle2D;
import java.time.LocalDateTime;
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
    private PersistenceManager persistenceManager;
    @Autowired
    private ComponentService componentService;
    @Autowired
    private GroupComponentService groupComponentService;

    private Logger logger = Logger.getLogger(WorkflowService.class.getName());

    @Override
    public WorkflowDescriptor findById(Long id) throws PersistenceException {
        return persistenceManager.getWorkflowDescriptor(id);
    }

    @Override
    public WorkflowDescriptor getFullDescriptor(Long id) {
        return persistenceManager.getFullWorkflowDescriptor(id);
    }

    @Override
    public List<WorkflowDescriptor> list() {
        return persistenceManager.getAllWorkflows();
    }

    @Override
    public List<WorkflowDescriptor> list(Iterable<Long> ids) {
        return persistenceManager.getWorkflows(ids);
    }

    @Override
    public List<WorkflowInfo> getUserWorkflowsByStatus(String user, Status status) {
        return ServiceTransformUtils.toWorkflowInfos(persistenceManager.getUserWorkflowsByStatus(user, status.value()));
    }

    @Override
    public List<WorkflowInfo> getUserPublishedWorkflowsByVisibility(String user, Visibility visibility) {
        return ServiceTransformUtils.toWorkflowInfos(persistenceManager.getUserPublishedWorkflowsByVisibility(user, visibility.value()));
    }

    @Override
    public List<WorkflowInfo> getOtherPublicWorkflows(String user) {
        return ServiceTransformUtils.toWorkflowInfos(persistenceManager.getOtherPublicWorkflows(user));
    }

    @Override
    public List<WorkflowInfo> getPublicWorkflows() {
        if (!isDevModeEnabled()) {
            return ServiceTransformUtils.toWorkflowInfos(persistenceManager.getPublicWorkflows());
        } else {
            return ServiceTransformUtils.toWorkflowInfos(MockData.getMockWorkflows());
        }
    }

    @Override
    public WorkflowInfo getWorkflowInfo(long workflowId) {
        return ServiceTransformUtils.toWorkflowInfo(persistenceManager.getWorkflowDescriptor(workflowId));
    }

    @Override
    public List<Tag> getWorkflowTags() {
        return persistenceManager.getWorkflowTags();
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
                return persistenceManager.saveWorkflowDescriptor(object);
            } catch (PersistenceException e) {
                logger.severe(e.getMessage());
                return null;
            }
        }
        return null;
    }

    @Override
    public WorkflowDescriptor update(WorkflowDescriptor object) throws PersistenceException {
        WorkflowDescriptor existing = persistenceManager.getWorkflowDescriptor(object.getId());
        existing.setName(object.getName());
        existing.setPath(object.getPath());
        existing.setStatus(object.getStatus());
        existing.setUserName(object.getUserName());
        existing.setVisibility(object.getVisibility());
        existing.setxCoord(object.getxCoord());
        existing.setyCoord(object.getyCoord());
        existing.setZoom(object.getZoom());
        existing.setTags(object.getTags());
        //addTagsIfNew(object);
        return persistenceManager.updateWorkflowDescriptor(existing);
    }

    @Override
    public void delete(Long id) throws PersistenceException {
        WorkflowDescriptor descriptor = findById(id);
        if (descriptor != null) {
            descriptor.setActive(false);
            save(descriptor);
        }
    }

    @Override
    public WorkflowDescriptor tag(Long id, List<String> tags) throws PersistenceException {
        WorkflowDescriptor entity = findById(id);
        if (entity == null) {
            throw new PersistenceException(String.format("Workflow with id '%s' not found", id));
        }
        if (tags != null && tags.size() > 0) {
            Set<String> existingTags = persistenceManager.getWorkflowTags().stream()
                    .map(Tag::getText).collect(Collectors.toSet());
            tags.stream().filter(t -> !existingTags.contains(t)).forEach(t -> persistenceManager.saveTag(new Tag(TagType.WORKFLOW, t)));
            entity.setTags(tags);
            return update(entity);
        }
        return entity;
    }

    @Override
    public WorkflowDescriptor untag(Long id, List<String> tags) throws PersistenceException {
        WorkflowDescriptor entity = findById(id);
        if (entity == null) {
            throw new PersistenceException(String.format("Workflow with id '%s' not found", id));
        }
        if (tags != null && tags.size() > 0) {
            List<String> entityTags = entity.getTags();
            if (entityTags != null) {
                entityTags.removeAll(tags);
                entity.setTags(entityTags);
                return update(entity);
            }
        }
        return entity;
    }

    @Override
    public WorkflowNodeDescriptor addNode(long workflowId, WorkflowNodeDescriptor nodeDescriptor) throws PersistenceException {
        if (nodeDescriptor == null) {
            throw new PersistenceException("Cannot add a null node");
        }
        if (nodeDescriptor.getId() != null && nodeDescriptor.getId() != 0) {
            throw new PersistenceException(String.format("Node [id:%s, name:%s] is not a new node",
                                                         nodeDescriptor.getId(), nodeDescriptor.getName()));
        }
        WorkflowDescriptor workflow = persistenceManager.getWorkflowDescriptor(workflowId);
        if (workflow == null) {
            throw new PersistenceException("Node is not attached to an existing workflow");
        }
        WorkflowUtilities.ensureUniqueName(workflow, nodeDescriptor);
        List<String> validationErrors = new ArrayList<>();
        validateNode(workflow, nodeDescriptor, validationErrors);
        if (validationErrors.size() > 0) {
            throw new PersistenceException(String.format("Node contains errors: [%s]",
                                                         String.join(",", validationErrors)));
        }
        return persistenceManager.saveWorkflowNodeDescriptor(nodeDescriptor, workflow);
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
        WorkflowDescriptor workflow = persistenceManager.getWorkflowDescriptor(workflowId);
        if (workflow == null) {
            throw new PersistenceException("Node is not attached to an existing workflow");
        }
        nodeDescriptor.setWorkflow(workflow);
        WorkflowUtilities.ensureUniqueName(workflow, nodeDescriptor);
        List<String> validationErrors = new ArrayList<>();
        validateNode(workflow, nodeDescriptor, validationErrors);
        if (validationErrors.size() > 0) {
            throw new PersistenceException(String.format("Node contains errors: [%s]",
                                                         String.join(",", validationErrors)));
        }
        return persistenceManager.updateWorkflowNodeDescriptor(nodeDescriptor);
    }

    @Override
    public List<WorkflowNodeDescriptor> updateNodes(long workflowId, List<WorkflowNodeDescriptor> nodeDescriptors) throws PersistenceException {
        if (nodeDescriptors == null || nodeDescriptors.size() == 0) {
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
        if (positions != null && positions.size() > 0) {
            List<WorkflowNodeDescriptor> nodes = persistenceManager.getWorkflowNodesById(positions.keySet().toArray(new Long[0]));
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
                persistenceManager.updateWorkflowNodeDescriptor(node);
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
        WorkflowDescriptor workflow = persistenceManager.getWorkflowDescriptor(workflowId);
        if (workflow == null) {
            throw new PersistenceException("Node is not attached to an existing workflow");
        }
        nodeDescriptor.setWorkflow(workflow);
        // delete any incoming links the node may have
        Set<ComponentLink> links = nodeDescriptor.getIncomingLinks();
        if (links != null && links.size() > 0) {
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
                persistenceManager.updateWorkflowNodeDescriptor(child);
            }
        }
        persistenceManager.updateWorkflowNodeDescriptor(nodeDescriptor);

        if (ComponentType.DATASOURCE.equals(nodeDescriptor.getComponentType())) {
            DataSourceComponent component = (DataSourceComponent) componentService.findComponent(nodeDescriptor.getComponentId(),
                                                                                                 nodeDescriptor.getComponentType());
            Query query = persistenceManager.getQuery(SessionStore.currentContext().getPrincipal().getName(),
                                                      component.getSensorName(), component.getDataSourceName(),
                                                      nodeDescriptor.getId());
            if (query != null) {
                persistenceManager.removeQuery(query);
            }
        }

        workflow.removeNode(nodeDescriptor);
        persistenceManager.updateWorkflowDescriptor(workflow);
    }

    @Override
    public WorkflowNodeDescriptor addLink(long sourceNodeId, String sourceTargetId,
                                      long targetNodeId, String targetSourceId) throws PersistenceException {
        if (sourceTargetId == null || targetSourceId == null || sourceNodeId == 0 || targetNodeId == 0) {
            throw new PersistenceException("Invalid link data");
        }
        WorkflowNodeDescriptor sourceNode = persistenceManager.getWorkflowNodeById(sourceNodeId);
        WorkflowNodeDescriptor targetNode = persistenceManager.getWorkflowNodeById(targetNodeId);
        if (targetNode == null) {
            throw new PersistenceException("Target node does not exist");
        }
        TaoComponent sourceComponent = componentService.findComponent(sourceNode.getComponentId(), sourceNode.getComponentType());
        if (sourceComponent == null) {
            throw new PersistenceException("Source component not found");
        }
        TaoComponent targetComponent = componentService.findComponent(targetNode.getComponentId(), targetNode.getComponentType());
        if (targetComponent == null) {
            throw new PersistenceException("Target component not found");
        }
        /*TargetDescriptor linkInput = sourceComponent.getTargets().stream()
                                                    .filter(t -> t.getId().equals(sourceTargetId))
                                                    .findFirst().orElse(null);
        SourceDescriptor linkOutput = targetComponent.getSources().stream()
                                                    .filter(s -> s.getId().equals(targetSourceId))
                                                    .findFirst().orElse(null);*/
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
        WorkflowNodeGroupDescriptor sourceGroupNode = persistenceManager.getGroupNode(sourceNodeId);
        WorkflowNodeGroupDescriptor targetGroupNode = persistenceManager.getGroupNode(targetNodeId);
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
        WorkflowNodeDescriptor node = persistenceManager.getWorkflowNodeById(nodeId);
        if (node == null) {
            throw new PersistenceException("Parent node does not exist");
        }
        node.getIncomingLinks().removeIf(l -> l.equals(link));
        return persistenceManager.updateWorkflowNodeDescriptor(node);
    }

    @Override
    public WorkflowNodeDescriptor addGroup(long workflowId, long nodeBeforeId, String groupName, Long[] nodeIds) throws PersistenceException {
        if (nodeIds == null || nodeIds.length == 0) {
            throw new PersistenceException("Empty node group");
        }
        List<WorkflowNodeDescriptor> nodes = persistenceManager.getWorkflowNodesById(nodeIds);
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
        return addGroup(workflowId, grpNode, nodeBeforeId, nodes);
    }

    @Override
    public WorkflowNodeDescriptor addGroup(long workflowId, WorkflowNodeGroupDescriptor groupDescriptor,
                                           long nodeBeforeId,
                                           WorkflowNodeDescriptor[] nodes) throws PersistenceException {
        return addGroup(workflowId, groupDescriptor, nodeBeforeId, Arrays.asList(nodes));
    }

    @Override
    public WorkflowNodeDescriptor addGroup(long workflowId, WorkflowNodeGroupDescriptor groupDescriptor,
                                           long nodeBeforeId,
                                           List<WorkflowNodeDescriptor> nodes) throws PersistenceException {
        if (nodes == null || nodes.size() == 0) {
            throw new PersistenceException("Empty node group");
        }

        ProcessingComponent firstComponent = componentService.findById(nodes.get(0).getComponentId());
        ProcessingComponent lastComponent = componentService.findById(nodes.get(nodes.size() - 1).getComponentId());

        WorkflowNodeDescriptor nodeBefore = persistenceManager.getWorkflowNodeById(nodeBeforeId);
        TaoComponent component = null;
        if (nodeBefore != null) {
            component = componentService.findComponent(nodeBefore.getComponentId(), nodeBefore.getComponentType());
        }
        int cardinality = component != null ? TaskUtilities.getSourceCardinality(component) : 1;
        GroupComponent groupComponent = GroupComponent.create(firstComponent.getSources(), lastComponent.getTargets());
        groupComponent = groupComponentService.save(groupComponent);
        groupDescriptor.setComponentId(groupComponent.getId());
        groupDescriptor.setComponentType(ComponentType.GROUP);
        WorkflowDescriptor workflow = persistenceManager.getWorkflowDescriptor(workflowId);

        for (WorkflowNodeDescriptor node : nodes) {
            groupDescriptor.addNode(node);
        }
        groupDescriptor = (WorkflowNodeGroupDescriptor) persistenceManager.saveWorkflowNodeDescriptor(groupDescriptor, workflow);

        if (component != null) {
            Set<ComponentLink> external = new HashSet<>();
            TargetDescriptor target = component.getTargets().get(0);
            SourceDescriptor source = groupComponent.getSources().get(0);
            external.add(new ComponentLink(nodeBefore.getId(), target, source));
            groupDescriptor.setIncomingLinks(external);
            groupDescriptor = (WorkflowNodeGroupDescriptor) persistenceManager.updateWorkflowNodeDescriptor(groupDescriptor);
            final Map<Long, ComponentLink> linksToRemove = new HashMap<>();
            groupDescriptor.getNodes().forEach(c -> {
                ComponentLink link;
                if (c.getIncomingLinks() != null &&
                        (link = c.getIncomingLinks().stream()
                                .filter(l -> l.getSourceNodeId() == nodeBeforeId)
                                .findFirst().orElse(null)) != null) {
                    linksToRemove.put(c.getId(), link);
                }
            });
            for (Map.Entry<Long, ComponentLink> entry : linksToRemove.entrySet()) {
                removeLink(entry.getKey(), entry.getValue());
            }
        }
        return groupDescriptor;
    }

    @Override
    public WorkflowNodeDescriptor addGroup(long workflowId, Long[] nodesBeforeIds, String groupName, Long[] nodeIds) throws PersistenceException {
        if (nodeIds == null || nodeIds.length == 0) {
            throw new PersistenceException("Empty node group");
        }
        List<WorkflowNodeDescriptor> nodes = persistenceManager.getWorkflowNodesById(nodeIds);
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
        return addGroup(workflowId, grpNode, nodesBeforeIds, nodes);
    }

    @Override
    public WorkflowNodeDescriptor addGroup(long workflowId, WorkflowNodeGroupDescriptor groupDescriptor,
                                           Long[] nodesBeforeIds, List<WorkflowNodeDescriptor> nodes) throws PersistenceException {
        if (nodes == null || nodes.size() == 0) {
            throw new PersistenceException("Empty node group");
        }
        final List<ProcessingComponent> firstComponents = new ArrayList<>();
        final Map<Long, ProcessingComponent> lastComponents = new LinkedHashMap<>();
        Map<Long, TaoComponent> componentsBefore = new LinkedHashMap<>();
        final List<WorkflowNodeDescriptor> terminals;
        // Step 1: find first nodes and terminal nodes
        if (nodesBeforeIds != null && nodesBeforeIds.length > 0) {
            final List<WorkflowNodeDescriptor> referrers = WorkflowUtilities.findReferrers(new LinkedHashSet<>(Arrays.asList(nodesBeforeIds)), nodes);
            terminals = WorkflowUtilities.findTerminals(nodes);
            for (WorkflowNodeDescriptor node : referrers) {
                firstComponents.add(componentService.findById(node.getComponentId()));
            }
            for (WorkflowNodeDescriptor node : terminals) {
                lastComponents.put(node.getId(), componentService.findById(node.getComponentId()));
            }
            List<WorkflowNodeDescriptor> nodesBefore = persistenceManager.getWorkflowNodesById(nodesBeforeIds);
            nodesBefore.forEach(n -> componentsBefore.put(n.getId(),
                                                          componentService.findComponent(n.getComponentId(),
                                                                                         n.getComponentType())));
        } else {
            firstComponents.add(componentService.findById(nodes.get(0).getComponentId()));
            WorkflowNodeDescriptor lastNode = nodes.get(nodes.size() - 1);
            lastComponents.put(lastNode.getId(), componentService.findById(lastNode.getComponentId()));
            terminals = new ArrayList<>();
        }
        final List<SourceDescriptor> sources = new ArrayList<>();
        firstComponents.forEach(c -> sources.addAll(c.getSources()));
        final List<TargetDescriptor> targets = new ArrayList<>();
        lastComponents.values().forEach(c -> targets.addAll(c.getTargets()));

        // Step 2: create the group component with the found sources and targets
        GroupComponent groupComponent = GroupComponent.create(sources, targets);
        groupComponent = groupComponentService.save(groupComponent);
        groupDescriptor.setComponentId(groupComponent.getId());
        groupDescriptor.setComponentType(ComponentType.GROUP);

        // Step 3: add the nodes to the group
        WorkflowDescriptor workflow = persistenceManager.getWorkflowDescriptor(workflowId);
        for (WorkflowNodeDescriptor node : nodes) {
            groupDescriptor.addNode(node);
        }
        groupDescriptor = (WorkflowNodeGroupDescriptor) persistenceManager.saveWorkflowNodeDescriptor(groupDescriptor, workflow);
        Set<ComponentLink> external = new HashSet<>();
        final Map<Long, ComponentLink> linksToRemove = new HashMap<>();

        // Step 4: re-assign the links of the first components
        for (Map.Entry<Long, TaoComponent> entry : componentsBefore.entrySet()) {
            final Long nodeBeforeId = entry.getKey();
            TargetDescriptor target = entry.getValue().getTargets().get(0);
            SourceDescriptor source = groupComponent.getSources().get(0);
            external.add(new ComponentLink(nodeBeforeId, target, source));
            groupDescriptor.setIncomingLinks(external);
            groupDescriptor = (WorkflowNodeGroupDescriptor) persistenceManager.updateWorkflowNodeDescriptor(groupDescriptor);
            groupDescriptor.getNodes().forEach(c -> {
                ComponentLink link;
                if (c.getIncomingLinks() != null &&
                        (link = c.getIncomingLinks().stream()
                                .filter(l -> l.getSourceNodeId() == nodeBeforeId)
                                .findFirst().orElse(null)) != null) {
                    linksToRemove.put(c.getId(), link);
                }
            });
            for (Map.Entry<Long, ComponentLink> e : linksToRemove.entrySet()) {
                removeLink(e.getKey(), e.getValue());
            }
        }
        linksToRemove.clear();
        // Step 5: re-assign the links of the terminal nodes
        List<WorkflowNodeDescriptor> terminalReferrers = WorkflowUtilities.findReferrers(terminals.stream()
                                                                                .map(WorkflowNodeDescriptor::getId)
                                                                                .collect(Collectors.toSet()),
                                                                       workflow.getNodes());
        final long groupId = groupDescriptor.getId();
        for (Map.Entry<Long, ProcessingComponent> entry : lastComponents.entrySet()) {
            final Long sourceNodeId = entry.getKey();
            for (WorkflowNodeDescriptor node : terminalReferrers) {
                Set<ComponentLink> links = node.getIncomingLinks();
                for (ComponentLink link : links) {
                    if (sourceNodeId.equals(link.getSourceNodeId())) {
                        node.addLink(new ComponentLink(groupId, link.getInput(), link.getOutput()));
                        node.removeLink(link);
                    }
                }
                persistenceManager.updateWorkflowNodeDescriptor(node);
            }
        }

        return groupDescriptor;
    }

    @Override
    public WorkflowNodeDescriptor updateGroup(WorkflowNodeGroupDescriptor groupDescriptor) throws PersistenceException {
        return persistenceManager.updateWorkflowNodeDescriptor(groupDescriptor);
    }

    @Override
    public void removeGroup(WorkflowNodeGroupDescriptor groupDescriptor, boolean removeChildren) throws PersistenceException {
        /*
        TODO:
        1. If removeChildren = true, delete all the child nodes
        2. Else: get first child and rebuild the links of the group to have the child as target
                 get last child, the target node that may be linked to the group, and rebuild the links of the group
                    having the last child as originator
         */
    }

    @Override
    public WorkflowDescriptor clone(WorkflowDescriptor workflow) throws PersistenceException {
        if (workflow == null) {
            return null;
        }
        WorkflowDescriptor clone = new WorkflowDescriptor();
        clone.setName(workflow.getName() + " - " + String.valueOf(System.currentTimeMillis()));
        clone.setStatus(Status.DRAFT);
        clone.setCreated(LocalDateTime.now());
        clone.setActive(workflow.isActive());
        clone.setUserName(SessionStore.currentContext().getPrincipal().getName());
        clone.setVisibility(Visibility.PRIVATE);
        clone = persistenceManager.saveWorkflowDescriptor(clone);
        // key: old id, value: new node
        final Map<Long, WorkflowNodeDescriptor> cloneMap = new HashMap<>();
        final List<WorkflowNodeDescriptor> workflowNodeDescriptors = workflow.orderNodes(workflow.getNodes());
        for (WorkflowNodeDescriptor node : workflowNodeDescriptors) {
            WorkflowNodeDescriptor clonedNode = node instanceof WorkflowNodeGroupDescriptor ?
                    new WorkflowNodeGroupDescriptor() : new WorkflowNodeDescriptor();
            clonedNode.setWorkflow(clone);
            clonedNode.setName(node.getName());
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
                persistenceManager.updateWorkflowNodeDescriptor(clonedNode);
            }
        }
        for (WorkflowNodeDescriptor node : workflowNodeDescriptors) {
            final Set<ComponentLink> links = node.getIncomingLinks();
            if (links != null) {
                WorkflowNodeDescriptor clonedTarget = cloneMap.get(node.getId());
                for (ComponentLink link : links) {
                    WorkflowNodeDescriptor clonedSource = cloneMap.get(link.getSourceNodeId());
                    TargetDescriptor input = findTarget(link.getInput().getId(), clonedSource);
                    SourceDescriptor output = findSource(link.getOutput().getId(), clonedTarget);
                    ComponentLink clonedLink = new ComponentLink(clonedSource.getId(), input, output);
                    Set<ComponentLink> clonedLinks = clonedTarget.getIncomingLinks();
                    if (clonedLinks == null) {
                        clonedLinks = new HashSet<>();
                    }
                    clonedLinks.add(clonedLink);
                    clonedTarget.setIncomingLinks(clonedLinks);
                }
                clonedTarget = persistenceManager.updateWorkflowNodeDescriptor(clonedTarget);
            }
        }
        return persistenceManager.getWorkflowDescriptor(clone.getId());
    }

    @Override
    public WorkflowDescriptor importWorkflowNodes(WorkflowDescriptor master,
                                                  WorkflowDescriptor subWorkflow,
                                                  boolean keepDataSources) throws PersistenceException {
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
                persistenceManager.updateWorkflowNodeDescriptor(clonedNode);
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
                        TargetDescriptor input = findTarget(link.getInput().getId(), clonedSource);
                        SourceDescriptor output = findSource(link.getOutput().getId(), clonedTarget);
                        ComponentLink clonedLink = new ComponentLink(clonedSource.getId(), input, output);
                        Set<ComponentLink> clonedLinks = clonedTarget.getIncomingLinks();
                        if (clonedLinks == null) {
                            clonedLinks = new HashSet<>();
                        }
                        clonedLinks.add(clonedLink);
                        clonedTarget.setIncomingLinks(clonedLinks);
                    }
                }
                persistenceManager.updateWorkflowNodeDescriptor(clonedTarget);
            }
        }
        // finally, create a group to hold the cloned sub-workflow
        addGroup(master.getId(), 0, subWorkflow.getName(),
                 cloneMap.values().stream().map(WorkflowNodeDescriptor::getId).toArray(Long[]::new));
        return persistenceManager.getWorkflowDescriptor(master.getId());
    }

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
        final WorkflowDescriptor workflow = persistenceManager.getWorkflowDescriptor(workflowId);
        if (workflow == null) {
            throw new PersistenceException("There is no workflow having the given identifier " + String.valueOf(workflowId));
        }
        return ServiceTransformUtils.toJobInfos(persistenceManager.getJobs(workflowId));
    }

    @Override
    public List<ExecutionTaskInfo> getWorkflowExecutionTasks(long executionJobId) throws PersistenceException {
        final ExecutionJob workflowExecution = persistenceManager.getJobById(executionJobId);
        if (workflowExecution == null) {
            throw new PersistenceException("There is no workflow execution having the given identifier " + String.valueOf(executionJobId));
        }
        return ServiceTransformUtils.toTaskInfos(workflowExecution.orderedTasks());
    }

    @Override
    public Map<String, List<Parameter>> getWorkflowParameters(long workflowId) {
        Map<String, List<Parameter>> parameters = new LinkedHashMap<>();
        WorkflowDescriptor workflow = persistenceManager.getWorkflowDescriptor(workflowId);
        if (workflow == null) {
            throw new IllegalArgumentException(String.format("Non-existent workflow with id '%s'", workflowId));
        }
        final List<WorkflowNodeDescriptor> nodes = workflow.getOrderedNodes();
        final long distinct = nodes.stream().distinct().count();
        final boolean prefixWithId = nodes.size() != distinct;
        /*final int size = nodes.size();
        for (int i = 0; i < size - 1; i++) {
            for (int j = i + 1; j < size; j++) {
                if (nodes.get(i).getName().equals(nodes.get(j).getName())) {
                    prefixWithId = true;
                    break;
                }
            }
        }*/
        for (WorkflowNodeDescriptor node : nodes) {
            if (node instanceof WorkflowNodeGroupDescriptor) {
                continue;
            }
            ComponentType componentType = node.getComponentType();
            TaoComponent component = componentService.findComponent(node.getComponentId(), componentType);
            List<Parameter> componentParams = new ArrayList<>();
            Class paramType;
            switch (componentType) {
                case DATASOURCE:
                    DataSourceComponent dataSourceComponent = (DataSourceComponent) component;
                    Map<String, DataSourceParameter> params =
                            DataSourceManager.getInstance().getSupportedParameters(dataSourceComponent.getSensorName(),
                                                                                   dataSourceComponent.getDataSourceName());
                    for (Map.Entry<String, DataSourceParameter> entry : params.entrySet()) {
                        DataSourceParameter descriptor = entry.getValue();
                        paramType = descriptor.getType();
                        componentParams.add(new Parameter(entry.getKey(),
                                                          paramType.getName(),
                                                          descriptor.getDefaultValue() != null ? String.valueOf(descriptor.getDefaultValue()) : null,
                                                          paramType.isEnum() ?
                                                                  Parameter.stringValueSet(paramType.getEnumConstants()) :
                                                                  Parameter.stringValueSet(descriptor.getValueSet())));
                    }
                    Query query = persistenceManager.getQuery(SessionStore.currentContext().getPrincipal().getName(),
                                                              dataSourceComponent.getSensorName(),
                                                              dataSourceComponent.getDataSourceName(),
                                                              node.getId());
                    if (query != null) {
                        for (Map.Entry<String, String> e : query.getValues().entrySet()) {
                            componentParams.stream()
                                    .filter(cp -> cp.getName().equals(e.getKey()))
                                    .forEach(cp -> cp.setValue(e.getValue()));
                        }
                        componentParams.add(new Parameter("pageSize", Integer.class.getName(), String.valueOf(query.getPageSize())));
                        componentParams.add(new Parameter("pageNumber", Integer.class.getName(), String.valueOf(query.getPageNumber())));
                        componentParams.add(new Parameter("limit", Integer.class.getName(), String.valueOf(query.getLimit())));
                    } else {
                        componentParams.add(new Parameter("pageSize", Integer.class.getName(), "25"));
                        componentParams.add(new Parameter("pageNumber", Integer.class.getName(), "1"));
                        componentParams.add(new Parameter("limit", Integer.class.getName(), "25"));
                    }
                    break;
                case PROCESSING:
                    ProcessingComponent processingComponent = (ProcessingComponent) component;
                    List<ParameterDescriptor> descriptors = processingComponent.getParameterDescriptors();
                    for (ParameterDescriptor descriptor : descriptors) {
                        paramType = descriptor.getDataType();
                        componentParams.add(new Parameter(descriptor.getName(),
                                                          paramType.getName(),
                                                          !"null".equals(descriptor.getDefaultValue()) ? descriptor.getDefaultValue() : null,
                                                          paramType.isEnum() ?
                                                                  Parameter.stringValueSet(paramType.getEnumConstants()) :
                                                                  Parameter.stringValueSet(descriptor.getValueSet())));
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
    public List<TargetDescriptor> getWorkflowOutputs(long workflowId) {
        WorkflowDescriptor workflow = persistenceManager.getWorkflowDescriptor(workflowId);
        if (workflow == null) {
            throw new IllegalArgumentException(String.format("Non-existent workflow with id '%s'", workflowId));
        }
        final List<TargetDescriptor> targetDescriptors = new ArrayList<>();
        final List<WorkflowNodeDescriptor> nodes = workflow.findLeaves(workflow.getOrderedNodes());
        if (nodes != null) {
            for (WorkflowNodeDescriptor node : nodes) {
                TaoComponent component = componentService.findComponent(node.getComponentId(), node.getComponentType());
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
            return GraphParser.parse(persistenceManager, this, graphXml);
        } catch (Exception e) {
            throw new SAXException(e);
        }
    }

    @Override
    public String workflowToSnapGraph(WorkflowDescriptor workflowDescriptor) throws Exception {
        return null;
    }

    /**
     * Returns the TargetDescriptor entity of a workflow node by its id.
     * @param id                The descriptor identifier
     * @param nodeDescriptor    The workflow node
     */
    private TargetDescriptor findTarget(String id, WorkflowNodeDescriptor nodeDescriptor) throws PersistenceException {
        TaoComponent component = componentService.findComponent(nodeDescriptor.getComponentId(), nodeDescriptor.getComponentType());
        return component != null ? component.findDescriptor(id) : null;
                //component.getTargets().stream().filter(t -> t.getId().equals(id)).findFirst().orElse(null) : null;
    }
    /**
     * Returns the SourceDescriptor entity of a workflow node by its id.
     * @param id                The descriptor identifier
     * @param nodeDescriptor    The workflow node
     */
    private SourceDescriptor findSource(String id, WorkflowNodeDescriptor nodeDescriptor) throws PersistenceException {
        TaoComponent component = componentService.findComponent(nodeDescriptor.getComponentId(), nodeDescriptor.getComponentType());
        return component != null ? component.findDescriptor(id) : null;
                //component.getSources().stream().filter(s -> s.getId().equals(id)).findFirst().orElse(null) : null;
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
        WorkflowNodeDescriptor outsideNode = persistenceManager.getWorkflowNodeById(sourceNodeId);
        TaoComponent component = componentService.findComponent(outsideNode.getComponentId(),
                                                                outsideNode.getComponentType());
        if (!component.hasDescriptor(linkInput.getId())) {
        //if (component.getTargets().stream().noneMatch(t -> t.getId().equals(linkInput.getId()))) {
            throw new IllegalArgumentException(String.format("Target descriptor %s not found in node %d",
                                                             linkInput.getId(), sourceNodeId));
        }
        component = componentService.findComponent(targetNode.getComponentId(), targetNode.getComponentType());
        if (!component.hasDescriptor(linkOutput.getId())) {
        //if (component.getSources().stream().noneMatch(t -> t.getId().equals(linkOutput.getId()))) {
            throw new IllegalArgumentException(String.format("Source descriptor %s not found in node %d",
                                                             linkOutput.getId(), targetNode.getId()));
        }
        component = componentService.findComponent(groupNode.getComponentId(), groupNode.getComponentType());
        if (!component.hasDescriptor(linkOutput.getId())) {
        /*List<SourceDescriptor> sources = component.getSources();
        if (sources == null || sources.stream().noneMatch(s -> s.getId().equals(linkOutput.getId()))) {*/
            component.addSource(linkOutput);
        }
        groupComponentService.save((GroupComponent) component);
        groupNode.addLink(new ComponentLink(sourceNodeId, linkInput, linkOutput));
        return persistenceManager.updateWorkflowNodeDescriptor(groupNode);
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
        WorkflowNodeDescriptor outsideNode = persistenceManager.getWorkflowNodeById(targetNodeId);
        TaoComponent component = componentService.findComponent(outsideNode.getComponentId(),
                                                                outsideNode.getComponentType());
        if (!component.hasDescriptor(linkOutput.getId())) {
        //if (component.getSources().stream().noneMatch(s -> s.getId().equals(linkOutput.getId()))) {
            throw new IllegalArgumentException(String.format("Source descriptor %s not found in node %d",
                                                             linkOutput.getId(), targetNodeId));
        }
        component = componentService.findComponent(sourceNode.getComponentId(), sourceNode.getComponentType());
        if (!component.hasDescriptor(linkInput.getId())) {
        //if (component.getTargets().stream().noneMatch(t -> t.getId().equals(linkInput.getId()))) {
            throw new IllegalArgumentException(String.format("Target descriptor %s not found in node %d",
                                                             linkInput.getId(), sourceNode.getId()));
        }
        component = componentService.findComponent(groupNode.getComponentId(), groupNode.getComponentType());
        if (!component.hasDescriptor(linkInput.getId())) {
        /*List<TargetDescriptor> targets = component.getTargets();
        if (targets == null || targets.stream().noneMatch(t -> t.getId().equals(linkInput.getId()))) {*/
            component.addTarget(linkInput);
        }
        groupComponentService.save((GroupComponent) component);
        outsideNode.addLink(new ComponentLink(targetNodeId, linkInput, linkOutput));
        return persistenceManager.updateWorkflowNodeDescriptor(outsideNode);
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
        if (incomingLinks != null && incomingLinks.size() > 0) {
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
                TaoComponent component = componentService.findComponent(value, node.getComponentType());
                if (component == null) {
                    errors.add("[node.componentId] component does not exist");
                } else {
                    List<ParameterValue> customValues = node.getCustomValues();
                    // Validate custom parameter values for the attached component
                    if (customValues != null && customValues.size() > 0) {
                        List<ParameterDescriptor> descriptors = null;
                        if (component instanceof ProcessingComponent) {
                            descriptors = ((ProcessingComponent) component).getParameterDescriptors();
                        }
                        if (descriptors != null && descriptors.size() > 0) {
                            final List<ParameterDescriptor> descriptorList = new ArrayList<>(descriptors);
                            component.getTargets().forEach(t -> {
                                descriptorList.addAll(t.toParameter());
                            });
                            customValues.forEach(v -> {
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
                            });
                        }
                    }
                    // Validate the compatibilities of the attached component with the declared incoming components
                    if (incomingLinks != null && incomingLinks.size() > 0) {
                        List<TaoComponent> linkedComponents = new ArrayList<>();
                        WorkflowNodeDescriptor nodeBefore;
                        for (ComponentLink link : incomingLinks) {
                            String parentId = link.getInput().getParentId();
                            nodeBefore = workflowNodes.stream()
                                    .filter(n -> link.getSourceNodeId() == n.getId())
                                    .findFirst().orElse(null);
                            if (nodeBefore != null) {
                                try {
                                    TaoComponent parentComponent = componentService.findComponent(parentId, nodeBefore.getComponentType());
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
                                errors.add(String.format("[node.incomingLinks] component %s is not compatible with component %s",
                                                         component.getId(), linkedComponent.getId()));
                            }
                        }
                    }
                }
            }
        }
    }
}
