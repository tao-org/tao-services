package ro.cs.tao.services.entity.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.Sort;
import ro.cs.tao.component.ConditionComponent;
import ro.cs.tao.component.ParameterDescriptor;
import ro.cs.tao.component.SourceDescriptor;
import ro.cs.tao.component.TargetDescriptor;
import ro.cs.tao.component.enums.ProcessingComponentVisibility;
import ro.cs.tao.persistence.ConditionComponentProvider;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.services.interfaces.ConditionComponentService;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service("conditionComponentService")
public class ConditionComponentServiceImpl extends EntityService<ConditionComponent> implements ConditionComponentService {

    @Autowired
    private ConditionComponentProvider conditionComponentProvider;

    @Override
    public ConditionComponent findById(String id) {
        return conditionComponentProvider.get(id);
    }

    @Override
    public List<ConditionComponent> list() {
        List<ConditionComponent> components = null;
        try {
            components = conditionComponentProvider.list();
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
        return components;
    }

    @Override
    public List<ConditionComponent> list(Iterable<String> ids) {
        return conditionComponentProvider.list(ids);
    }

    @Override
    public List<ConditionComponent> list(Optional<Integer> pageNumber, Optional<Integer> pageSize, Sort sort) {
        if (pageNumber.isPresent() && pageSize.isPresent()) {
            return conditionComponentProvider.list(pageNumber.get(), pageSize.get(), sort);
        } else {
            return conditionComponentProvider.list();
        }
    }

    @Override
    public ConditionComponent save(ConditionComponent component) {
        if (component != null) {
            try {
                return conditionComponentProvider.save(component);
            } catch (PersistenceException e) {
                logger.severe(e.getMessage());
            }
        }
        return null;
    }

    @Override
    public ConditionComponent update(ConditionComponent component) {
        try{
            return conditionComponentProvider.update(component);
        } catch (PersistenceException e) {
            logger.severe(e.getMessage());
            return null;
        }
    }

    @Override
    public void delete(String id) throws PersistenceException {
        try{
            conditionComponentProvider.delete(id);
        } catch (PersistenceException ex) {
            logger.severe(ex.getMessage());
        }
    }

    @Override
    protected void validateFields(ConditionComponent entity, List<String> errors) {
        String value = entity.getId();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[id] cannot be empty");
        }
        value = entity.getLabel();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[label] cannot be empty");
        }
        value = entity.getVersion();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[version] cannot be empty");
        }
        value = entity.getDescription();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[description] cannot be empty");
        }
        value = entity.getAuthors();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[authors] cannot be empty");
        }
        value = entity.getCopyright();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[copyright] cannot be empty");
        }
        ProcessingComponentVisibility visibility = entity.getVisibility();
        if (visibility == null) {
            errors.add("[visibility] cannot be empty");
        }
        Set<ParameterDescriptor> parameterDescriptors = entity.getParameterDescriptors();
        if (parameterDescriptors != null) {
            for (ParameterDescriptor descriptor : parameterDescriptors) {
                String descriptorId = descriptor.getId();
                if (descriptorId == null || descriptorId.trim().isEmpty()) {
                    errors.add("Invalid parameter found (missing id)");
                    continue;
                }
                value = descriptor.getLabel();
                if (value == null || value.trim().isEmpty()) {
                    errors.add(String.format("[$%s] label cannot be empty", descriptorId));
                }
                Class<?> dataType = descriptor.getDataType();
                if (dataType == null) {
                    errors.add(String.format("[$%s] cannot determine type", descriptorId));
                }
                if (LocalDateTime.class.equals(dataType)) {
                    value = descriptor.getFormat();
                    if (value == null || value.trim().isEmpty()) {
                        errors.add(String.format("[$%s] format for date parameter not specified", descriptorId));
                    }
                }
                value = descriptor.getDefaultValue();
                if (descriptor.isNotNull() && (value == null || value.trim().isEmpty())) {
                    errors.add(String.format("[$%s] is mandatory, but has no default value", descriptorId));
                }
            }

        }
        List<SourceDescriptor> sources = entity.getSources();
        Set<String> uniques = new HashSet<>();
        if (sources == null || sources.isEmpty()) {
            errors.add("[sources] at least one source must be defined");
        } else {
            Set<String> duplicates = sources.stream().map(SourceDescriptor::getName)
                                            .filter(name -> !uniques.add(name))
                                            .collect(Collectors.toSet());
            if (!duplicates.isEmpty()) {
                errors.add(String.format("[sources] contain duplicate names: %s", String.join(",", duplicates)));
                uniques.clear();
            }
            duplicates = sources.stream().filter(s -> s.getId() != null && !uniques.add(s.getId()))
                                .map(SourceDescriptor::getId).collect(Collectors.toSet());
            if (!duplicates.isEmpty()) {
                errors.add(String.format("[sources] contain duplicate ids: %s", String.join(",", duplicates)));
                uniques.clear();
            }
        }
        List<TargetDescriptor> targets = entity.getTargets();
        if (targets == null || targets.isEmpty()) {
            errors.add("[targets] at least one target must be defined");
        } else {
            Set<String> duplicates = targets.stream().map(TargetDescriptor::getName)
                                            .filter(name -> !uniques.add(name))
                                            .collect(Collectors.toSet());
            if (!duplicates.isEmpty()) {
                errors.add(String.format("[targets] contain duplicate names: %s", String.join(",", duplicates)));
                uniques.clear();
            }
            duplicates = targets.stream().filter(t -> t.getId() != null && !uniques.add(t.getId()))
                                .map(TargetDescriptor::getId).collect(Collectors.toSet());
            if (!duplicates.isEmpty()) {
                errors.add(String.format("[targets] contain duplicate ids: %s", String.join(",", duplicates)));
                uniques.clear();
            }
        }
    }
}
