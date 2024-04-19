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
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.h2.util.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ro.cs.tao.EnumUtils;
import ro.cs.tao.SortDirection;
import ro.cs.tao.component.*;
import ro.cs.tao.component.enums.ProcessingComponentType;
import ro.cs.tao.component.enums.ProcessingComponentVisibility;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.docker.*;
import ro.cs.tao.eodata.AuxiliaryData;
import ro.cs.tao.messaging.Messaging;
import ro.cs.tao.messaging.Topic;
import ro.cs.tao.persistence.AuxiliaryDataProvider;
import ro.cs.tao.persistence.ContainerInstanceProvider;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.ProcessingComponentProvider;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.security.SystemPrincipal;
import ro.cs.tao.security.Token;
import ro.cs.tao.serialization.JsonMapper;
import ro.cs.tao.services.auth.token.TokenManagementService;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.entity.beans.ContainerRequest;
import ro.cs.tao.services.entity.beans.ContainerUploadRequest;
import ro.cs.tao.services.interfaces.ContainerService;
import ro.cs.tao.spi.ServiceRegistryManager;
import ro.cs.tao.topology.docker.DockerImageInstaller;
import ro.cs.tao.topology.docker.DockerManager;
import ro.cs.tao.topology.docker.SingletonContainer;
import ro.cs.tao.utils.StringUtilities;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
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
    private ContainerInstanceProvider containerInstanceProvider;
    @Autowired
    private ProcessingComponentProvider processingComponentProvider;
    @Autowired
    private TokenManagementService tokenManagementService;

    /**
     * Lists the docker images available on the system.
     *
     * @param pageNumber    (optional) The page number of the result set
     * @param pageSize      (optiona) The page size
     * @param sortByField   (optional) The name of the field used for sorting
     * @param sortDirection (optional) The sort direction
     */
    @Override
    @RequestMapping(value = "/", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> list(@RequestParam(name = "pageNumber", required = false) Optional<Integer> pageNumber,
                                                   @RequestParam(name = "pageSize", required = false)Optional<Integer> pageSize,
                                                   @RequestParam(name = "sortByField", required = false)Optional<String> sortByField,
                                                   @RequestParam(name = "sortDirection", required = false)Optional<SortDirection> sortDirection) {
        //List<Container> objects = DockerManager.getAvailableDockerImages();
        List<Container> objects;
        if (isCurrentUserAdmin()) {
            objects = service.listByType(ContainerType.DOCKER);
        } else {
            objects = service.listContainersVisibleToUser(currentUser());
        }
        if (objects == null) {
            objects = new ArrayList<>();
        }
        return prepareResult(objects);
    }

    /**
     * Lists the docker images with a given visibility.
     *
     * @param typeId    The container type identifier
     * @param visibilityId  The container visibility identifier
     */
    @RequestMapping(value = "/type", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> listByType(@RequestParam("type") int typeId,
                                                         @RequestParam("visibility") int visibilityId) {
        if (!isCurrentUserAdmin()) {
            return prepareResult("Unauthorized", HttpStatus.UNAUTHORIZED);
        }
        try {
            return prepareResult(service.listByTypeAndVisibility(EnumUtils.getEnumConstantByValue(ContainerType.class, typeId),
                                                                 EnumUtils.getEnumConstantByValue(ContainerVisibility.class, visibilityId)));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * Lists the docker images for the current user.
     *
     */
    @RequestMapping(value = "/user", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> listByUser() {
        try {
            return prepareResult(service.listUserContainers(currentUser()));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * Lists the formats defined for a container.
     *
     * @param containerId   The container identifier
     */
    @RequestMapping(value = "/formats", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
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
    @RequestMapping(value = "/upload", method = RequestMethod.POST, consumes = "multipart/form-data", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> upload(HttpServletRequest servletRequest,
                                                     @ModelAttribute ContainerUploadRequest request,
                                                     Model model) {
        if (!isCurrentUserAdmin()) {
            return prepareResult("Unauthorized", HttpStatus.UNAUTHORIZED);
        }
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
            container.setVisibility(ContainerVisibility.PRIVATE);
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
                    data.setUserId(request.isSystem() ? SystemPrincipal.instance().getName() : currentUser());
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
            container.setOwnerId(currentUser());
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
    @RequestMapping(value = "/pull", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
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
    @RequestMapping(value = "/register", method = RequestMethod.POST, consumes = "multipart/form-data", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> upload(HttpServletRequest servletRequest,
                                                     @ModelAttribute ContainerRequest request,
                                                     Model model) {
        try {
            final String containerDescriptor = request.getContainerDescriptor();
            if (containerDescriptor == null) {
                throw new IllegalArgumentException("No container descriptor received");
            }
            final ObjectMapper mapper = new ObjectMapper();
            final Container container = mapper.readerFor(Container.class).readValue(containerDescriptor);
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
            container.setVisibility(ContainerVisibility.UNDEFINED);
            final MultipartFile logoFile = request.getContainerLogo();
            if (logoFile != null) {
                container.setLogo(Base64.getEncoder().encodeToString(logoFile.getBytes()));
            }
            container.setOwnerId(currentUser());
            final String componentDescriptors = request.getComponentDescriptors();
            final ProcessingComponent[] components;
            if (componentDescriptors != null) {
                components = mapper.readerForArrayOf(ProcessingComponent.class).readValue(componentDescriptors);
            } else {
                components = new ProcessingComponent[0];
            }
            final Runnable registrationCode;

            final Principal principal = currentPrincipal();
            if (noFilePresent) {
                // The container was pulled from Docker Hub
                registrationCode = () -> {
                    try {
                        initializeContainer(container, components);
                        Messaging.send(principal, Topic.INFORMATION.value(), this,
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
                        data.setUserId(request.isSystem() ? SystemPrincipal.instance().getName() : currentUser());
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
                        Messaging.send(principal, Topic.INFORMATION.value(), this,
                                       String.format("Docker image '%s' successfully registered", container.getName()));
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

    @RequestMapping(value = "/start", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> startContainer(@RequestParam("id") String imageId) {
        try {
            if (StringUtilities.isNullOrEmpty(imageId)) {
                throw new IllegalArgumentException("[id] null");
            }
            Container container = service.findById(imageId);
            if (container == null) {
                container = service.listContainersVisibleToUser(currentUser())
                                   .stream().filter(c -> c.getName().equals(imageId)).findFirst().orElse(null);
                if (container == null) {
                    throw new IllegalArgumentException(String.format("[%s] not registered", imageId));
                }
            }
            final Set<DockerImageInstaller> services = ServiceRegistryManager.getInstance().getServiceRegistry(DockerImageInstaller.class).getServices();
            final Container finalContainer = container;
            final DockerImageInstaller installer = services.stream()
                                                           .filter(i -> (i instanceof SingletonContainer) &&
                                                                        ((SingletonContainer) i).getContainerName().equals(finalContainer.getName()))
                                                           .findFirst().orElse(null);
            if (installer != null) {
                try {
                    installer.getClass().getMethod("start", String.class, String.class);
                    final String user = currentUser();
                    final Token token = tokenManagementService.getUserToken(user);
                    return prepareResult(((SingletonContainer) installer).start(user, user));//token.getToken()));
                } catch (Exception e) {
                    return prepareResult(((SingletonContainer) installer).start());
                }
            } else {
                final String name = SystemPrincipal.instance().getName();
                return prepareResult(DockerManager.runDaemon(container.getName(), name, null, name, null));
            }
            //return prepareResult("Request to start container [" + imageId + "] sent", ResponseStatus.SUCCEEDED);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/stop", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> stopContainerInstance(@RequestParam("id") String instanceId) {
        try {
            if (StringUtilities.isNullOrEmpty(instanceId)) {
                throw new IllegalArgumentException("[id] null");
            }
            final ContainerInstance instance = containerInstanceProvider.get(instanceId);
            if (instance == null) {
                throw new IllegalArgumentException(String.format("[%s] not found", instanceId));
            }
            if (!currentUser().equals(instance.getUserId()) && !isCurrentUserAdmin()) {
                prepareResult("Not authorized", HttpStatus.FORBIDDEN);
            }
            DockerManager.stopInstance(instance.getContainerId(), instance.getUserId());
            return prepareResult("Container [" + instanceId + "] stopped", ResponseStatus.SUCCEEDED);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/connect", method = RequestMethod.GET)
    public void tunnel(@RequestParam(name = "host", required = false) String host,
                       @RequestParam(name = "port", required = false) Integer port,
                       @RequestParam(name = "query", required = false) String query,
                       HttpServletRequest request, HttpServletResponse response) {
        try {
            HttpUriRequest proxiedRequest = createHttpUriRequest(request,
                                                                 host != null ? host : "127.0.0.1",
                                                                 port != null ? port : 8888,
                                                                 query);
            try (CloseableHttpClient httpClient = HttpClients.createMinimal()) {
                HttpResponse proxiedResponse = httpClient.execute(proxiedRequest);
                writeToResponse(proxiedResponse, response);
            }
        } catch (Exception e) {
            error(e.getMessage());
        }
    }

    @Override
    @RequestMapping(value = "/{id:.+}", method = RequestMethod.DELETE)
    public ResponseEntity<ServiceResponse<?>> delete(@PathVariable("id") String id) {
        try {
            final Container container = service.findById(id);
            if (isCurrentUserAdmin() || currentUser().equals(container.getOwnerId())) {
                return super.delete(id);
            } else {
                return prepareResult("Unauthorized", HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception e) {
            return handleException(e);
        }
    }

    private HttpUriRequest createHttpUriRequest(HttpServletRequest request, String host, int port, String query) throws Exception {
        final URI uri = new URI("http://" + host + ":" + port + (query != null ? "/" + query : ""));
        final RequestBuilder rb = RequestBuilder.create(request.getMethod());
        rb.addHeader("Remote Address", Inet4Address.getLocalHost().getHostAddress());
        rb.setUri(uri);
        final Enumeration<String> headerNames = request.getHeaderNames();
        while(headerNames.hasMoreElements()){
            String headerName = headerNames.nextElement();
            if(!headerName.equalsIgnoreCase("accept-encoding")) {
                rb.addHeader(headerName, request.getHeader(headerName));
            }
        }

        return rb.build();
    }

    private void writeToResponse(HttpResponse proxiedResponse, HttpServletResponse response) throws IOException {
        for (Header header : proxiedResponse.getAllHeaders()) {
            if ((! header.getName().equals("Transfer-Encoding")) || (! header.getValue().equals("chunked"))) {
                response.addHeader(header.getName(), header.getValue());
            }
        }
        try (InputStream is = proxiedResponse.getEntity().getContent();
             OutputStream os = response.getOutputStream()) {
            IOUtils.copy(is, os);
        }
    }

    private Container initializeContainer(Container container, ProcessingComponent[] components) throws PersistenceException {
        container = service.save(container);
        final List<Application> containerApplications = container.getApplications();
        for (ProcessingComponent component : components) {
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
