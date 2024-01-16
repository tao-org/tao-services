package ro.cs.tao.services.entity.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.Sort;
import ro.cs.tao.component.ParameterDescriptor;
import ro.cs.tao.component.SourceDescriptor;
import ro.cs.tao.component.TargetDescriptor;
import ro.cs.tao.component.enums.ProcessingComponentVisibility;
import ro.cs.tao.component.ogc.WMSComponent;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.WMSComponentProvider;
import ro.cs.tao.services.entity.util.ServiceTransformUtils;
import ro.cs.tao.services.interfaces.WMSComponentService;
import ro.cs.tao.services.model.component.WMSComponentInfo;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service("wmsComponentService")
public class WMSComponentServiceImpl extends EntityService<WMSComponent> implements WMSComponentService {

    @Autowired
    private WMSComponentProvider wmsComponentProvider;

    @Override
    public WMSComponent findById(String id) {
        return wmsComponentProvider.get(id);
    }

    @Override
    public List<WMSComponent> list() {
        List<WMSComponent> components = null;
        try {
            components = wmsComponentProvider.list();
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
        return components;
    }

    @Override
    public List<WMSComponent> list(Iterable<String> ids) {
        return wmsComponentProvider.list(ids);
    }

    @Override
    public List<WMSComponent> list(Optional<Integer> pageNumber, Optional<Integer> pageSize, Sort sort) {
        if (pageNumber.isPresent() && pageSize.isPresent()) {
            return wmsComponentProvider.list(pageNumber.get(), pageSize.get(), sort);
        } else {
            return wmsComponentProvider.list();
        }
    }

    @Override
    public WMSComponent save(WMSComponent component) {
        if (component != null) {
            try {
                return wmsComponentProvider.save(component);
            } catch (PersistenceException e) {
                logger.severe(e.getMessage());
            }
        }
        return null;
    }

    @Override
    public WMSComponent update(WMSComponent component) {
        try{
            return wmsComponentProvider.update(component);
        } catch (PersistenceException e) {
            logger.severe(e.getMessage());
            return null;
        }
    }

    @Override
    public void delete(String id) throws PersistenceException {
        try{
            wmsComponentProvider.delete(id);
        } catch (PersistenceException ex) {
            logger.severe(ex.getMessage());
        }
    }

    @Override
    public List<WMSComponentInfo> getWPSComponents() {
        return ServiceTransformUtils.toWMSComponentInfos(list());
    }

    @Override
    public List<WMSComponentInfo> getWPSComponents(int pageNumber, int pageSize, Sort sort) {
        return ServiceTransformUtils.toWMSComponentInfos(list(Optional.of(pageNumber),
                                                                Optional.of(pageSize), sort));
    }

    @Override
    protected void validateFields(WMSComponent entity, List<String> errors) {
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
