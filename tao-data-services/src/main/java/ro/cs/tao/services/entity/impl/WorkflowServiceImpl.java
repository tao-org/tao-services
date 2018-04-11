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
import ro.cs.tao.services.interfaces.WorkflowService;
import ro.cs.tao.workflow.ParameterValue;
import ro.cs.tao.workflow.WorkflowDescriptor;
import ro.cs.tao.workflow.WorkflowNodeDescriptor;

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
    public void save(WorkflowDescriptor object) {
        if (object != null) {
            List<WorkflowNodeDescriptor> nodes = object.getNodes();
            if (nodes != null) {
                nodes.forEach(node -> node.setWorkflow(object));
            }
            validate(object);
            try {
                persistenceManager.saveWorkflowDescriptor(object);
            } catch (PersistenceException e) {
                logger.severe(e.getMessage());
            }
        }
    }

    @Override
    public void update(WorkflowDescriptor object) {
        save(object);
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

    private void validateNode(WorkflowDescriptor workflow, WorkflowNodeDescriptor node, List<String> errors) {
        // Validate simple fields
        String value = node.getName();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[node.name] cannot be empty");
        }
        List<ComponentLink> incomingLinks = node.getIncomingLinks();
        if (incomingLinks != null) {
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
            try {
                ProcessingComponent component = persistenceManager.getProcessingComponentById(value);
                if (component == null) {
                    errors.add("[node.componentId] component does not exist");
                } else {
                    List<ParameterValue> customValues = node.getCustomValues();
                    // Validate custom parameter values for the attached component
                    if (customValues != null) {
                        final List<ParameterDescriptor> descriptors = component.getParameterDescriptors();
                        customValues.forEach(v -> {
                            ParameterDescriptor descriptor = descriptors.stream()
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
                    // Validate the compatibilities of the attached component with the declared incoming components
                    if (incomingLinks != null) {
                        List<ProcessingComponent> linkedComponents = new ArrayList<>();
                        for (ComponentLink link : incomingLinks) {
                            try {
                                linkedComponents.add(persistenceManager.getProcessingComponentById(link.getInput().getParentId()));
                            } catch (PersistenceException e) {
                                errors.add(String.format("[node.componentId] cannot retrieve component with id = %s",
                                                         node.getComponentId()));
                            }
                        }
                        List<SourceDescriptor> sources = component.getSources();
                        for (ProcessingComponent linkedComponent : linkedComponents) {
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
            } catch (PersistenceException e) {
                logger.severe(e.getMessage());
            }
        }
    }
}
