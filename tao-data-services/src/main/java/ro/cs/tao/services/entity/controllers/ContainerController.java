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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import ro.cs.tao.SortDirection;
import ro.cs.tao.component.ProcessingComponent;
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.configuration.Configuration;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.docker.Container;
import ro.cs.tao.eodata.AuxiliaryData;
import ro.cs.tao.messaging.Message;
import ro.cs.tao.messaging.Messaging;
import ro.cs.tao.messaging.Topic;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.security.SystemPrincipal;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
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
import java.util.logging.Logger;

/**
 * @author Cosmin Cara
 */
@Controller
@RequestMapping("/docker")
public class ContainerController extends DataEntityController<Container, String, ContainerService> {

    @Autowired
    private PersistenceManager persistenceManager;

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
            final Container container = new ObjectMapper().readValue(json, Container.class);

            final MultipartFile logoFile = request.getContainerLogo();
            if (logoFile != null) {
                container.setLogo(Base64.getEncoder().encodeToString(logoFile.getBytes()));
            }

            final MultipartFile jsonComponentsFile = request.getJsonComponentsDescriptor();
            final ProcessingComponent[] components;
            if (jsonComponentsFile != null) {
                json = new String(jsonComponentsFile.getBytes());
                components = new ObjectMapper().readValue(json, ProcessingComponent[].class);
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
            dockerFiles.remove(dockerFile);
            final Path dockerImagesPath = Paths.get(ConfigurationManager.getInstance().getValue(Configuration.Docker.IMAGES_LOCATION), name.replace(" ", "-"));
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
                    persistenceManager.saveAuxiliaryData(data);
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
            }, this::registrationCallback);
            return prepareResult("Docker image registration started. A notification will be send when the process completes",
                                 ResponseStatus.SUCCEEDED);
        } catch (Exception e) {
            return handleException(e);
        }
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

    private void registrationCallback(Exception ex) {
        final Message message = new Message();
        message.setTimestamp(System.currentTimeMillis());
        String msg;
        final String topic;
        if (ex != null) {
            msg = "Docker image registration failed. Reason: " + ex.getMessage();
            message.setData(msg);
            topic = Topic.ERROR.value();
            Logger.getLogger(getClass().getName()).severe(msg);
        } else {
            msg = "Docker image registration completed";
            message.setData(msg);
            topic = Topic.INFORMATION.value();
            Logger.getLogger(getClass().getName()).info(msg);
        }
        Messaging.send(SessionStore.currentContext().getPrincipal(), topic, message);
    }
}
