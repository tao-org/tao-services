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

import com.google.common.net.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.eodata.enums.Visibility;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.StorageService;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Controller
@RequestMapping("/files")
public class FileController extends BaseController {

    @Autowired
    private StorageService<MultipartFile> storageService;

    @Autowired
    private PersistenceManager persistenceManager;

    @RequestMapping(value = "/config", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getConfiguration(@RequestParam("filter") String filter) {
        if (filter == null || filter.trim().isEmpty()) {
            return handleException(new IllegalAccessException("[filter] Value required"));
        }
        final Map<String, String> springSettings = new TreeMap<>();
        final Map<String, String> all = ConfigurationManager.getInstance().getAll();
        for (Map.Entry<String, String> entry : all.entrySet()) {
            if (entry.getKey().startsWith(filter)) {
                springSettings.put(entry.getKey(), entry.getValue());
            }
        }
        return prepareResult(springSettings);
    }

    @RequestMapping(value = "/user/uploaded/", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> listFiles() {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            responseEntity = prepareResult(storageService.listUploaded(currentUser()));
        } catch (IOException ex) {
            responseEntity = handleException(ex);
        }

        return responseEntity;
    }

    @RequestMapping(value = "/user/", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> list() {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            responseEntity = prepareResult(storageService.listUserWorkspace(currentUser()));
        } catch (IOException ex) {
            responseEntity = handleException(ex);
        }

        return responseEntity;
    }

    @RequestMapping(value = "/user", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> list(@RequestParam("folder") String relativeFolder) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            if (relativeFolder == null || relativeFolder.trim().isEmpty() || relativeFolder.equals(".")) {
                responseEntity = prepareResult(storageService.listUserWorkspace(currentUser()));
            } else {
                Path relativePath = Paths.get(relativeFolder);
                if (relativePath.getName(0).toString().equals(currentUser())) {
                    relativeFolder = relativePath.getNameCount() > 1 ? relativePath.subpath(1, relativePath.getNameCount()).toString() : "";
                }
                responseEntity = prepareResult(storageService.listFiles(SessionStore.currentContext().getWorkspace().resolve(relativeFolder), null));
            }
        } catch (IOException ex) {
            responseEntity = handleException(ex);
        }

        return responseEntity;
    }

    @RequestMapping(value = "/user/output", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> listOutputs(@RequestParam("workflowId") long workflowId) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            responseEntity = prepareResult(storageService.getWorkflowResults(workflowId));
        } catch (IOException ex) {
            responseEntity = handleException(ex);
        }

