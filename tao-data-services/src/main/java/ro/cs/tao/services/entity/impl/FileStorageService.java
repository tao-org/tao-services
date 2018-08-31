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

package ro.cs.tao.services.entity.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.eodata.AuxiliaryData;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.eodata.VectorData;
import ro.cs.tao.eodata.enums.Visibility;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.interfaces.StorageService;
import ro.cs.tao.utils.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

@Service("storageService")
public class FileStorageService implements StorageService<MultipartFile> {

    @Autowired
    private PersistenceManager persistenceManager;

    public FileStorageService() { }

    @Override
    public void store(MultipartFile file, String description) throws Exception {
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        if (file.isEmpty()) {
            throw new IOException("Failed to store empty file " + fileName);
        }
        if (fileName.contains("..")) {
            // This is a security check
            throw new IOException( "Cannot store file with relative path outside user directory " + fileName);
        }
        Path filePath;
        try (InputStream inputStream = file.getInputStream()) {
            Path userPath = SessionStore.currentContext().getUploadPath();
            Files.createDirectories(userPath);
            filePath = userPath.resolve(fileName);
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        }
        AuxiliaryData data = new AuxiliaryData();
        data.setLocation(SessionStore.currentContext().getWorkspace().relativize(filePath).toString());
        data.setDescription(description);
        data.setUserName(SessionStore.currentContext().getPrincipal().getName());
        data.setCreated(LocalDateTime.now());
        data.setModified(data.getCreated());
        persistenceManager.saveAuxiliaryData(data);
    }

    @Override
    public void remove(String fileName) throws IOException{
        if (fileName == null || fileName.isEmpty()) {
            throw new IOException("Invalid argument (empty)");
        }
        if (fileName.contains("..")) {
            // This is a security check
            throw new IOException( "Cannot remove file with relative path outside user directory " + fileName);
        }
        Path uploadPath = SessionStore.currentContext().getUploadPath();
        if (fileName.equals(uploadPath.toString()) ||
                fileName.startsWith(SystemVariable.SHARED_WORKSPACE.value()) ||
                fileName.startsWith(SystemVariable.SHARED_FILES.value())) {
            throw new IOException( "Operation not allowed");
        }
        if (fileName.startsWith("files")) {
            Path filePath = uploadPath.getParent().resolve(fileName);
            Files.delete(filePath);
            try {
                persistenceManager.removeAuxiliaryData(SessionStore.currentContext().getWorkspace().relativize(filePath).toString());
            } catch (Exception e) {
                Logger.getLogger(FileStorageService.class.getName()).warning(String.format("File %s was not found in database", fileName));
            }
        } else { // it has to be a product folder
            Path filePath = SessionStore.currentContext().getWorkspace().resolve(fileName);
            List<EOProduct> products = persistenceManager.getEOProducts(filePath.toUri().toString());
            if (products == null || products.size() == 0) {
                List<VectorData> vectorProducts = persistenceManager.getVectorDataProducts(filePath.toUri().toString());
                if (vectorProducts.size() > 1) {
                    throw new IOException("Cannot remove vector products");
                }
                if (vectorProducts.size() == 1) {
                    VectorData product = vectorProducts.get(0);
                    if (Visibility.PUBLIC.equals(product.getVisibility())) {
                        throw new IOException("Cannot remove a public product. Please reduce its visibility first.");
                    }
                    FileUtils.deleteTree(filePath.toFile());
                    try {
                        persistenceManager.remove(product);
                    } catch (PersistenceException e) {
                        throw new IOException(e);
                    }
                }
            } else {
                if (products.size() > 1) {
                    throw new IOException("Cannot remove raster products");
                }
                EOProduct product = products.get(0);
                if (Visibility.PUBLIC.equals(product.getVisibility())) {
                    throw new IOException("Cannot remove a public product. Please reduce its visibility first.");
                }
                FileUtils.deleteTree(filePath.toFile());
                try {
                    persistenceManager.remove(product);
                } catch (PersistenceException e) {
                    throw new IOException(e);
                }
            }
        }
    }

    @Override
    public Stream<Path> listFiles(boolean userOnly) throws IOException {
        Path location = Paths.get(userOnly ?
                                          SystemVariable.USER_FILES.value() :
                                          SystemVariable.SHARED_FILES.value());
        if (!Files.exists(location)) {
            Files.createDirectories(location);
        }
        return list(location, 1);
    }

    @Override
    public Stream<Path> listWorkspace(boolean userOnly) throws IOException {
        Path location = Paths.get(userOnly ?
                                          SystemVariable.USER_WORKSPACE.value() :
                                          SystemVariable.SHARED_WORKSPACE.value());
        if (!Files.exists(location)) {
            Files.createDirectories(location);
        }
        return list(location, 3);
    }

    @Override
    public Stream<Path> listFiles(String fromPath) throws IOException {
        Path path = Paths.get(fromPath);
        if (!path.isAbsolute()) {
            path = Paths.get(ConfigurationManager.getInstance().getValue("product.location"), fromPath);
        }
        return list(path, 1);
    }

    private Stream<Path> list(Path path, int depth) throws IOException {
        return Files.walk(path, depth)
                    .filter(p -> !p.toString().endsWith(".png") && !p.toString().endsWith(".png.aux.xml"))
                    .map(path::relativize);
    }
}
