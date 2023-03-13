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
package ro.cs.tao.services.entity.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ro.cs.tao.SortDirection;
import ro.cs.tao.component.*;
import ro.cs.tao.component.enums.ProcessingComponentType;
import ro.cs.tao.component.enums.ProcessingComponentVisibility;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.docker.Application;
import ro.cs.tao.docker.Container;
import ro.cs.tao.docker.ContainerType;
import ro.cs.tao.eodata.AuxiliaryData;
import ro.cs.tao.messaging.Messaging;
import ro.cs.tao.messaging.Topic;
import ro.cs.tao.persistence.AuxiliaryDataProvider;
import ro.cs.tao.persistence.ContainerProvider;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.ProcessingComponentProvider;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.security.SystemPrincipal;
import ro.cs.tao.serialization.JsonMapper;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.entity.beans.ContainerRequest;
import ro.cs.tao.services.entity.beans.ContainerUploadRequest;
import ro.cs.tao.services.interfaces.ContainerService;
import ro.cs.tao.topology.docker.DockerManager;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Groups all API operations related to Docker containers
 *
 * @author Cosmin Cara
 */
@RestController
@RequestMapping("/docker")
@Tag(name = "Containers", description = "Operations related to docker containers")
public class ContainerController extends DataEntityController<Container, String, ContainerService> {

    @Autowired
    private AuxiliaryDataProvider auxiliaryDataProvider;
    @Autowired
    private ContainerProvider containerProvider;
    @Autowired
    private ProcessingComponentProvider processingComponentProvider;

