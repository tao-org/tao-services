package ro.cs.tao.services.entity.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.Sort;
import ro.cs.tao.component.PassThroughComponent;
import ro.cs.tao.component.SourceDescriptor;
import ro.cs.tao.component.TargetDescriptor;
import ro.cs.tao.component.enums.ProcessingComponentVisibility;
import ro.cs.tao.persistence.PassThroughComponentProvider;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.services.interfaces.PassThroughComponentService;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service("passThroughComponentService")
public class PassThroughComponentServiceImpl extends EntityService<PassThroughComponent> implements PassThroughComponentService {

    @Autowired
    private PassThroughComponentProvider passThroughComponentProvider;

    @Override
    public PassThroughComponent findById(String id) {
        return passThroughComponentProvider.get(id);
    }

    @Override
    public List<PassThroughComponent> list() {
        List<PassThroughComponent> components = null;
        try {
            components = passThroughComponentProvider.list();
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
        return components;
    }

    @Override
    public List<PassThroughComponent> list(Iterable<String> ids) {
        return passThroughComponentProvider.list(ids);
    }

    @Override
    public List<PassThroughComponent> list(Optional<Integer> pageNumber, Optional<Integer> pageSize, Sort sort) {
        if (pageNumber.isPresent() && pageSize.isPresent()) {
            return passThroughComponentProvider.list(pageNumber.get(), pageSize.get(), sort);
        } else {
            return passThroughComponentProvider.list();
        }
    }

    @Override
    public PassThroughComponent save(PassThroughComponent component) {
        if (component != null) {
            try {
                return passThroughComponentProvider.save(component);
            } catch (PersistenceException e) {
                logger.severe(e.getMessage());
            }
        }
        return null;
    }

    @Override
    public PassThroughComponent update(PassThroughComponent component) {
        try{
            return passThroughComponentProvider.update(component);
        } catch (PersistenceException e) {
            logger.severe(e.getMessage());
            return null;
        }
    }

    @Override
    public void delete(String id) throws PersistenceException {
        try{
            passThroughComponentProvider.delete(id);
        } catch (PersistenceException ex) {
            logger.severe(ex.getMessage());
        }
    }

    @Override
    protected void validateFields(PassThroughComponent entity, List<String> errors) {
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
