package ro.cs.tao.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import ro.cs.tao.component.ComponentLink;
import ro.cs.tao.component.ParameterDescriptor;
import ro.cs.tao.component.ProcessingComponent;
import ro.cs.tao.component.SourceDescriptor;
import ro.cs.tao.component.TargetDescriptor;
import ro.cs.tao.component.converters.ConverterFactory;
import ro.cs.tao.component.converters.ParameterConverter;
import ro.cs.tao.datasource.converters.ConversionException;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.services.interfaces.ComponentService;
import ro.cs.tao.services.interfaces.WorkflowService;
import ro.cs.tao.workflow.ParameterValue;
import ro.cs.tao.workflow.Visibility;
import ro.cs.tao.workflow.WorkflowDescriptor;
import ro.cs.tao.workflow.WorkflowNodeDescriptor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Cosmin Cara
 */
public class WorkflowServiceImpl
        extends EntityService<WorkflowDescriptor> implements WorkflowService {

    @Autowired
    private PersistenceManager persistenceManager;
    @Autowired
    private ComponentService componentService;

    private Logger logger = Logger.getLogger(WorkflowService.class.getName());

    @Override
    public WorkflowDescriptor findById(String id) {
        // TODO persistenceManager.getWorkflow(id);
        WorkflowDescriptor mock = new WorkflowDescriptor();
        mock.setId(1L);
        mock.setName("Test workflow");
        mock.setCreated(LocalDateTime.now());
        mock.setActive(true);
        mock.setUserName("admin");
        mock.setVisibility(Visibility.PRIVATE);
        addNodes(mock);
        return mock;
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
    public void delete(String id) {
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
                    .anyMatch(l -> l.getOutput().getParent().getId().equals(node.getComponentId()))) {
                errors.add("[node.incomingLinks] contains some invalid node identifiers");
            }
            incomingLinks.forEach(n -> {
                if (workflow.getNodes().stream()
                        .noneMatch(nd -> nd.getComponentId().equals(n.getInput().getParent().getId()))) {
                    errors.add("[node.incomingLinks] contains one or more invalid node identifiers");
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
                                linkedComponents.add(persistenceManager.getProcessingComponentById(link.getInput().getParent().getId()));
                            } catch (PersistenceException e) {
                                errors.add(String.format("[node.componentId] cannot retrieve component with id = %s",
                                                         node.getComponentId()));
                            }
                        }
                        List<SourceDescriptor> sources = Arrays.asList(component.getSources());
                        for (ProcessingComponent linkedComponent : linkedComponents) {
                            List<TargetDescriptor> targets = Arrays.asList(linkedComponent.getTargets());
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

    private void addNodes(WorkflowDescriptor parent) {
        if (parent.getNodes() == null) {
            parent.setNodes(new ArrayList<>());
        }
        List<WorkflowNodeDescriptor> nodes = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            int nodeNumber = i + 1;
            WorkflowNodeDescriptor node = new WorkflowNodeDescriptor();
            node.setId(nodeNumber);
            node.setWorkflow(parent);
            node.setName("Node-" + nodeNumber);
            node.setxCoord(10 + 200 * (nodeNumber - 1));
            node.setyCoord(10 + 200 * (nodeNumber - 1));
            node.setComponentId("segmentation-cc-" + nodeNumber);
            ArrayList<ParameterValue> values = new ArrayList<>();
            ParameterValue value = new ParameterValue();
            value.setParameterName("neighbor_bool");
            value.setParameterValue(String.valueOf(nodeNumber % 2 == 1));
            node.setCustomValues(values);
            nodes.add(node);
        }
        ProcessingComponent component1 = componentService.findById(nodes.get(0).getComponentId());
        ProcessingComponent component2 = componentService.findById(nodes.get(1).getComponentId());
        ArrayList<ComponentLink> links = new ArrayList<>();
        ComponentLink link = new ComponentLink(component1.getTargets()[0],
                                               component2.getSources()[0]);
        links.add(link);
        nodes.get(1).setIncomingLinks(links);
        parent.setNodes(nodes);
    }
}
