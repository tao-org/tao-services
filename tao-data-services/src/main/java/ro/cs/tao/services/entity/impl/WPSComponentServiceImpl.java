package ro.cs.tao.services.entity.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.Sort;
import ro.cs.tao.component.ParameterDescriptor;
import ro.cs.tao.component.SourceDescriptor;
import ro.cs.tao.component.TargetDescriptor;
import ro.cs.tao.component.enums.ProcessingComponentVisibility;
import ro.cs.tao.component.ogc.WPSComponent;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.WPSComponentProvider;
import ro.cs.tao.services.entity.util.ServiceTransformUtils;
import ro.cs.tao.services.interfaces.WPSComponentService;
import ro.cs.tao.services.model.component.WPSComponentInfo;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service("wpsComponentService")
public class WPSComponentServiceImpl extends EntityService<WPSComponent> implements WPSComponentService {

    @Autowired
    private WPSComponentProvider wpsComponentProvider;

    @Override
    public WPSComponent findById(String id) {
        return wpsComponentProvider.get(id);
    }

    @Override
    public List<WPSComponent> list() {
        List<WPSComponent> components = null;
        try {
            components = wpsComponentProvider.list();
            if (components == null) {
                components = new ArrayList<>();
            }
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
        return components;
    }

    @Override
    public List<WPSComponent> list(Iterable<String> ids) {
        return wpsComponentProvider.list(ids);
    }

    @Override
    public List<WPSComponent> list(Optional<Integer> pageNumber, Optional<Integer> pageSize, Sort sort) {
        List<WPSComponent> components;
        if (pageNumber.isPresent() && pageSize.isPresent()) {
            components = wpsComponentProvider.list(pageNumber.get(), pageSize.get(), sort);
        } else {
            components = wpsComponentProvider.list();
        }
        if (components == null) {
            return new ArrayList<>();
        }
        return components;
    }

    @Override
    public WPSComponent save(WPSComponent component) {
        if (component != null) {
            try {
                return wpsComponentProvider.save(component);
            } catch (PersistenceException e) {
                logger.severe(e.getMessage());
            }
        }
        return null;
    }

    @Override
    public WPSComponent update(WPSComponent component) {
        try{
            return wpsComponentProvider.update(component);
        } catch (PersistenceException e) {
            logger.severe(e.getMessage());
            return null;
        }
    }

    @Override
    public void delete(String id) throws PersistenceException {
        try{
            wpsComponentProvider.delete(id);
        } catch (PersistenceException ex) {
            logger.severe(ex.getMessage());
        }
    }

    @Override
    public List<WPSComponentInfo> getWPSComponents() {
        return ServiceTransformUtils.toWPSComponentInfos(list());
    }

    @Override
    public List<WPSComponentInfo> getWPSComponents(int pageNumber, int pageSize, Sort sort) {
        return ServiceTransformUtils.toWPSComponentInfos(list(Optional.of(pageNumber),
                                                                Optional.of(pageSize), sort));
    }

    @Override
    protected void validateFields(WPSComponent entity, List<String> errors) {
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
        value = entity.getRemoteAddress();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[remoteAddress] cannot be empty");
        }
        value = entity.getCapabilityName();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[capabilityName] cannot be empty");
        }
        /*final WPSAuthentication authentication = entity.getAuthentication();
        if (authentication != null) {
            value = authentication.getLoginUrl();
            if (value == null || value.trim().isEmpty()) {
                errors.add("[authenticationEndpoint] cannot be empty");
            }
            value = authentication.getAuthHeader();
            if (value == null || value.trim().isEmpty()) {
                errors.add("[headerName] cannot be empty");
            }
            value = authentication.getUser();
            if (value == null || value.trim().isEmpty()) {
                errors.add("[username] cannot be empty");
            }
            value = authentication.getPassword();
            if (value == null || value.trim().isEmpty()) {
                errors.add("[password] cannot be empty");
            }
        }*/
        ProcessingComponentVisibility visibility = entity.getVisibility();
        if (visibility == null) {
            errors.add("[visibility] cannot be empty");
        }
        List<ParameterDescriptor> parameterDescriptors = entity.getParameters();
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
