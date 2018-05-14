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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Cosmin Cara
 */
@Service("containerService")
public class ContainerServiceImpl
    extends EntityService<Container>
        implements ContainerService {

    private static final Set<String> winExtensions = new HashSet<String>() {{ add(".bat"); add(".exe"); }};

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
    public Container initializeContainer(String name, String path, List<String> applicationNames) {
        List<Container> containers = persistenceManager.getContainers();
        boolean isWin = Platform.getCurrentPlatform().getId().equals(Platform.ID.win);
        Container container = null;
        if (containers == null || containers.stream().noneMatch(c -> c.getName().equals(name))) {
            container = new Container();
            container.setId(name);
            container.setName(name);
            container.setTag(name);
            container.setApplicationPath(path);
            String appPath;
            for (String appName : applicationNames) {
                Application application = new Application();
                appPath = appName + (isWin && (winExtensions.stream()
                                                            .noneMatch(e -> path.toLowerCase().endsWith(e))) ? ".bat" : "");
                application.setName(appName);
                application.setPath(appPath);
                container.addApplication(application);
            }
            try {
                container = persistenceManager.saveContainer(container);
            } catch (Exception e) {
                logger.severe(e.getMessage());
            }
        } else {
            container = containers.stream().filter(c -> c.getName().equals(name)).findFirst().orElse(null);
            container.setName(name);
            container.setTag(name);
            String appPath;
            for (String appName : applicationNames) {
                Application application = new Application();
                appPath = appName + (isWin && (winExtensions.stream()
                                                            .noneMatch(e -> path.toLowerCase().endsWith(e))) ? ".bat" : "");
                application.setName(appName);
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
    public Container initOTB(String name, String path) {
        Container otbContainer = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(ContainerController.class.getResourceAsStream("otb_container.json")))) {
            String str = String.join("", reader.lines().collect(Collectors.toList()));
            Container tmp = JacksonUtil.fromString(str, Container.class);
            List<String> applications = tmp.getApplications().stream().map(Application::getName).collect(Collectors.toList());
            otbContainer = initializeContainer(name, path, applications);
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
        return otbContainer;
    }

    @Override
    public Container initSNAP(String name, String path) {
        List<Container> containers = persistenceManager.getContainers();
        boolean isWin = Platform.getCurrentPlatform().getId().equals(Platform.ID.win);
        Container snapContainer;
        if (containers == null || containers.stream().noneMatch(c -> c.getName().equals(name))) {
            snapContainer = new Container();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(ContainerController.class.getResourceAsStream("snap_container.json")))) {
                String str = String.join("", reader.lines().collect(Collectors.toList()));
                snapContainer = JacksonUtil.fromString(str, Container.class);
                snapContainer.setId(name);
                snapContainer.setName(name);
                snapContainer.setTag(name);
                snapContainer.setApplicationPath(path);
                snapContainer.getApplications().forEach(a -> {
                    if (a.getPath() == null) {
                        a.setPath("gpt");
                    }
                    if (isWin && !a.getPath().endsWith(".exe")) {
                        a.setPath(a.getPath() + ".exe");
                    }
                });
                snapContainer = persistenceManager.saveContainer(snapContainer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            snapContainer = containers.stream().filter(c -> c.getName().equals(name)).findFirst().orElse(null);
            try {
                snapContainer.setName(name);
                snapContainer.setTag(name);
                snapContainer.setApplicationPath(path);
                snapContainer.getApplications().forEach(a -> {
                    if (a.getPath() == null) {
                        a.setPath("gpt");
                    }
                    if (isWin && !a.getPath().endsWith(".exe")) {
                        a.setPath(a.getPath() + ".exe");
                    }
                });
                snapContainer = persistenceManager.updateContainer(snapContainer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // UNCOMMENT BELOW TO IMPORT AVAILABLE SNAP OPERATORS DIRECTLY FROM GPT (may take some time)
        /*
        Path rootPath = Paths.get(ConfigurationManager.getInstance().getValue("product.location"));
        for (Application app : snapContainer.getApplications()) {
            List<String> args = new ArrayList<>();
            args.add(app.getPath());
            args.add(app.getName());
            args.add("-h");
            Executor executor = ProcessExecutor.create(ExecutorType.PROCESS, "localhost", args, true);
            OutputAccumulator accumulator = new OutputAccumulator();
            executor.setOutputConsumer(accumulator);
            try {
                int ret = executor.execute(true);
                if (ret == 0) {
                    String output = accumulator.getOutput();
                    String xml = output.substring(output.indexOf("Graph XML Format:") + 18);
                    ProcessingComponent component = DescriptorConverter.fromXml(xml);
                    component.setAuthors("SNAP Team");
                    component.setCopyright("(C) SNAP Team");
                    component.setFileLocation("gpt");
                    component.setWorkingDirectory(".");
                    component.setNodeAffinity("Any");
                    component.setVisibility(ProcessingComponentVisibility.SYSTEM);
                    component.getTargets().get(0).getDataDescriptor()
                            .setLocation(rootPath.resolve(component.getTargets().get(0).getDataDescriptor().getLocation()).toUri().toString());
                    component.setTemplateType(TemplateType.VELOCITY);
                    component.setTemplate(SNAPDemo.newTemplate(component.getId() + ".vm", component.getLabel(), component, "="));
                    component.setActive(true);
                    persistenceManager.saveProcessingComponent(component);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }*/
        return snapContainer;
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