    /**
     * Lists the docker images available on the system.
     *
     * @param pageNumber    (optional) The page number of the result set
     * @param pageSize      (optiona) The page size
     * @param sortByField   (optional) The name of the field used for sorting
     * @param sortDirection (optional) The sort direction
     */
    @Override
    @RequestMapping(value = "/", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> list(@RequestParam(name = "pageNumber", required = false) Optional<Integer> pageNumber,
                                                   @RequestParam(name = "pageSize", required = false)Optional<Integer> pageSize,
                                                   @RequestParam(name = "sortByField", required = false)Optional<String> sortByField,
                                                   @RequestParam(name = "sortDirection", required = false)Optional<SortDirection> sortDirection) {
        List<Container> objects = DockerManager.getAvailableDockerImages();
        if (objects == null) {
            objects = new ArrayList<>();
        }
        return prepareResult(objects);
    }

    /**
     * Lists the formats defined for a container.
     *
     * @param containerId   The container identifier
     */
    @RequestMapping(value = "/formats", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getFormats(@RequestParam("containerId") String containerId) {
        try {
            Container container = service.findById(containerId);
            if (container == null) {
                return prepareResult(String.format("Container [%s] does not exist", containerId), ResponseStatus.FAILED);
            } else {
                Set<String> formats = container.getFormat();
                return prepareResult(formats != null ? formats : new HashSet<>());
            }
        } catch (Exception pex) {
            return handleException(pex);
        }
    }

    /**
     * Uploads the files necessary to build a new Docker image
     *
     * @param request   The structure containing the necessary information
     */
    @RequestMapping(value = "/upload", method = RequestMethod.POST, consumes = "multipart/form-data", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> upload(HttpServletRequest servletRequest,
                                                     @ModelAttribute ContainerUploadRequest request,
                                                     Model model) {
        try {
            final List<MultipartFile> dockerFiles = request.getDockerFiles();
            if (dockerFiles == null || dockerFiles.isEmpty()) {
                throw new IllegalArgumentException("Empty file list");
            }
            final MultipartFile jsonContainerFile = request.getJsonContainerDescriptor();
            if (jsonContainerFile == null) {
                throw new IllegalArgumentException("Please attach a json file with the container and applications descriptors");
            }
            String json;
            json = new String(jsonContainerFile.getBytes());
            final Container container = JsonMapper.instance().readValue(json, Container.class);

            final MultipartFile logoFile = request.getContainerLogo();
            if (logoFile != null) {
                container.setLogo(Base64.getEncoder().encodeToString(logoFile.getBytes()));
            }

            final MultipartFile jsonComponentsFile = request.getJsonComponentsDescriptor();
            final ProcessingComponent[] components;
            if (jsonComponentsFile != null) {
                json = new String(jsonComponentsFile.getBytes());
                components = JsonMapper.instance().readValue(json, ProcessingComponent[].class);
            } else {
                components = null;
            }

            final MultipartFile dockerFile = dockerFiles.stream().filter(f -> "Dockerfile".equals(f.getOriginalFilename())).findFirst().orElse(null);
            if (dockerFile == null) {
                throw new IllegalArgumentException("Dockerfile not found");
            }
            final String name = request.getName();
            if (name == null) {
                throw new IllegalArgumentException("[name] cannot be null");
            }
            if (!name.equals(container.getName())) {
                container.setName(name);
            }
            if (container.getType() == null) {
                container.setType(ContainerType.DOCKER);
            }
            dockerFiles.remove(dockerFile);
            final Path dockerImagesPath = Paths.get(ConfigurationManager.getInstance().getValue("tao.docker.images"), name.replace(" ", "-"));
            final Path dockerPath = resolveMultiPartFile(dockerFile, dockerImagesPath);
            for (MultipartFile file : dockerFiles) {
                try {
                    Path auxFile = resolveMultiPartFile(file, dockerImagesPath);
                    AuxiliaryData data = new AuxiliaryData();
                    data.setId(UUID.randomUUID().toString());
                    data.setLocation(auxFile.toString());
                    data.setDescription(String.format("Auxiliary file for container '%s'", name));
                    data.setUserName(request.isSystem() ? SystemPrincipal.instance().getName() : currentUser());
                    data.setCreated(LocalDateTime.now());
                    data.setModified(data.getCreated());
                    auxiliaryDataProvider.save(data);
                } catch (IOException e) {
                    error(e.getMessage());
                }
            }
            List<MultipartFile> auxiliaryFiles = request.getAuxiliaryFiles();
            if (auxiliaryFiles != null) {
                Path auxiliaryFilesPath = request.isSystem() ?
                        Paths.get(SystemVariable.SHARED_FILES.value()) :
                        SessionStore.currentContext().getUploadPath();
                for (MultipartFile file : auxiliaryFiles) {
                    try {
                        resolveMultiPartFile(file, auxiliaryFilesPath);
                    } catch (IOException e) {
                        error(e.getMessage());
                    }
                }
            }
            final String description = request.getDescription();
            if (description != null && !description.equals(container.getDescription())) {
                container.setDescription(description);
            }
            asyncExecute(() -> {
                try {
                    service.registerContainer(dockerPath, container, components);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }, "Docker image registration completed", this::exceptionCallbackHandler);
            return prepareResult("Docker image registration started. A notification will be send when the process completes",
                                 ResponseStatus.SUCCEEDED);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * Pulls a Docker image to the local repository
     * @param image     The name of the Docker image
     */
    @RequestMapping(value = "/pull", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> pullFromHub(@RequestParam("image") String image) {
        try {
            return prepareResult(DockerManager.pullImage(image));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * Registers a Docker image with TAO.
     *
     * @param request   The structure containing the necessary information
     */
    @RequestMapping(value = "/register", method = RequestMethod.POST, consumes = "multipart/form-data", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> upload(HttpServletRequest servletRequest,
                                                     @ModelAttribute ContainerRequest request,
                                                     Model model) {
        try {
            final Container container = request.getContainerDescriptor();
            if (container == null) {
                throw new IllegalArgumentException("No container descriptor received");
            }
            final List<MultipartFile> dockerFiles = request.getDockerFiles();
            final boolean noFilePresent = dockerFiles == null || dockerFiles.isEmpty();
            if (noFilePresent && DockerManager.getDockerImage(container.getName()) == null) {
                throw new IllegalArgumentException("Empty file list");
            }
            final String description = request.getDescription();
            if (description != null && !description.equals(container.getDescription())) {
                container.setDescription(description);
            }
            final String name = request.getName();
            if (name == null) {
                throw new IllegalArgumentException("[name] cannot be null");
            }
            if (!name.equals(container.getName())) {
                container.setName(name);
            }
            if (container.getType() == null) {
                container.setType(ContainerType.DOCKER);
            }
            final MultipartFile logoFile = request.getContainerLogo();
            if (logoFile != null) {
                container.setLogo(Base64.getEncoder().encodeToString(logoFile.getBytes()));
            }

            final ProcessingComponent[] components = request.getComponentDescriptors();
            final Runnable registrationCode;

            if (noFilePresent) {
                // The container was pulled from Docker Hub
                registrationCode = () -> {
                    try {
                        initializeContainer(container, components);
                        Messaging.send(currentPrincipal(), Topic.INFORMATION.value(), this,
                                       String.format("Docker image '%s' successfully registered", container.getName()));
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                };
            } else {
                // The container will be built locally
                final MultipartFile dockerFile = dockerFiles.stream().filter(f -> "Dockerfile".equals(f.getOriginalFilename())).findFirst().orElse(null);
                if (dockerFile == null) {
                    throw new IllegalArgumentException("Dockerfile not found");
                }

                dockerFiles.remove(dockerFile);
                final Path dockerImagesPath = Paths.get(ConfigurationManager.getInstance().getValue("tao.docker.images"), name.replace(" ", "-"));
                final Path dockerPath = resolveMultiPartFile(dockerFile, dockerImagesPath);
                for (MultipartFile file : dockerFiles) {
                    try {
                        Path auxFile = resolveMultiPartFile(file, dockerImagesPath);
                        AuxiliaryData data = new AuxiliaryData();
                        data.setId(UUID.randomUUID().toString());
                        data.setLocation(auxFile.toString());
                        data.setDescription(String.format("Auxiliary file for container '%s'", name));
                        data.setUserName(request.isSystem() ? SystemPrincipal.instance().getName() : currentUser());
                        data.setCreated(LocalDateTime.now());
                        data.setModified(data.getCreated());
                        auxiliaryDataProvider.save(data);
                    } catch (IOException e) {
                        error(e.getMessage());
                    }
                }
                List<MultipartFile> auxiliaryFiles = request.getAuxiliaryFiles();
                if (auxiliaryFiles != null) {
                    Path auxiliaryFilesPath = request.isSystem() ?
                                              Paths.get(SystemVariable.SHARED_FILES.value()) :
                                              SessionStore.currentContext().getUploadPath();
                    for (MultipartFile file : auxiliaryFiles) {
                        try {
                            resolveMultiPartFile(file, auxiliaryFilesPath);
                        } catch (IOException e) {
                            error(e.getMessage());
                        }
                    }
                }
                registrationCode = () -> {
                    try {
                        service.registerContainer(dockerPath, container, components);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                };
            }
            asyncExecute(registrationCode, "Docker image registration completed", this::exceptionCallbackHandler);
            return prepareResult("Docker image registration started. A notification will be send when the process completes",
                                 ResponseStatus.SUCCEEDED);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    private Container initializeContainer(Container container, ProcessingComponent[] components) throws PersistenceException {
        container = this.containerProvider.save(container);
        ProcessingComponent current = null;
        final List<Application> containerApplications = container.getApplications();
        for (ProcessingComponent component : components) {
            current = component;
            component.setContainerId(container.getId());
            component.setLabel(component.getId());
            component.setComponentType(ProcessingComponentType.EXECUTABLE);
            containerApplications.stream().filter(a -> a.getName().equals(component.getId())).findFirst()
                                 .ifPresent(application -> component.setFileLocation(application.getPath()));
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
            final List<SourceDescriptor> sources = component.getSources();
            if (sources != null) {
                sources.forEach(s -> s.setId(UUID.randomUUID().toString()));
            }
            final List<TargetDescriptor> targets = component.getTargets();
            if (targets != null) {
                targets.forEach(t -> t.setId(UUID.randomUUID().toString()));
            }
            String template = component.getTemplateContents();
            final int length = template.length();
            int i = 0;
            while (i < length) {
                char ch = template.charAt(i);
                if (ch == '$' && template.charAt(i - 1) != '\n' && template.charAt(i - 1) != '=') {
                    template = template.substring(0, i) + "\n" + template.substring(i);
                }
                i++;
            }
            component.setTemplateContents(template);
            component.setComponentType(ProcessingComponentType.EXECUTABLE);
            component.setVisibility(ProcessingComponentVisibility.SYSTEM);
            component.setOwner(SystemPrincipal.instance().getName());
            component.addTags(container.getName());
            if (this.processingComponentProvider.get(component.getId(), component.getContainerId()) == null) {
                this.processingComponentProvider.save(component);
            } else {
                this.processingComponentProvider.update(component);
            }
        }
        return container;
    }

    private Path resolveMultiPartFile(MultipartFile file, Path targetBase) throws IOException {
        if (file == null) {
            throw new IOException("Failed to store empty file");
        }
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        if (fileName.contains("..")) {
            // This is a security check
            throw new IOException( "Cannot store docker file with relative path outside image directory " + fileName);
        }
        Files.createDirectories(targetBase);
        Path destination = targetBase.resolve(fileName);
        file.transferTo(destination.toAbsolutePath().toFile());
        return destination;
    }
}
