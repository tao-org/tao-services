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
import ro.cs.tao.services.interfaces.ComponentService;
import ro.cs.tao.services.interfaces.GroupComponentService;
import ro.cs.tao.services.interfaces.WorkflowService;
import ro.cs.tao.workflow.ParameterValue;
import ro.cs.tao.workflow.WorkflowDescriptor;
import ro.cs.tao.workflow.WorkflowNodeDescriptor;
import ro.cs.tao.workflow.WorkflowNodeGroupDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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
    public WorkflowDescriptor update(WorkflowDescriptor object) {
        return save(object);
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
        //return persistenceManager.getWorkflowDescriptor(workflow.getId());
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
    public WorkflowDescriptor removeNode(long workflowId, WorkflowNodeDescriptor nodeDescriptor) throws PersistenceException {
        if (nodeDescriptor == null) {
            throw new PersistenceException("Cannot remove a null node");
        }
        if (nodeDescriptor.getId() != null) {
            throw new PersistenceException(String.format("Node [name:%s] is not an existing node",
                                                         nodeDescriptor.getName()));
        }
        WorkflowDescriptor workflow = persistenceManager.getWorkflowDescriptor(workflowId);
        if (workflow == null) {
            throw new PersistenceException("Node is not attached to an existing workflow");
        }
        nodeDescriptor.setWorkflow(workflow);
        persistenceManager.delete(nodeDescriptor);
        return persistenceManager.getWorkflowDescriptor(workflow.getId());
    }

    @Override
    public WorkflowDescriptor addLink(long sourceNodeId, String sourceTargetId,
                                      long targetNodeId, String targetSourceId) throws PersistenceException {
        if (sourceTargetId == null || targetSourceId == null || sourceNodeId == 0 || targetNodeId == 0) {
            throw new PersistenceException("Invalid link data");
        }

        WorkflowNodeDescriptor sourceNode = persistenceManager.getWorkflowNodeById(sourceNodeId);
        if (sourceNode == null) {
            throw new PersistenceException("Source node does not exist");
        }
        WorkflowNodeDescriptor targetNode = persistenceManager.getWorkflowNodeById(targetNodeId);
        if (targetNode == null) {
            throw new PersistenceException("Target node does not exist");
        }
        TaoComponent sourceComponent = findComponent(sourceNode.getComponentId());
        if (sourceComponent == null) {
            throw new PersistenceException("Source component not found");
        }
        TaoComponent targetComponent = findComponent(targetNode.getComponentId());
        if (targetComponent == null) {
            throw new PersistenceException("Target component not found");
        }
        if (targetNode.getIncomingLinks() != null &&
                targetNode.getIncomingLinks().stream().anyMatch(l -> l.getInput().getId().equals(sourceTargetId) &&
                                                                     l.getOutput().getId().equals(targetSourceId) &&
                                                                     l.getSourceNodeId().equals(sourceNodeId))) {
            throw new PersistenceException("Link already exists");
        }
        TargetDescriptor linkInput = sourceComponent.getTargets().stream()
                                                    .filter(t -> t.getId().equals(sourceTargetId))
                                                    .findFirst().get();
        SourceDescriptor linkOutput = targetComponent.getSources().stream()
                                                    .filter(s -> s.getId().equals(targetSourceId))
                                                    .findFirst().get();
        ComponentLink link = new ComponentLink(sourceNodeId, linkInput, linkOutput);
        List<ComponentLink> links = targetNode.getIncomingLinks();
        if (links == null) {
            links = new ArrayList<>();
        }
        links.add(link);
        targetNode.setIncomingLinks(links);
        persistenceManager.updateWorkflowNodeDescriptor(targetNode);
        return persistenceManager.getWorkflowDescriptor(targetNode.getWorkflow().getId());
    }

    @Override
    public WorkflowDescriptor removeLink(ComponentLink link) throws PersistenceException {
        if (link == null) {
            throw new PersistenceException("Cannot remove a null link");
        }
        if (link.getSourceNodeId() == null || link.getSourceNodeId() == 0) {
            throw new PersistenceException("Link doesn't have a source node set");
        }
        if (link.getInput() == null) {
            throw new PersistenceException("Invalid link input");
        }
        if (link.getOutput() == null) {
            throw new PersistenceException("Invalid link output");
        }
        WorkflowNodeDescriptor node = persistenceManager.getWorkflowNodeById(link.getSourceNodeId());
        if (node == null) {
            throw new PersistenceException("Link source does not exist");
        }
        String componentId = link.getOutput().getParentId();
        TaoComponent component = findComponent(componentId);
        if (component == null) {
            throw new PersistenceException(String.format("Inexistent component with identifier %s", componentId));
        }
        WorkflowDescriptor workflow = node.getWorkflow();
        List<WorkflowNodeDescriptor> nodes = persistenceManager.getWorkflowNodesByComponentId(workflow.getId(), componentId);
        node = nodes.stream().filter(n -> n.getIncomingLinks() != null &&
                n.getIncomingLinks().stream().anyMatch(l -> l.equals(link)))
                .findFirst()
                .orElse(null);
        if (node == null) {
            throw new PersistenceException("Link not found");
        }
        node.getIncomingLinks().removeIf(l -> l.equals(link));
        persistenceManager.updateWorkflowNodeDescriptor(node);
        return persistenceManager.getWorkflowDescriptor(workflow.getId());
    }

    @Override
    public WorkflowDescriptor addGroup(long workflowId, WorkflowNodeGroupDescriptor groupDescriptor,
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
            component = findComponent(nodeBefore.getComponentId());
        }
        int cardinality = component != null ? component.getTargetCardinality() : 1;
        GroupComponent groupComponent = GroupComponent.create(firstComponent.getSources(), cardinality,
                                                              lastComponent.getTargets(), cardinality);
        groupComponent = groupComponentService.save(groupComponent);
        groupDescriptor.setComponentId(groupComponent.getId());
        WorkflowDescriptor workflow = persistenceManager.getWorkflowDescriptor(workflowId);

        for (WorkflowNodeDescriptor node : nodes) {
            groupDescriptor.addNode(node);
        }
        groupDescriptor = (WorkflowNodeGroupDescriptor) persistenceManager.saveWorkflowNodeDescriptor(groupDescriptor, workflow);

        if (component != null) {
            List<ComponentLink> external = new ArrayList<>();
            TargetDescriptor target1 = component.getTargets().get(0);
            external.add(new ComponentLink(nodeBefore.getId(), target1,
                                           groupComponent.getSources().get(0)));
            groupDescriptor.setIncomingLinks(external);
            persistenceManager.updateWorkflowNodeDescriptor(groupDescriptor);
        }
        return persistenceManager.getWorkflowDescriptor(workflowId);
    }

    @Override
    public WorkflowDescriptor updateGroup(WorkflowNodeGroupDescriptor groupDescriptor) throws PersistenceException {
        return null;
    }

    @Override
    public WorkflowDescriptor removeGroup(WorkflowNodeGroupDescriptor groupDescriptor) throws PersistenceException {
        return null;
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

    private TaoComponent findComponent(String id) {
        TaoComponent component;
        try {
            component = persistenceManager.getProcessingComponentById(id);
        } catch (Exception ignored1) {
            try {
                component = persistenceManager.getGroupComponentById(id);
            } catch (Exception ignored2) {
                component = persistenceManager.getDataSourceInstance(id);
            }
        }
        return component;
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
        value = node.getComponentId();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[node] is not linked to a processing component");
        } else {
            TaoComponent component = findComponent(value);
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
                    for (ComponentLink link : incomingLinks) {
                        String parentId = link.getInput().getParentId();
                        try {
                            TaoComponent parentComponent = findComponent(parentId);
                            if (parentComponent == null) {
                                throw new PersistenceException();
                            }
                            linkedComponents.add(parentComponent);
                        } catch (PersistenceException e) {
                            errors.add(String.format("[node.componentId] cannot retrieve component with id = %s",
                                                     parentId));
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
