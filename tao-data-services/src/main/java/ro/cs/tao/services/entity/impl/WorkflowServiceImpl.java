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
package ro.cs.tao.services.entity.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.component.*;
import ro.cs.tao.component.converters.ConverterFactory;
import ro.cs.tao.component.converters.ParameterConverter;
import ro.cs.tao.datasource.converters.ConversionException;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.interfaces.ComponentService;
import ro.cs.tao.services.interfaces.GroupComponentService;
import ro.cs.tao.services.interfaces.WorkflowService;
import ro.cs.tao.workflow.*;
import ro.cs.tao.workflow.enums.Status;
import ro.cs.tao.workflow.enums.Visibility;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
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
    public WorkflowDescriptor findById(String id) throws PersistenceException {
        return persistenceManager.getWorkflowDescriptor(Long.parseLong(id));
    }

    @Override
    public List<WorkflowDescriptor> list() {
        return persistenceManager.getAllWorkflows();
    }

    @Override
    public List<WorkflowDescriptor> getUserWorkflowsByStatus(String user, Status status) {
        return persistenceManager.getUserWorkflowsByStatus(user, status.value());
    }

    @Override
    public List<WorkflowDescriptor> getUserPublishedWorkflowsByVisibility(String user, Visibility visibility) {
        return persistenceManager.getUserPublishedWorkflowsByVisibility(user, visibility.value());
    }

    @Override
    public List<WorkflowDescriptor> getOtherPublicWorkflows(String user) {
        return persistenceManager.getOtherPublicWorkflows(user);
    }

    @Override
    public WorkflowDescriptor save(WorkflowDescriptor object) {
        if (object != null) {
            List<WorkflowNodeDescriptor> nodes = object.getNodes();
            if (nodes != null) {
                nodes.forEach(node -> node.setWorkflow(object));
            }
            validate(object);
            try {
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
        return persistenceManager.updateWorkflowDescriptor(existing);
    }

    @Override
    public void delete(String id) throws PersistenceException {
        WorkflowDescriptor descriptor = findById(id);
        if (descriptor != null) {
            descriptor.setActive(false);
            save(descriptor);
        }
    }

    @Override
    public WorkflowNodeDescriptor addNode(long workflowId, WorkflowNodeDescriptor nodeDescriptor) throws PersistenceException {
        if (nodeDescriptor == null) {
            throw new PersistenceException("Cannot add a null node");
        }
        if (nodeDescriptor.getId() != null) {
            throw new PersistenceException(String.format("Node [id:%s, name:%s] is not a new node",
                                                         nodeDescriptor.getId(), nodeDescriptor.getName()));
        }
        WorkflowDescriptor workflow = persistenceManager.getWorkflowDescriptor(workflowId);
        if (workflow == null) {
            throw new PersistenceException("Node is not attached to an existing workflow");
        }
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
        List<String> validationErrors = new ArrayList<>();
        validateNode(workflow, nodeDescriptor, validationErrors);
        if (validationErrors.size() > 0) {
            throw new PersistenceException(String.format("Node contains errors: [%s]",
                                                         String.join(",", validationErrors)));
        }
        return persistenceManager.updateWorkflowNodeDescriptor(nodeDescriptor);
        //return persistenceManager.getWorkflowDescriptor(workflow.getId());
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
        List<ComponentLink> links = nodeDescriptor.getIncomingLinks();
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
                List<ComponentLink> incomingLinks = child.getIncomingLinks();
                incomingLinks = incomingLinks.stream()
                                             .filter(l -> l.getSourceNodeId() == nodeDescriptor.getId())
                                             .collect(Collectors.toList());
                for (ComponentLink link : incomingLinks) {
                    child.removeLink(link);
                }
                persistenceManager.updateWorkflowNodeDescriptor(child);
            }
        }

        persistenceManager.updateWorkflowNodeDescriptor(nodeDescriptor);
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
        TaoComponent sourceComponent = findComponent(sourceNode.getComponentId(), sourceNode.getComponentType());
        if (sourceComponent == null) {
            throw new PersistenceException("Source component not found");
        }
        TaoComponent targetComponent = findComponent(targetNode.getComponentId(), targetNode.getComponentType());
        if (targetComponent == null) {
            throw new PersistenceException("Target component not found");
        }
        TargetDescriptor linkInput = sourceComponent.getTargets().stream()
                                                    .filter(t -> t.getId().equals(sourceTargetId))
                                                    .findFirst().get();
        SourceDescriptor linkOutput = targetComponent.getSources().stream()
                                                    .filter(s -> s.getId().equals(targetSourceId))
                                                    .findFirst().get();
        try {
            ComponentLink link = new ComponentLink(sourceNodeId, linkInput, linkOutput);
            List<ComponentLink> links = targetNode.getIncomingLinks();
            if (links == null) {
                links = new ArrayList<>();
            }
            links.add(link);
            targetNode.setIncomingLinks(links);
            return persistenceManager.updateWorkflowNodeDescriptor(targetNode);
        } catch (Exception e) {
            throw new PersistenceException(e);
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
    public WorkflowNodeDescriptor addGroup(long workflowId, WorkflowNodeGroupDescriptor groupDescriptor,
                                       long nodeBeforeId,
                                       WorkflowNodeDescriptor[] nodes) throws PersistenceException {
        if (nodes == null || nodes.length == 0) {
            throw new PersistenceException("Empty node group");
        }
        ProcessingComponent firstComponent = componentService.findById(nodes[0].getComponentId());
        ProcessingComponent lastComponent = componentService.findById(nodes[nodes.length - 1].getComponentId());

        WorkflowNodeDescriptor nodeBefore = persistenceManager.getWorkflowNodeById(nodeBeforeId);
        TaoComponent component = null;
        if (nodeBefore != null) {
            component = findComponent(nodeBefore.getComponentId(), nodeBefore.getComponentType());
        }
        int cardinality = component != null ? component.getTargetCardinality() : 1;
        GroupComponent groupComponent = GroupComponent.create(firstComponent.getSources(), cardinality,
                                                              lastComponent.getTargets(), cardinality);
        groupComponent = groupComponentService.save(groupComponent);
        groupDescriptor.setComponentId(groupComponent.getId());
        groupDescriptor.setComponentType(ComponentType.GROUP);
        WorkflowDescriptor workflow = persistenceManager.getWorkflowDescriptor(workflowId);

        for (WorkflowNodeDescriptor node : nodes) {
            groupDescriptor.addNode(node);
        }
        groupDescriptor = (WorkflowNodeGroupDescriptor) persistenceManager.saveWorkflowNodeDescriptor(groupDescriptor, workflow);

        if (component != null) {
            List<ComponentLink> external = new ArrayList<>();
            TargetDescriptor target = component.getTargets().get(0);
            SourceDescriptor source = groupComponent.getSources().get(0);
            external.add(new ComponentLink(nodeBefore.getId(), target, source));
            groupDescriptor.setIncomingLinks(external);
            groupDescriptor = (WorkflowNodeGroupDescriptor) persistenceManager.updateWorkflowNodeDescriptor(groupDescriptor);
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
        clone.setName(workflow.getName());
        clone.setStatus(Status.DRAFT);
        clone.setCreated(LocalDateTime.now());
        clone.setActive(workflow.isActive());
        clone.setUserName(SessionStore.currentContext().getPrincipal().getName());
        clone.setVisibility(Visibility.PRIVATE);
        clone = persistenceManager.saveWorkflowDescriptor(clone);
        // key: old id, value: new node
        Map<Long, WorkflowNodeDescriptor> cloneMap = new HashMap<>();
        List<WorkflowNodeDescriptor> workflowNodeDescriptors = workflow.orderNodes(workflow.getNodes());
        for (WorkflowNodeDescriptor node : workflowNodeDescriptors) {
            WorkflowNodeDescriptor clonedNode = new WorkflowNodeDescriptor();
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
            List<ComponentLink> links = node.getIncomingLinks();
            if (links != null) {
                for (ComponentLink link : links) {
                    WorkflowNodeDescriptor clonedSource = cloneMap.get(link.getSourceNodeId());
                    TargetDescriptor input = findTarget(link.getInput().getId(), node);
                    SourceDescriptor output = findSource(link.getOutput().getId(), clonedNode);
                    ComponentLink clonedLink = new ComponentLink(clonedSource.getId(), input, output);
                    List<ComponentLink> clonedLinks = clonedNode.getIncomingLinks();
                    if (clonedLinks == null) {
                        clonedLinks = new ArrayList<>();
                    }
                    clonedLinks.add(clonedLink);
                    clonedNode.setIncomingLinks(links);
                }
                clonedNode = persistenceManager.updateWorkflowNodeDescriptor(clonedNode);
            }
        }
        return persistenceManager.getWorkflowDescriptor(clone.getId());
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

    private TaoComponent findComponent(String id, ComponentType type) {
        TaoComponent component = null;
        switch (type) {
            case DATASOURCE:
                component = persistenceManager.getDataSourceInstance(id);
                break;
            case PROCESSING:
                component = persistenceManager.getProcessingComponentById(id);
                break;
            case GROUP:
                component = persistenceManager.getGroupComponentById(id);
                break;
        }
        return component;
    }

    private TargetDescriptor findTarget(String id, WorkflowNodeDescriptor nodeDescriptor) throws PersistenceException {
        ProcessingComponent component = componentService.findById(nodeDescriptor.getComponentId());
        return component != null ?
                component.getTargets().stream().filter(t -> t.getId().equals(id)).findFirst().orElse(null) : null;
    }

    private SourceDescriptor findSource(String id, WorkflowNodeDescriptor nodeDescriptor) throws PersistenceException {
        ProcessingComponent component = componentService.findById(nodeDescriptor.getComponentId());
        return component != null ?
                component.getSources().stream().filter(s -> s.getId().equals(id)).findFirst().orElse(null) : null;
    }

    private void validateNode(WorkflowDescriptor workflow, WorkflowNodeDescriptor node, List<String> errors) {
        // Validate simple fields
        String value = node.getName();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[node.name] cannot be empty");
        }
        List<ComponentLink> incomingLinks = node.getIncomingLinks();
        if (incomingLinks != null && incomingLinks.size() > 0) {
            if (incomingLinks.stream()
                    .noneMatch(l -> l.getOutput().getParentId().equals(node.getComponentId()))) {
                errors.add(String.format("[%s.incomingLinks] contains some invalid node identifiers",
                                         node.getId()));
            }
            incomingLinks.forEach(n -> {
                if (workflow.getNodes().stream()
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
                TaoComponent component = findComponent(value, node.getComponentType());
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
                            final List<ParameterDescriptor> descriptorList = descriptors;
                            customValues.forEach(v -> {
                                ParameterDescriptor descriptor = descriptorList.stream()
                                        .filter(d -> d.getId().equals(v.getParameterName()))
                                        .findFirst().orElse(null);
                                if (descriptor == null) {
                                    errors.add("[node.customValues.parameterName] invalid parameter name");
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
                        List<WorkflowNodeDescriptor> workflowNodes = workflow.getNodes();
                        for (ComponentLink link : incomingLinks) {
                            String parentId = link.getInput().getParentId();
                            nodeBefore = workflowNodes.stream()
                                    .filter(n -> link.getSourceNodeId() == n.getId())
                                    .findFirst().orElse(null);
                            if (nodeBefore != null) {
                                try {
                                    TaoComponent parentComponent = findComponent(parentId, nodeBefore.getComponentType());
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
