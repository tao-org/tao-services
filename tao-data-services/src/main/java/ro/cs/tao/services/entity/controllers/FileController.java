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

package ro.cs.tao.services.entity.controllers;

import com.google.common.net.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.eodata.VectorData;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.FileObject;
import ro.cs.tao.services.commons.ServiceError;
import ro.cs.tao.services.interfaces.StorageService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/files")
public class FileController extends BaseController {

    @Autowired
    private StorageService<MultipartFile> storageService;

    @Autowired
    private PersistenceManager persistenceManager;

    @GetMapping("/user/uploaded")
    public ResponseEntity<?> listFiles() {
        ResponseEntity<?> responseEntity;
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
            responseEntity = new ResponseEntity<>(fileObjects, HttpStatus.OK);
        } catch (IOException ex) {
            responseEntity = handleException(ex);
        }

        return responseEntity;
    }

    @GetMapping("/user/")
    public ResponseEntity<?> list() {
        ResponseEntity<?> responseEntity;
        try {
            List<Path> list = storageService.listWorkspace(true).collect(Collectors.toList());
            List<FileObject> fileObjects = new ArrayList<>(list.size());
            if (list.size() > 0) {
                long size;
                Path realRoot = Paths.get(SystemVariable.USER_WORKSPACE.value());
                String[] strings = list.stream().map(p -> realRoot.resolve(p).toUri().toString()).toArray(String[]::new);
                List<EOProduct> rasters = persistenceManager.getEOProducts(strings);
                List<VectorData> vectors = persistenceManager.getVectorDataProducts(strings);
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
                                                         .filter(r -> realUri.equals(r.getLocation() + r.getEntryPoint()))
                                                         .findFirst();
                    if (product.isPresent()) {
                        fileObject.setAttributes(product.get().toAttributeMap());
                    } else {
                        Optional<VectorData> vector = vectors.stream()
                                                        .filter(v -> realUri.equals(v.getLocation() + v.getLocation()))
                                                        .findFirst();
                        vector.ifPresent(vectorData -> fileObject.setAttributes(vectorData.toAttributeMap()));
                    }
                    fileObjects.add(fileObject);
                }
            }
            responseEntity = new ResponseEntity<>(fileObjects, HttpStatus.OK);
        } catch (IOException ex) {
            responseEntity = handleException(ex);
        }

        return responseEntity;
    }

    @GetMapping("/public/uploaded")
    public ResponseEntity<?> listPublicFiles() {
        ResponseEntity<?> responseEntity;
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
            responseEntity = new ResponseEntity<>(fileObjects, HttpStatus.OK);
        } catch (IOException ex) {
            responseEntity = handleException(ex);
        }

        return responseEntity;
    }

    @GetMapping("/public/")
    public ResponseEntity<?> listAllPublic() {
        ResponseEntity<?> responseEntity;
        try {
            List<Path> list = storageService.listWorkspace(false).collect(Collectors.toList());
            List<FileObject> fileObjects = new ArrayList<>(list.size());
            long size;
            Path realRoot = Paths.get(SystemVariable.SHARED_WORKSPACE.value());
            for (Path path : list) {
                Path realPath = realRoot.resolve(path);
                try {
                    size = Files.size(realPath);
                } catch (IOException e) {
                    size = -1;
                }
                fileObjects.add(new FileObject(path.toString(), Files.isDirectory(realPath), size));
            }
            responseEntity = new ResponseEntity<>(fileObjects, HttpStatus.OK);
        } catch (IOException ex) {
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

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        ResponseEntity<?> responseEntity;
        try {
            storageService.store(file);
            responseEntity = new ResponseEntity<>("Upload succeeded", HttpStatus.OK);
        } catch (IOException ex) {
            responseEntity = handleException(ex);
        }
        return responseEntity;
    }

    @DeleteMapping("/{fileName:.+}")
    public ResponseEntity<?> delete(@PathVariable("fileName") String fileName) {
        ResponseEntity<?> responseEntity;
        try {
            storageService.remove(fileName);
            responseEntity = new ResponseEntity<>("Upload succeeded", HttpStatus.OK);
        } catch (IOException ex) {
            responseEntity = handleException(ex);
        }
        return responseEntity;
    }

    private ResponseEntity<?> handleException(IOException ex) {
        return  new ResponseEntity<>(new ServiceError(String.format("Failed with error: %s",
                                                                    ex.getMessage())),
                                     HttpStatus.OK);
    }

    private Resource loadAsResource(String fileName) throws IOException {
        if (fileName == null || fileName.isEmpty()) {
            throw new IOException("[fileName] cannot be null or empty");
        }
        Path file = fileName.startsWith("public") ?
                Paths.get(SystemVariable.SHARED_WORKSPACE.value(), fileName) :
                Paths.get(SystemVariable.USER_WORKSPACE.value(), fileName);
        Resource resource = new UrlResource(file.toUri());
        if (resource.exists() || resource.isReadable()) {
            return resource;
        } else {
            throw new IOException("Could not read file: " + fileName);
        }
    }
}
