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
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.docker.Container;
import ro.cs.tao.messaging.Message;
import ro.cs.tao.messaging.Messaging;
import ro.cs.tao.messaging.Topics;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.entity.beans.ContainerUploadRequest;
import ro.cs.tao.services.interfaces.ContainerService;
import ro.cs.tao.topology.TopologyManager;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * @author Cosmin Cara
 */
@Controller
@RequestMapping("/docker")
public class ContainerController extends DataEntityController<Container, String, ContainerService> {

    @Override
    @RequestMapping(value = "/", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> list(@RequestParam(name = "pageNumber", required = false) Optional<Integer> pageNumber,
                                                   @RequestParam(name = "pageSize", required = false)Optional<Integer> pageSize,
                                                   @RequestParam(name = "sortByField", required = false)Optional<String> sortByField,
                                                   @RequestParam(name = "sortDirection", required = false)Optional<SortDirection> sortDirection) {
        List<Container> objects = TopologyManager.getInstance().getAvailableDockerImages();
        if (objects == null) {
            objects = new ArrayList<>();
        }
        return prepareResult(objects);
    }

    @RequestMapping(value = "/upload", method = RequestMethod.POST, consumes = "multipart/form-data", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> upload(HttpServletRequest servletRequest,
                                                     @ModelAttribute ContainerUploadRequest request,
                                                     Model model) {
        try {
            final List<MultipartFile> files = request.getDockerFiles();
            if (files == null || files.isEmpty()) {
                throw new IllegalArgumentException("Empty file list");
            }
            final MultipartFile jsonFile = files.stream().filter(f -> f.getOriginalFilename().toLowerCase().endsWith(".json"))
                                                .findFirst().orElse(null);
            if (request.getJsonDescriptor() == null && jsonFile == null) {
                throw new IllegalArgumentException("Please either attach a json file or set the [jsonDescriptor] field");
            }
            final String json;
            if (jsonFile != null) {
                json = new String(jsonFile.getBytes());
            } else {
                json = request.getJsonDescriptor();
            }
            final Container container = new ObjectMapper().readValue(json, Container.class);
            final MultipartFile dockerFile = files.stream().filter(f -> "Dockerfile".equals(f.getOriginalFilename())).findFirst().orElse(null);
            if (dockerFile == null) {
                throw new IllegalArgumentException("Dockerfile not found");
            }
            final String name = request.getName();
            if (name == null) {
                throw new IllegalArgumentException("[name] cannot be null");
            }
            files.remove(dockerFile);
            final Path dockerImagesPath = Paths.get(ConfigurationManager.getInstance().getValue("tao.docker.images"), name.replace(" ", "-"));
            final Path dockerPath = resolveMultiPartFile(dockerFile, dockerImagesPath);
            for (MultipartFile file : files) {
                try {
                    resolveMultiPartFile(file, dockerImagesPath);
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
            asyncExecute(() -> {
                try {
                    service.registerContainer(dockerPath, name,
                                              request.getDescription() == null ? name : request.getDescription(),
                                              container);
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
            topic = Topics.ERROR;
            Logger.getLogger(getClass().getName()).severe(msg);
        } else {
            msg = "Docker image registration completed";
            message.setData(msg);
            topic = Topics.INFORMATION;
            Logger.getLogger(getClass().getName()).info(msg);
        }
        Messaging.send(SessionStore.currentContext().getPrincipal(), topic, message);
    }
}
