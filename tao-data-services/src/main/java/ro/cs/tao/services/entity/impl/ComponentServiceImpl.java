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
import ro.cs.tao.Sort;
import ro.cs.tao.Tag;
import ro.cs.tao.component.*;
import ro.cs.tao.component.constraints.ConstraintFactory;
import ro.cs.tao.component.enums.TagType;
import ro.cs.tao.component.template.Template;
import ro.cs.tao.component.template.TemplateException;
import ro.cs.tao.component.template.TemplateType;
import ro.cs.tao.component.validation.ValidationException;
import ro.cs.tao.docker.Application;
import ro.cs.tao.docker.Container;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.security.SystemSessionContext;
import ro.cs.tao.serialization.MediaType;
import ro.cs.tao.serialization.SerializationException;
import ro.cs.tao.serialization.Serializer;
import ro.cs.tao.serialization.SerializerFactory;
import ro.cs.tao.services.entity.util.ServiceTransformUtils;
import ro.cs.tao.services.interfaces.ComponentService;
import ro.cs.tao.services.model.component.ProcessingComponentInfo;

import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Cosmin Cara
 */
@Service("componentService")
public class ComponentServiceImpl
    extends EntityService<ProcessingComponent>
        implements ComponentService {

    @Autowired
    private PersistenceManager persistenceManager;
    private Logger logger = Logger.getLogger(ComponentService.class.getName());

    @Override
    public ProcessingComponent findById(String id) {
        return persistenceManager.getProcessingComponentById(id);
    }

    @Override
    public List<ProcessingComponent> list() {
        List<ProcessingComponent> components = null;
        try {
            components = persistenceManager.getProcessingComponents();
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
        return components;
    }

    @Override
    public List<ProcessingComponent> list(Iterable<String> ids) {
        return persistenceManager.getProcessingComponents(ids);
    }

    @Override
    public List<ProcessingComponent> list(Optional<Integer> pageNumber, Optional<Integer> pageSize, Sort sort) {
        if (pageNumber.isPresent() && pageSize.isPresent()) {
            return persistenceManager.getProcessingComponents(pageNumber.get(), pageSize.get(), sort);
        } else {
            return persistenceManager.getProcessingComponents();
        }
    }

    @Override
    public List<ProcessingComponentInfo> getProcessingComponents(int pageNumber, int pageSize, Sort sort) {
        return ServiceTransformUtils.toProcessingComponentInfos(list(Optional.of(pageNumber),
                                                                     Optional.of(pageSize), sort));
    }

    @Override
    public List<ProcessingComponentInfo> getProcessingComponents() {
        return ServiceTransformUtils.toProcessingComponentInfos(list());
    }

    @Override
    public List<ProcessingComponentInfo> getUserProcessingComponents(String userName) {
        return ServiceTransformUtils.toProcessingComponentInfos(persistenceManager.getUserProcessingComponents(userName));
    }

    @Override
    public List<ProcessingComponentInfo> getUserScriptComponents(String userName) {
        return ServiceTransformUtils.toProcessingComponentInfos(persistenceManager.getUserScriptComponents(userName));
    }

    @Override
    public ProcessingComponent save(ProcessingComponent component) {
        if (component != null) {
            if (persistenceManager.existsProcessingComponent(component.getId())) {
                return update(component);
            } else {
                try {
                    addTagsIfNew(component);
                    return persistenceManager.saveProcessingComponent(component);
                } catch (PersistenceException e) {
                    logger.severe(e.getMessage());
                    return null;
                }
            }
        }
        return null;
    }

    @Override
    public ProcessingComponent update(ProcessingComponent component) {
        try {
            addTagsIfNew(component);
            return persistenceManager.updateProcessingComponent(component);
        } catch (PersistenceException e) {
            logger.severe(e.getMessage());
            return null;
        }
    }

    @Override
    public void delete(String name) {
        try {
            persistenceManager.deleteProcessingComponent(name);
        } catch (PersistenceException e) {
            logger.severe(e.getMessage());
        }
    }

    @Override
    public ProcessingComponent tag(String id, List<String> tags) throws PersistenceException {
        ProcessingComponent entity = findById(id);
        if (entity == null) {
            throw new PersistenceException(String.format("Component with id '%s' not found", id));
        }
        if (tags != null && tags.size() > 0) {
            Set<String> existingTags = persistenceManager.getWorkflowTags().stream()
                    .map(Tag::getText).collect(Collectors.toSet());
            for (String value : tags) {
                if (!existingTags.contains(value)) {
                    persistenceManager.saveTag(new Tag(TagType.COMPONENT, value));
                }
            }
            entity.setTags(tags);
            return update(entity);
        }
        return entity;
    }

    @Override
    public ProcessingComponent untag(String id, List<String> tags) throws PersistenceException {
        ProcessingComponent entity = findById(id);
        if (entity == null) {
            throw new PersistenceException(String.format("Component with id '%s' not found", id));
        }
        if (tags != null && tags.size() > 0) {
            List<String> entityTags = entity.getTags();
            if (entityTags != null) {
                for (String value : tags) {
                    entityTags.remove(value);
                }
                entity.setTags(entityTags);
                return update(entity);
            }
        }
        return entity;
    }

    @Override
    public void validate(ProcessingComponent entity) throws ValidationException {
        super.validate(entity);
    }

    @Override
    public ProcessingComponent importFrom(MediaType mediaType, String data) throws SerializationException {
        Serializer<ProcessingComponent, String> serializer = SerializerFactory.create(ProcessingComponent.class, mediaType);
        return serializer.deserialize(data);
    }

    @Override
    public String exportTo(MediaType mediaType, ProcessingComponent component) throws SerializationException {
        Serializer<ProcessingComponent, String> serializer = SerializerFactory.create(ProcessingComponent.class, mediaType);
        return serializer.serialize(component);
    }

    @Override
    public List<String> getAvailableConstraints() {
        return ConstraintFactory.getAvailableConstraints();
    }

    @Override
    public List<Tag> getComponentTags() {
        return persistenceManager.getComponentTags();
    }

    @Override
    protected void validateFields(ProcessingComponent entity, List<String> errors) {
        String value = entity.getId();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[id] cannot be empty");
        }
        value = entity.getContainerId();
        Container container = null;
        if (value == null || value.trim().isEmpty()) {
            errors.add("[containerId] cannot be empty");
        } else {
            if ((container = persistenceManager.getContainerById(value)) == null) {
                errors.add("[containerId] points to a non-existing container");
            }
        }
        value = entity.getLabel();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[label] cannot be empty");
        }
        value = entity.getVersion();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[version] cannot be empty");
        }
        value = entity.getFileLocation();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[fileLocation] cannot be empty");
        } else {
            if (container != null) {
                List<Application> applications = container.getApplications();
                String v = value;
                final Container c = container;
                if (applications != null &&
                        applications.stream().noneMatch(a -> v.equals(a.getPath()) || v.equals(Paths.get(c.getApplicationPath(), a.getName()).toString()))) {
                    errors.add("[fileLocation] has an invalid value");
                }
            }
        }
        value = entity.getWorkingDirectory();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[workingDirectory] cannot be empty");
        } else {
            try {
                Paths.get(value);
            } catch (InvalidPathException e) {
                try {
                    Paths.get(value.replace(SystemVariable.USER_WORKSPACE.key(), SystemSessionContext.instance().getWorkspace().toString()));
                } catch (InvalidPathException e1) {
                    errors.add("[workingDirectory] has an invalid value");
                }
            }
        }
        TemplateType templateType = entity.getTemplateType();
        if (templateType == null) {
            errors.add("[templateType] cannot be determined");
        } else {
            Template template = entity.getTemplate();
            if (template == null || template.getContents() == null || template.getContents().isEmpty()) {
                errors.add("[template] cannot be empty");
            } else {
                try {
                    entity.getTemplateEngine().parse(template);
                } catch (TemplateException e) {
                    errors.add("[template] has parsing errors: " + e.getMessage());
                }
            }
        }
        List<ParameterDescriptor> parameterDescriptors = entity.getParameterDescriptors();
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
                if (Date.class.equals(dataType)) {
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
            Set<String> duplicates = sources.stream().filter(s -> !uniques.add(s.getName()))
                                                     .map(SourceDescriptor::getName)
                                                     .collect(Collectors.toSet());
            if (duplicates.size() > 0) {
                errors.add(String.format("[sources] contain duplicate names: %s", String.join(",", duplicates)));
                uniques.clear();
            }
            duplicates = sources.stream().filter(s -> s.getId() != null && !uniques.add(s.getId()))
                                         .map(SourceDescriptor::getId).collect(Collectors.toSet());
            if (duplicates.size() > 0) {
                errors.add(String.format("[sources] contain duplicate ids: %s", String.join(",", duplicates)));
                uniques.clear();
            }
        }
        List<TargetDescriptor> targets = entity.getTargets();
        if (targets == null || targets.isEmpty()) {
            errors.add("[targets] at least one target must be defined");
        } else {
            Set<String> duplicates = targets.stream().filter(t -> !uniques.add(t.getName()))
                    .map(TargetDescriptor::getName)
                    .collect(Collectors.toSet());
            if (duplicates.size() > 0) {
                errors.add(String.format("[targets] contain duplicate names: %s", String.join(",", duplicates)));
                uniques.clear();
            }
            duplicates = targets.stream().filter(t -> t.getId() != null && !uniques.add(t.getId()))
                    .map(TargetDescriptor::getId).collect(Collectors.toSet());
            if (duplicates.size() > 0) {
                errors.add(String.format("[targets] contain duplicate ids: %s", String.join(",", duplicates)));
                uniques.clear();
            }
        }
    }

    private void addTagsIfNew(TaoComponent component) {
        List<String> tags = component.getTags();
        if (tags != null) {
            List<Tag> componentTags = persistenceManager.getComponentTags();
            for (String value : tags) {
                if (componentTags.stream().noneMatch(t -> t.getText().equalsIgnoreCase(value))) {
                    persistenceManager.saveTag(new Tag(TagType.COMPONENT, value));
                }
            }
        }
    }
}
