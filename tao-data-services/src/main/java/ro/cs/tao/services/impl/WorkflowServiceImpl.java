package ro.cs.tao.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import ro.cs.tao.component.ParameterDescriptor;
import ro.cs.tao.component.ProcessingComponent;
import ro.cs.tao.component.converters.ConverterFactory;
import ro.cs.tao.component.converters.ParameterConverter;
import ro.cs.tao.datasource.converters.ConversionException;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.services.interfaces.WorkflowService;
import ro.cs.tao.workflow.ParameterValue;
import ro.cs.tao.workflow.WorkflowDescriptor;
import ro.cs.tao.workflow.WorkflowNodeDescriptor;

import java.util.List;
import java.util.logging.Logger;

/**
 * @author Cosmin Cara
 */
public class WorkflowServiceImpl
        extends EntityService<WorkflowDescriptor> implements WorkflowService {

    @Autowired
    private PersistenceManager persistenceManager;
    private Logger logger = Logger.getLogger(WorkflowService.class.getName());

    @Override
    public WorkflowDescriptor findById(String id) {
        // TODO
        return null;
    }

    @Override
    public List<WorkflowDescriptor> list() {
        // TODO
        return null;
    }

    @Override
    public void save(WorkflowDescriptor object) {
        // TODO
    }

    @Override
    public void update(WorkflowDescriptor object) {
        // TODO
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
        String value = node.getName();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[node.name] cannot be empty");
        }
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
                }
            } catch (PersistenceException e) {
                logger.severe(e.getMessage());
            }
        }
        List<Long> incomingNodes = node.getIncomingNodes();
        if (incomingNodes != null) {
            if (incomingNodes.stream().anyMatch(n -> n <= 0)) {
                errors.add("[node.incomingNodes] contains some invalid node identifiers");
            }
            incomingNodes.forEach(n -> {
                if (workflow.getNodes().stream().noneMatch(nd -> nd.getId() == n)) {
                    errors.add("[node.incomingNodes] contains one or more invalid node identifiers");
                }
            });
        }
    }
}
