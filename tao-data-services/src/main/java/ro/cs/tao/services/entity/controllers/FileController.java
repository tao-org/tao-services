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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.eodata.AuxiliaryData;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.eodata.VectorData;
import ro.cs.tao.eodata.enums.Visibility;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.FileObject;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.StorageService;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/files")
public class FileController extends BaseController {

    @Autowired
    private StorageService<MultipartFile> storageService;

    @Autowired
    private PersistenceManager persistenceManager;

    @GetMapping("/user/uploaded")
    public ResponseEntity<ServiceResponse<?>> listFiles() {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            List<Path> list = storageService.listFiles(true).collect(Collectors.toList());
            List<FileObject> fileObjects = new ArrayList<>(list.size());
            long size;
            Path realRoot = Paths.get(SystemVariable.USER_WORKSPACE.value());
            for (Path path : list) {
                Path realPath = realRoot.resolve(path);
                try {
                    size = Files.size(realPath);
                } catch (IOException e) {
                    size = -1;
                }
                fileObjects.add(new FileObject(path.toString(), Files.isDirectory(realPath), size));
            }
            responseEntity = prepareResult(fileObjects);
        } catch (IOException ex) {
            responseEntity = handleException(ex);
        }

        return responseEntity;
    }

    @GetMapping("/user/")
    public ResponseEntity<ServiceResponse<?>> list() {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            List<Path> list = storageService.listWorkspace(true).collect(Collectors.toList());
            //list.removeIf(p -> p.endsWith(".png") && list.contains(Paths.get(p.toString().replace(".png", ""))));
            List<FileObject> fileObjects = new ArrayList<>(list.size());
            if (list.size() > 0) {
                long size;
                Path realRoot = Paths.get(SystemVariable.USER_WORKSPACE.value());
                String[] strings = list.stream().map(p -> realRoot.resolve(p).toUri().toString()).toArray(String[]::new);
                List<EOProduct> rasters = persistenceManager.getEOProducts(strings);
                List<VectorData> vectors = persistenceManager.getVectorDataProducts(strings);
                List<AuxiliaryData> auxData = persistenceManager.getAuxiliaryData(SessionStore.currentContext().getPrincipal().getName(),
                                                                                  list.stream().map(Path::toString).toArray(String[]::new));
                for (Path path : list) {
                    Path realPath = realRoot.resolve(path);
                    String realUri = realPath.toUri().toString();
                    try {
                        size = Files.size(realPath);
                    } catch (IOException e) {
                        size = -1;
                    }
                    FileObject fileObject = new FileObject(path.toString(), Files.isDirectory(realPath), size);
                    Optional<EOProduct> product = rasters.stream()
                                                         .filter(r -> realUri.equals(r.getLocation()))
                                                         .findFirst();
                    if (product.isPresent()) {
                        Map<String, String> attributeMap = product.get().toAttributeMap();
                        attributeMap.remove("formatType");
                        attributeMap.remove("width");
                        attributeMap.remove("height");
                        attributeMap.remove("pixelType");
                        attributeMap.remove("sensorType");
                        fileObject.setAttributes(attributeMap);
                    } else {
                        product = rasters.stream()
                                .filter(r -> realUri.equals(r.getLocation() + r.getEntryPoint()))
                                .findFirst();
                    }
                    if (product.isPresent() && !fileObject.isFolder()) {
                        Map<String, String> attributeMap = product.get().toAttributeMap();
                        fileObject.setAttributes(attributeMap);
                    } else {
                        Optional<VectorData> vector = vectors.stream()
                                                        .filter(v -> realUri.equals(v.getLocation() + v.getLocation()))
                                                        .findFirst();
                        if (vector.isPresent()) {
                            fileObject.setAttributes(vector.get().toAttributeMap());
                        } else {
                            Optional<AuxiliaryData> aData = auxData.stream()
                                                        .filter(a -> path.toString().equals(a.getLocation()))
                                                        .findFirst();
                            aData.ifPresent(auxiliaryData -> fileObject.setAttributes(auxiliaryData.toAttributeMap()));
                        }
                    }
                    fileObjects.add(fileObject);
                }
            }
            responseEntity = prepareResult(fileObjects);
        } catch (IOException ex) {
            responseEntity = handleException(ex);
        }

        return responseEntity;
    }

    @GetMapping("/user/output")
    public ResponseEntity<ServiceResponse<?>> listOutputs(@RequestParam("workflowId") long workflowId) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            List<Path> list = storageService.listWorkspace(true).collect(Collectors.toList());
            List<FileObject> fileObjects = new ArrayList<>(list.size());
            if (list.size() > 0) {
                long size;
                Path realRoot = Paths.get(SystemVariable.USER_WORKSPACE.value());
                String[] strings = list.stream().map(p -> realRoot.resolve(p).toUri().toString()).toArray(String[]::new);
                List<String> outputKeys = persistenceManager.getJobsOutputKeys(workflowId);
                if (outputKeys != null && outputKeys.size() > 0) {
                    Set<String> keys = new LinkedHashSet<>(outputKeys);
                    List<EOProduct> rasters = persistenceManager.getEOProducts(strings);
                    List<VectorData> vectors = persistenceManager.getVectorDataProducts(strings);
                    List<AuxiliaryData> auxData = persistenceManager.getAuxiliaryData(SessionStore.currentContext().getPrincipal().getName(),
                                                                                      list.stream().map(Path::toString).toArray(String[]::new));
                    for (Path path : list) {
                        String stringPath = path.toString();
                        if (stringPath.indexOf('-') > 0 && stringPath.indexOf('-', stringPath.indexOf('-') + 1) > 0 &&
                                keys.contains(stringPath.substring(0, stringPath.indexOf('-', stringPath.indexOf('-', 0) + 1)))) {
                            Path realPath = realRoot.resolve(path);
                            String realUri = realPath.toUri().toString();
                            try {
                                size = Files.size(realPath);
                            } catch (IOException e) {
                                size = -1;
                            }
                            FileObject fileObject = new FileObject(stringPath, Files.isDirectory(realPath), size);
                            Optional<EOProduct> product = rasters.stream()
                                    .filter(r -> realUri.equals(r.getLocation()))
                                    .findFirst();
                            if (product.isPresent()) {
                                Map<String, String> attributeMap = product.get().toAttributeMap();
                                attributeMap.remove("formatType");
                                attributeMap.remove("width");
                                attributeMap.remove("height");
                                attributeMap.remove("pixelType");
                                attributeMap.remove("sensorType");
                                fileObject.setAttributes(attributeMap);
                            } else {
                                product = rasters.stream()
                                        .filter(r -> realUri.equals(r.getLocation() + r.getEntryPoint()))
                                        .findFirst();
                            }
                            if (product.isPresent() && !fileObject.isFolder()) {
                                Map<String, String> attributeMap = product.get().toAttributeMap();
                                fileObject.setAttributes(attributeMap);
                            } else {
                                Optional<VectorData> vector = vectors.stream()
                                        .filter(v -> realUri.equals(v.getLocation() + v.getLocation()))
                                        .findFirst();
                                if (vector.isPresent()) {
                                    fileObject.setAttributes(vector.get().toAttributeMap());
                                } else {
                                    Optional<AuxiliaryData> aData = auxData.stream()
                                            .filter(a -> stringPath.equals(a.getLocation()))
                                            .findFirst();
                                    aData.ifPresent(auxiliaryData -> fileObject.setAttributes(auxiliaryData.toAttributeMap()));
                                }
                            }
                            fileObjects.add(fileObject);
                        }
                    }
                }
            }
            responseEntity = prepareResult(fileObjects);
        } catch (IOException ex) {
            responseEntity = handleException(ex);
        }

        return responseEntity;
    }

    @GetMapping("/public/uploaded")
    public ResponseEntity<?> listPublicFiles() {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            List<Path> list = storageService.listWorkspace(false).collect(Collectors.toList());
            List<FileObject> fileObjects = new ArrayList<>(list.size());
            long size;
            Path realRoot = Paths.get(SystemVariable.SHARED_FILES.value());
            for (Path path : list) {
                Path realPath = realRoot.resolve(path);
                try {
                    size = Files.size(realPath);
                } catch (IOException e) {
                    size = -1;
                }
                fileObjects.add(new FileObject(path.toString(), Files.isDirectory(realPath), size));
            }
            responseEntity = prepareResult(fileObjects);
        } catch (IOException ex) {
            responseEntity = handleException(ex);
        }

        return responseEntity;
    }

    @GetMapping("/public/")
    public ResponseEntity<ServiceResponse<?>> listAllPublic() {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            List<Path> list = storageService.listWorkspace(false).collect(Collectors.toList());
            List<FileObject> fileObjects = new ArrayList<>(list.size());
            long size;
            Path realRoot = Paths.get(SystemVariable.SHARED_WORKSPACE.value());
            for (Path path : list) {
                if (path.startsWith("files")) {
                    Path realPath = realRoot.resolve(path);
                    try {
                        size = Files.size(realPath);
                    } catch (IOException e) {
                        size = -1;
                    }
                    fileObjects.add(new FileObject(path.toString(), Files.isDirectory(realPath), size));
                }
            }
            List<EOProduct> publicProducts = persistenceManager.getPublicProducts();
            Path root = Paths.get(ConfigurationManager.getInstance().getValue("product.location"));
            for (EOProduct product : publicProducts) {
                String location = product.getLocation();
                if (location != null) {
                    Path productLocation = Paths.get(URI.create(location));
                    Path userPath = Paths.get(productLocation.toString().replace(root.toString(), "")).getName(0);
                    realRoot = root.resolve(userPath).resolve(product.getName());
                    if (!realRoot.getFileName().equals(productLocation.getFileName())) {
                        realRoot = root.resolve(userPath).resolve(productLocation.getFileName());
                    }
                    FileObject fileObject = new FileObject(root.relativize(realRoot).toString(), Files.isDirectory(realRoot), 0);
                    Map<String, String> attributeMap = product.toAttributeMap();
                    attributeMap.remove("formatType");
                    attributeMap.remove("width");
                    attributeMap.remove("height");
                    attributeMap.remove("pixelType");
                    attributeMap.remove("sensorType");
                    fileObject.setAttributes(attributeMap);
                    fileObjects.add(fileObject);
                    String productFolder = root.relativize(realRoot).toString();
                    list = storageService.listFiles(productFolder).collect(Collectors.toList());
                    for (Path path : list) {
                        if (!path.toString().isEmpty()) {
                            Path realPath = realRoot.resolve(path);
                            try {
                                size = Files.size(realPath);
                            } catch (IOException e) {
                                size = -1;
                            }
                            FileObject fo = new FileObject(root.relativize(realPath).toString(), Files.isDirectory(realPath), size);
                            fo.setAttributes(product.toAttributeMap());
                            fileObjects.add(fo);
                        }
                    }
                }
            }

            responseEntity = prepareResult(fileObjects);
        } catch (IOException ex) {
            responseEntity = handleException(ex);
        }

        return responseEntity;
    }

    @PostMapping("/")
    @ResponseBody
    public ResponseEntity<ServiceResponse<?>> toggleVisibility(@RequestParam("folder") String folder,
                                              @RequestParam("visibility") Visibility visibility) {
        ResponseEntity<ServiceResponse<?>> responseEntity = null;
        try {
            Path path = Paths.get(SystemVariable.USER_WORKSPACE.value(), folder);
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

    @GetMapping("/")
    @ResponseBody
    public ResponseEntity<?> download(@RequestParam("fileName") String fileName) {
        ResponseEntity<?> responseEntity;
        try {
            Resource file = loadAsResource(fileName);
            responseEntity = ResponseEntity.ok()
                                           .header(HttpHeaders.CONTENT_DISPOSITION,
                                                   "attachment; filename=\"" + file.getFilename() + "\"")
                                           .body(file);
        } catch (IOException ex) {
            responseEntity = handleException(ex);
        }
        return responseEntity;
    }

    @GetMapping("/preview")
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

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam("desc") String description) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            storageService.store(file, description);
            responseEntity = prepareResult("Upload succeeded", ResponseStatus.SUCCEEDED);
        } catch (Exception ex) {
            responseEntity = handleException(ex);
        }
        return responseEntity;
    }

    @DeleteMapping("/")
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
        Path file = fileName.startsWith("public") ?
                Paths.get(SystemVariable.SHARED_WORKSPACE.value(), fileName) :
                Paths.get(SystemVariable.USER_WORKSPACE.value(), fileName);
        if (!Files.exists(file)) {
            // maybe it is a file published by another user
            file = Paths.get(ConfigurationManager.getInstance().getValue("product.location"), fileName);
        }
        Resource resource = new UrlResource(file.toUri());
        if (resource.exists() || resource.isReadable()) {
            return resource;
        } else {
            throw new IOException("Could not read file: " + fileName);
        }
    }
}
