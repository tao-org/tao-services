/*
 * Copyright (C) 2017 CS ROMANIA
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
import ro.cs.tao.persistence.data.jsonutil.JacksonUtil;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.services.entity.controllers.ContainerController;
import ro.cs.tao.services.interfaces.ContainerService;
import ro.cs.tao.utils.Platform;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Cosmin Cara
 */
@Service("containerService")
public class ContainerServiceImpl
    extends EntityService<Container>
        implements ContainerService {

    @Autowired
    private PersistenceManager persistenceManager;
    private Logger logger = Logger.getLogger(ContainerService.class.getName());

    @Override
    public Container findById(String id) {
        Container container = null;
        try {
            container = persistenceManager.getContainerById(id);
        } catch (PersistenceException e) {
            logger.severe(e.getMessage());
        }
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
    public void save(Container object) {
        if (object != null) {
            try {
                Container shouldBeNull = findById(object.getId());
                if (shouldBeNull != null) {
                    update(object);
                } else {
                    persistenceManager.saveContainer(object);
                }
            } catch (PersistenceException e) {
                logger.severe(e.getMessage());
            }
        }
    }

    @Override
    public void update(Container object) {
        if (object != null) {
            try {
                persistenceManager.updateContainer(object);
            } catch (PersistenceException e) {
                logger.severe(e.getMessage());
            }
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
    public Container initOTB(String path) {
        List<Container> containers = persistenceManager.getContainers();
        boolean isWin = Platform.getCurrentPlatform().getId().equals(Platform.ID.win);
        Container otbContainer = null;
        if (containers == null || containers.stream().noneMatch(c -> c.getName().equals("OTB-6.4.0"))) {
            otbContainer = new Container();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(ContainerController.class.getResourceAsStream("otb_container.json")))) {
                String str = String.join("", reader.lines().collect(Collectors.toList()));
                otbContainer = JacksonUtil.fromString(str, Container.class);
                otbContainer.setApplicationPath(path);
                otbContainer.getApplications().forEach(a -> {
                    if (a.getPath() == null) {
                        a.setPath(a.getName());
                    }
                    if (isWin && !a.getPath().endsWith(".bat")) {
                        a.setPath(a.getPath() + ".bat");
                    }
                });
                otbContainer = persistenceManager.saveContainer(otbContainer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            otbContainer = containers.stream().filter(c -> c.getName().equals("OTB-6.4.0")).findFirst().orElse(null);
            try {
                otbContainer.setApplicationPath(path);
                otbContainer.getApplications().forEach(a -> {
                    if (a.getPath() == null) {
                        a.setPath(a.getName());
                    }
                    if (isWin && !a.getPath().endsWith(".bat")) {
                        a.setPath(a.getPath() + ".bat");
                    }
                });
                otbContainer = persistenceManager.updateContainer(otbContainer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return otbContainer;
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