        return responseEntity;
    }

    @RequestMapping(value = "/public/uploaded/", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> listPublicFiles() {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            responseEntity = prepareResult(storageService.listUploaded());
        } catch (IOException ex) {
            responseEntity = handleException(ex);
        }

        return responseEntity;
    }

    @RequestMapping(value = "/public/", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> listPublic() {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            responseEntity = prepareResult(storageService.listPublicWorkspace());
        } catch (IOException ex) {
            responseEntity = handleException(ex);
        }

        return responseEntity;
    }

    @RequestMapping(value = "/public", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> listPublic(@RequestParam("folder") String relativeFolder) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            if (relativeFolder == null || relativeFolder.trim().isEmpty() || relativeFolder.equals(".")) {
                responseEntity = prepareResult(storageService.listPublicWorkspace());
            } else {
                if (relativeFolder.startsWith("public")) {
                    relativeFolder = relativeFolder.substring(relativeFolder.indexOf('/') + 1);
                }
                responseEntity = prepareResult(storageService.listFiles(Paths.get(SystemVariable.SHARED_WORKSPACE.value(), relativeFolder), null));
            }
        } catch (IOException ex) {
            responseEntity = handleException(ex);
        }

        return responseEntity;
    }

    @RequestMapping(value = "/", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public ResponseEntity<ServiceResponse<?>> toggleVisibility(@RequestParam("folder") String folder,
                                                               @RequestParam("visibility") Visibility visibility) {
        ResponseEntity<ServiceResponse<?>> responseEntity = null;
        try {
            Path path = Paths.get(SystemVariable.USER_WORKSPACE.value()).getParent().resolve(folder);
            if (Files.isDirectory(path)) {
                List<EOProduct> eoProducts = persistenceManager.getEOProducts(path.toUri().toString());
                for (EOProduct eoProduct : eoProducts) {
                    eoProduct.setVisibility(visibility);
                    try {
                        persistenceManager.saveEOProduct(eoProduct);
                    } catch (Exception e) {
                        String message = String.format("Cannot update product %s. Reason: %s",
                                                       eoProduct.getName(), e.getMessage());
                        responseEntity = prepareResult(message, ResponseStatus.FAILED);
                        warn(message);
                    }
                }
                if (responseEntity == null) {
                    responseEntity = prepareResult(folder + " visibility changed", ResponseStatus.SUCCEEDED);
                }
            }
        } catch (Exception ex) {
            responseEntity = handleException(ex);
        }
        return responseEntity;
    }

    @RequestMapping(value = "/download", method = RequestMethod.GET, produces = { "application/octet-stream", "application/json" } )
    public @ResponseBody ResponseEntity<?> download(@RequestParam("fileName") String fileName) {
        ResponseEntity<?> responseEntity;
        try {
            Resource file = loadAsResource(fileName);
            responseEntity =  ResponseEntity.ok()
                                .contentLength(file.contentLength())
                                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getFilename())
                                .body(file);
        } catch (IOException ex) {
            responseEntity = handleException(ex);
        }
        return responseEntity;
    }

    @RequestMapping(value = "/preview", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> downloadPreview(@RequestParam("fileName") String fileName) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            Resource file = loadAsResource(fileName + ".png");
            String result = Base64.getEncoder().encodeToString(Files.readAllBytes(file.getFile().toPath()));
            responseEntity = prepareResult(result);
        } catch (IOException ex) {
            responseEntity = handleException(ex);
        }
        return responseEntity;
    }

    @RequestMapping(value = "/user/folder", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<?> createUserFolder(@RequestParam("folder") String folder) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            storageService.createFolder(folder, true);
            responseEntity = prepareResult(String.format("Folder %s created", folder), ResponseStatus.SUCCEEDED);
        } catch (Exception ex) {
            responseEntity = handleException(ex);
        }
        return responseEntity;
    }

    @RequestMapping(value = "/user/upload", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<?> uploadUser(@RequestParam("file") MultipartFile file,
                                        @RequestParam("folder") String folder,
                                        @RequestParam("desc") String description) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            storageService.storeUserFile(file, folder, description);
            responseEntity = prepareResult("Upload succeeded", ResponseStatus.SUCCEEDED);
        } catch (Exception ex) {
            responseEntity = handleException(ex);
        }
        return responseEntity;
    }

    @RequestMapping(value = "/public/folder", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<?> createPublicFolder(@RequestParam("folder") String folder) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            storageService.createFolder(folder, false);
            responseEntity = prepareResult(String.format("Folder %s created", folder), ResponseStatus.SUCCEEDED);
        } catch (Exception ex) {
            responseEntity = handleException(ex);
        }
        return responseEntity;
    }

    @RequestMapping(value = "/public/upload", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<?> uploadPublic(@RequestParam("file") MultipartFile file,
                                          @RequestParam("folder") String folder,
                                          @RequestParam("desc") String description) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            if (!isCurrentUserAdmin()) {
                throw new AccessDeniedException("The operation is permitted only for administrators");
            }
            storageService.storePublicFile(file, folder, description);
            responseEntity = prepareResult("Upload succeeded", ResponseStatus.SUCCEEDED);
        } catch (Exception ex) {
            responseEntity = handleException(ex);
        }
        return responseEntity;
    }

    @RequestMapping(value = "/", method = RequestMethod.DELETE, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> delete(@RequestParam("fileName") String fileName) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            storageService.remove(fileName);
            responseEntity = prepareResult("Delete succeeded", ResponseStatus.SUCCEEDED);
        } catch (IOException ex) {
            responseEntity = handleException(ex);
        }
        return responseEntity;
    }

    private Resource loadAsResource(String fileName) throws IOException {
        if (fileName == null || fileName.isEmpty()) {
            throw new IOException("[fileName] cannot be null or empty");
        }
        final Path filePath = Paths.get(SystemVariable.ROOT.value(), fileName);
        if (!Files.exists(filePath)) {
            throw new IOException(String.format("File '%s' does not exist", filePath));
        }
        Resource resource = new UrlResource(filePath.toUri());
        if (resource.exists() || resource.isReadable()) {
            return resource;
        } else {
            throw new IOException("Could not read file: " + fileName);
        }
    }
}
