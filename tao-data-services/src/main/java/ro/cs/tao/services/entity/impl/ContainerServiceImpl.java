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
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import ro.cs.tao.component.ParameterDescriptor;
import ro.cs.tao.component.ProcessingComponent;
import ro.cs.tao.component.SourceDescriptor;
import ro.cs.tao.component.TargetDescriptor;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.docker.Application;
import ro.cs.tao.docker.Container;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.data.jsonutil.JacksonUtil;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.services.entity.controllers.ContainerController;
import ro.cs.tao.services.interfaces.ContainerService;
import ro.cs.tao.topology.TopologyManager;
import ro.cs.tao.utils.Platform;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
    public void registerContainer(MultipartFile dockerFile, String shortName, String description) throws IOException {
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
            topologyManager.registerImage(filePath, shortName, description);
        }
    }

    @Override
    public Container initializeContainer(String id, String name, String path, List<Application> applications) {
        List<Container> containers = persistenceManager.getContainers();
        boolean isWin = Platform.getCurrentPlatform().getId().equals(Platform.ID.win);
        Container container = null;
        if (containers == null || containers.stream().noneMatch(c -> c.getName().equals(name))) {
            container = new Container();
            container.setId(id);
            container.setName(name);
            container.setTag(name);
            container.setApplicationPath(path);
            String appPath;
            for (Application app : applications) {
                Application application = new Application();
                appPath = app.getPath() + (isWin && (winExtensions.stream()
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
            container = containers.stream().filter(c -> c.getName().equals(name)).findFirst().orElse(null);
            container.setId(id);
            container.setName(name);
            container.setTag(name);
            String appPath;
            for (Application app : applications) {
                Application application = new Application();
                appPath = app.getName() + (isWin && (winExtensions.stream()
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
    public Container initOTB(String id, String name, String path) {
        Container otbContainer = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(ContainerController.class.getResourceAsStream("otb_container.json")))) {
            String str = String.join("", reader.lines().collect(Collectors.toList()));
            Container tmp = JacksonUtil.fromString(str, Container.class);
            List<Application> applications = tmp.getApplications();
            otbContainer = initializeContainer(id, name, path, applications);
            try (InputStream in = ContainerController.class.getResourceAsStream("otb_logo.png")) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                int read;
                byte[] buffer = new byte[1024];
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
                otbContainer.setLogo(Base64.getEncoder().encodeToString(out.toByteArray()));
            }
            ProcessingComponent current = null;
            try (BufferedReader reader2 = new BufferedReader(new InputStreamReader(ContainerController.class.getResourceAsStream("otb_applications.json")))) {
                String str2 = String.join("", reader2.lines().collect(Collectors.toList()));
                ProcessingComponent[] components = JacksonUtil.OBJECT_MAPPER.readValue(str2, ProcessingComponent[].class);
                List<Application> containerApplications = otbContainer.getApplications();
                for (ProcessingComponent component : components) {
                    current = component;
                    component.setContainerId(otbContainer.getId());
                    component.setLabel(component.getId());
                    component.setFileLocation(containerApplications.stream().filter(a -> a.getName().equals(component.getId())).findFirst().get().getPath());
                    List<ParameterDescriptor> parameterDescriptors = component.getParameterDescriptors();
                    if (parameterDescriptors != null) {
                        parameterDescriptors.forEach(p -> {
                            String[] valueSet = p.getValueSet();
                            if (valueSet != null && valueSet.length > 0) {
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
                        Character ch = template.charAt(i);
                        if (ch == '$' && template.charAt(i - 1) != '\n') {
                            template = template.substring(0, i) + "\n" + template.substring(i);
                        }
                        i++;
                    }
                    String[] tokens = template.split("\n");
                    for (int j = 0; j < tokens.length; j++) {
                        final int idx = j;
                        if ((targets != null && targets.stream().anyMatch(t -> t.getName().equals(tokens[idx].substring(1)))) ||
                            (sources != null && sources.stream().anyMatch(s -> s.getName().equals(tokens[idx].substring(1))))) {
                            tokens[j + 1] = tokens[j].replace('-', '$');
                            j++;
                        }
                    }
                    component.setTemplateContents(String.join("\n", tokens));
                    persistenceManager.saveProcessingComponent(component);
                }
            } catch (Exception e) {
                logger.severe(String.format("Faulty component: %s. Error: %s",
                                            current != null ? current.getId() : "n/a",
                                            e.getMessage()));
            }
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
        return otbContainer;
    }

    @Override
    public Container initSNAP(String id, String name, String path) {
        List<Container> containers = persistenceManager.getContainers();
        boolean isWin = Platform.getCurrentPlatform().getId().equals(Platform.ID.win);
        Container snapContainer;
        if (containers == null || containers.stream().noneMatch(c -> c.getName().equals(name))) {
            snapContainer = new Container();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(ContainerController.class.getResourceAsStream("snap_container.json")))) {
                String str = String.join("", reader.lines().collect(Collectors.toList()));
                snapContainer = JacksonUtil.fromString(str, Container.class);
                snapContainer.setId(id);
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
                    a.setParallelFlagTemplate("-q <integer>");
                });
                try (InputStream in = ContainerController.class.getResourceAsStream("snap_logo.png")) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    int read;
                    byte[] buffer = new byte[1024];
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    out.flush();
                    snapContainer.setLogo(Base64.getEncoder().encodeToString(out.toByteArray()));
                }
                snapContainer = persistenceManager.saveContainer(snapContainer);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(ContainerController.class.getResourceAsStream("snap_operators.json")))) {
                String str = String.join("", reader.lines().collect(Collectors.toList()));
                ProcessingComponent[] components = JacksonUtil.OBJECT_MAPPER.readValue(str, ProcessingComponent[].class);
                for (ProcessingComponent component : components) {
                    component.setContainerId(snapContainer.getId());
                    persistenceManager.saveProcessingComponent(component);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            snapContainer = containers.stream().filter(c -> c.getName().equals(name)).findFirst().orElse(null);
            try {
                snapContainer.setId(id);
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
                    a.setParallelFlagTemplate("-q <integer>");
                });
                snapContainer = persistenceManager.updateContainer(snapContainer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
