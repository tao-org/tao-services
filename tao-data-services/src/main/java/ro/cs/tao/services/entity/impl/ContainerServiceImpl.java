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
import ro.cs.tao.docker.Application;
import ro.cs.tao.docker.Container;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.services.interfaces.ContainerService;
import ro.cs.tao.topology.TopologyManager;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author Cosmin Cara
 */
@Service("containerService")
public class ContainerServiceImpl
        extends EntityService<Container> implements ContainerService {

    private static final Set<String> winExtensions = new HashSet<String>() {{ add(".bat"); add(".exe"); }};

    @Autowired
    private PersistenceManager persistenceManager;
    private Logger logger = Logger.getLogger(ContainerService.class.getName());

    @Override
    public Container findById(String id) {
        Container container = null;
        container = persistenceManager.getContainerById(id);
        return container;
    }

    @Override
    public List<Container> list() {
        List<Container> containers = null;
        try {
            containers = persistenceManager.getContainers();
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
        return containers;
    }

    @Override
    public List<Container> list(Iterable<String> ids) {
        return persistenceManager.getContainers(ids);
    }

    @Override
    public Container save(Container object) {
        if (object != null) {
            try {
                Container shouldBeNull = findById(object.getId());
                if (shouldBeNull != null) {
                    return update(object);
                } else {
                    return persistenceManager.saveContainer(object);
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
            return persistenceManager.updateContainer(object);
        } catch (PersistenceException e) {
            logger.severe(e.getMessage());
            return null;
        }
    }

    @Override
    public void delete(String id) {
        try {
            persistenceManager.deleteContainer(id);
        } catch (PersistenceException e) {
            logger.severe(e.getMessage());
        }
    }

    @Override
    public String registerContainer(Path dockerFile, String shortName, String description, Container dbContainer) {
        String containerId = null;
        TopologyManager topologyManager = TopologyManager.getInstance();
        if (topologyManager.getDockerImage(shortName) == null) {
            containerId = topologyManager.registerImage(dockerFile, shortName, description);
            initializeContainer(containerId, shortName, dbContainer.getApplicationPath(),
                                dbContainer.getApplications());
        }
        return containerId;
    }

    @Override
    public Container initializeContainer(String id, String name, String path, List<Application> applications) {
        List<Container> containers = persistenceManager.getContainers();
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
            container = existing ? persistenceManager.updateContainer(container) : persistenceManager.saveContainer(container);
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
        value = entity.getTag();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[tag] cannot be empty");
        }
        List<Application> applications = entity.getApplications();
        if (applications.size() == 0) {
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
}
