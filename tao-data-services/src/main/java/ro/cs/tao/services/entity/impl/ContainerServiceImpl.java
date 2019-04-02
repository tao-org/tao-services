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

import org.apache.commons.lang3.SystemUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.docker.Application;
import ro.cs.tao.docker.Container;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.services.interfaces.ContainerService;
import ro.cs.tao.topology.TopologyManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author Cosmin Cara
 */
@Service("containerService")
public class ContainerServiceImpl
    extends EntityService<Container>
        implements ContainerService<MultipartFile> {

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
    public String registerContainer(MultipartFile dockerFile, String shortName, String description) throws IOException {
        String containerId = null;
        String fileName = StringUtils.cleanPath(dockerFile.getOriginalFilename());
        if (dockerFile.isEmpty()) {
            throw new IOException("Failed to store empty docker file " + fileName);
        }
        if (fileName.contains("..")) {
            // This is a security check
            throw new IOException( "Cannot store docker file with relative path outside image directory " + fileName);
        }
        Path filePath;
        try (InputStream inputStream = dockerFile.getInputStream()) {
            Path imagesPath = Paths.get(ConfigurationManager.getInstance().getValue("tao.docker.images"), shortName.replace(" ", "-"));
            Files.createDirectories(imagesPath);
            filePath = imagesPath.resolve("Dockerfile");
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        }
        TopologyManager topologyManager = TopologyManager.getInstance();
        if (topologyManager.getDockerImage(shortName) == null) {
            containerId = topologyManager.registerImage(filePath, shortName, description);
        }
        return containerId;
    }

    @Override
    public Container initializeContainer(String id, String name, String path, List<Application> applications) {
        List<Container> containers = persistenceManager.getContainers();
        Container container;
        if (containers == null || (container = containers.stream().filter(c -> c.getName().equals(name)).findFirst().orElse(null)) == null) {
            container = new Container();
            container.setId(id);
            container.setName(name);
            container.setTag(name);
            container.setApplicationPath(path);
            String appPath;
            for (Application app : applications) {
                Application application = new Application();
                appPath = app.getPath() + (SystemUtils.IS_OS_WINDOWS && (winExtensions.stream()
                                                              .noneMatch(e -> path.toLowerCase().endsWith(e))) ? ".bat" : "");
                application.setName(app.getName());
                application.setPath(appPath);
                container.addApplication(application);
            }
            try {
                container = persistenceManager.saveContainer(container);
            } catch (Exception e) {
                logger.severe(e.getMessage());
            }
        } else {
            container.setId(id);
            container.setName(name);
            container.setTag(name);
            String appPath;
            for (Application app : applications) {
                Application application = new Application();
                appPath = app.getName() + (SystemUtils.IS_OS_WINDOWS && (winExtensions.stream()
                                                            .noneMatch(e -> path.toLowerCase().endsWith(e))) ? ".bat" : "");
                application.setName(app.getName());
                application.setPath(appPath);
                container.addApplication(application);
            }
            try {
                container = persistenceManager.updateContainer(container);
            } catch (Exception e) {
                logger.severe(e.getMessage());
            }
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
