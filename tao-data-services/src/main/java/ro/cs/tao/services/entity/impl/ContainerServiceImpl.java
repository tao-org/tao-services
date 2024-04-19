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
import ro.cs.tao.Tag;
import ro.cs.tao.component.ParameterDescriptor;
import ro.cs.tao.component.ProcessingComponent;
import ro.cs.tao.component.SourceDescriptor;
import ro.cs.tao.component.TargetDescriptor;
import ro.cs.tao.component.enums.ProcessingComponentType;
import ro.cs.tao.component.enums.ProcessingComponentVisibility;
import ro.cs.tao.component.enums.TagType;
import ro.cs.tao.docker.Application;
import ro.cs.tao.docker.Container;
import ro.cs.tao.docker.ContainerType;
import ro.cs.tao.docker.ContainerVisibility;
import ro.cs.tao.messaging.Messaging;
import ro.cs.tao.messaging.Topic;
import ro.cs.tao.persistence.ContainerProvider;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.ProcessingComponentProvider;
import ro.cs.tao.persistence.TagProvider;
import ro.cs.tao.security.SystemPrincipal;
import ro.cs.tao.services.interfaces.ContainerService;
import ro.cs.tao.topology.docker.DockerManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * @author Cosmin Cara
 */
@Service("containerService")
public class ContainerServiceImpl
        extends EntityService<Container> implements ContainerService {

    @Autowired
    private ContainerProvider containerProvider;
    @Autowired
    private TagProvider tagProvider;
    @Autowired
    private ProcessingComponentProvider componentProvider;

    private Logger logger = Logger.getLogger(ContainerService.class.getName());

    @Override
    public Container findById(String id) {
        return containerProvider.get(id);
    }

    @Override
    public List<Container> list() {
        List<Container> containers = null;
        try {
            containers = containerProvider.list();
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
        return containers;
    }

    @Override
    public List<Container> list(Iterable<String> ids) {
        return containerProvider.list(ids);
    }

    @Override
    public List<Container> listByType(ContainerType type) {
        return containerProvider.getByType(type);
    }

    @Override
    public List<Container> listByTypeAndVisibility(ContainerType type, ContainerVisibility visibility) {
        return containerProvider.getByTypeAndVisibility(type, visibility);
    }

    @Override
    public List<Container> listContainersVisibleToUser(String userId) {
        return containerProvider.listContainersVisibleToUser(userId);
    }

    @Override
    public List<Container> listUserContainers(String userId) {
        return containerProvider.listUserContainers(userId);
    }

    @Override
    public Container save(Container object) {
        if (object != null) {
            try {
                Container shouldBeNull = findById(object.getId());
                if (shouldBeNull != null) {
                    return update(object);
                } else {
                    return containerProvider.save(object);
                }
            } catch (PersistenceException e) {
                logger.severe(e.getMessage());
                return null;
            }
        }
        return null;
    }

    @Override
    public Container update(Container object) {
        try {
            return containerProvider.update(object);
        } catch (PersistenceException e) {
            logger.severe(e.getMessage());
            return null;
        }
    }

    @Override
    public void delete(String id) {
        try {
            containerProvider.delete(id);
        } catch (PersistenceException e) {
            logger.severe(e.getMessage());
        }
    }

    @Override
    public String registerContainer(Path dockerFile, Container container, ProcessingComponent[] components) {
        String containerId;
        final Container dockerImage = DockerManager.getDockerImage(container.getName());
        if (dockerImage == null) {
            // build Docker image and put it in Docker registry
            containerId = DockerManager.registerImage(dockerFile, container.getName(), container.getDescription());
            // update database with container and applications information
            initializeContainer(containerId, container);
        } else {
            // the image is already registered with Docker, and should be also registered in the database
            Container c = containerProvider.getByName(container.getName());
            if (c == null) {
                container.setId(dockerImage.getId());
                try {
                    containerProvider.save(container);
                } catch (PersistenceException e) {
                    logger.warning(String.format("Container %s could not be registered. Reason: %s",
                                                 container.getName(), e.getMessage()));
                }
                containerId = container.getId();
            } else {
                containerId = c.getId();
            }
        }
        // if provided, register the components of this container
        if (containerId != null && components != null) {
            final List<Application> containerApplications = container.getApplications();
            List<Tag> componentTags = tagProvider.list(TagType.COMPONENT);
            if (componentTags == null) {
                componentTags = new ArrayList<>();
            }
            ProcessingComponent current = null;
            for (ProcessingComponent component : components) {
                try {
                    current = component;
                    component.setContainerId(containerId);
                    component.setLabel(component.getId());
                    component.setComponentType(ProcessingComponentType.EXECUTABLE);
                    component.setFileLocation(containerApplications.stream().filter(a -> a.getName().equals(component.getId())).findFirst().get().getPath());
                    List<ParameterDescriptor> parameterDescriptors = component.getParameterDescriptors();
                    if (parameterDescriptors != null) {
                        parameterDescriptors.forEach(p -> {
                            if (p.getName() == null) {
                                p.setName(p.getId());
                                p.setId(UUID.randomUUID().toString());
                            }
                            String[] valueSet = p.getValueSet();
                            if (valueSet != null && valueSet.length == 1 &&
                                    ("null".equals(valueSet[0]) || valueSet[0].isEmpty())) {
                                p.setValueSet(null);
                            }
                            if (valueSet != null && valueSet.length > 0 &&
                                    ("null".equals(valueSet[0]) || valueSet[0].isEmpty())) {
                                p.setDefaultValue(valueSet[0]);
                            }
                        });
                    }
                    List<SourceDescriptor> sources = component.getSources();
                    if (sources != null) {
                        sources.forEach(s -> s.setId(UUID.randomUUID().toString()));
                    }
                    List<TargetDescriptor> targets = component.getTargets();
                    if (targets != null) {
                        targets.forEach(t -> t.setId(UUID.randomUUID().toString()));
                    }
                    String template = component.getTemplateContents();
                    int i = 0;
                    while (i < template.length()) {
                        char ch = template.charAt(i);
                        if (ch == '$' && template.charAt(i - 1) != '\n') {
                            template = template.substring(0, i) + "\n" + template.substring(i);
                        }
                        i++;
                    }
                    String[] tokens = template.split("\n");
                    for (int j = 0; j < tokens.length - 1; j++) {
                        final int idx = j;
                        if ((targets != null && targets.stream().anyMatch(t -> t.getName().equals(tokens[idx].substring(1)))) ||
                                (sources != null && sources.stream().anyMatch(s -> s.getName().equals(tokens[idx].substring(1))))) {
                            tokens[j + 1] = tokens[j].replace('-', '$');
                            j++;
                        }
                    }
                    component.setTemplateContents(String.join("\n", tokens));
                    component.setComponentType(ProcessingComponentType.EXECUTABLE);
                    component.setVisibility(ProcessingComponentVisibility.SYSTEM);
                    component.setOwner(SystemPrincipal.instance().getName());
                    component.addTags(getOrCreateTag(componentTags, container.getName()).getText());
                    componentProvider.save(component);
                } catch (Exception inner) {
                    logger.severe(String.format("Faulty component: %s. Error: %s",
                                                current != null ? current.getId() : "n/a",
                                                inner.getClass().getSimpleName() + ":" + inner.getMessage()));
                }
            }
        }
        if (containerId != null) {
            Messaging.send(SystemPrincipal.instance(), Topic.CONTAINER.value(),
                           String.format("Container %s has been added. Visibility: %s",
                                         containerId, container.getVisibility().friendlyName()));
        }
        return containerId;
    }

    @Override
    public Container initializeContainer(String id, Container webContainer) {
        if (webContainer == null) {
            return null;
        }
        Container container = containerProvider.getByName(webContainer.getName());
        boolean existing = (container != null);
        if (!existing) {
            container = webContainer;
        } else {
            existing = id.equals(container.getId());
        }
        container.setId(id);
        try {
            container = existing ? containerProvider.update(container) : containerProvider.save(container);
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
        return container;
    }

    @Override
    public Container initializeContainer(String id, String name, String path, List<Application> applications) {
        List<Container> containers = containerProvider.list();
        Container container = null;
        if (containers != null) {
            container = containers.stream().filter(c -> c.getName().equals(name)).findFirst().orElse(null);
        }
        boolean existing = (container != null);
        if (!existing) {
            container = new Container();
            container.setName(name);
            container.setTag(name);
            container.setApplicationPath(path);
        } else {
            existing = id.equals(container.getId());
        }
        container.setId(id);
        for (Application app : applications) {
            Application application = new Application();
            application.setName(app.getName());
            application.setPath(app.getPath());
            container.addApplication(application);
        }

        try {
            container = existing ? containerProvider.update(container) : containerProvider.save(container);
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
        return container;
    }

    @Override
    protected void validateFields(Container entity, List<String> errors) {
        String value = entity.getId();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[id] cannot be empty");
        }
        value = entity.getName();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[name] cannot be empty");
        }
        /*value = entity.getTag();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[tag] cannot be empty");
        }*/
        List<Application> applications = entity.getApplications();
        if (applications.isEmpty()) {
            errors.add("[applications] cannot be empty");
        }
        applications.forEach(app -> {
            if (app == null) {
                errors.add("[applications] empty entity not allowed");
            } else {
                String val = app.getName();
                if (val == null || val.trim().isEmpty()) {
                    errors.add("[application.name] cannot be empty");
                }
                val = app.getPath();
                if (val == null || val.trim().isEmpty()) {
                    errors.add("[application.path] cannot be empty");
                }
            }
        });
    }

    private Tag getOrCreateTag(List<Tag> tags, String tagText) throws PersistenceException {
        Tag tag = tags.stream().filter(t -> t.getText().equalsIgnoreCase(tagText)).findFirst().orElse(null);
        if (tag == null) {
            tag = tagProvider.save(new Tag(TagType.COMPONENT, tagText));
            tags.add(tag);
        }
        return tag;
    }
}
