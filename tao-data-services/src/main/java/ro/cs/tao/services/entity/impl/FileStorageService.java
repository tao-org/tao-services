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
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.eodata.AuxiliaryData;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.interfaces.StorageService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
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
        if (fileName.contains("..")) {
            // This is a security check
            throw new IOException( "Cannot remove file with relative path outside user directory " + fileName);
        }
        Path filePath = SessionStore.currentContext().getUploadPath().resolve(fileName);
        Files.delete(filePath);
        persistenceManager.removeAuxiliaryData(SessionStore.currentContext().getWorkspace().relativize(filePath).toString());
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

    private Stream<Path> list(Path path, int depth) throws IOException {
        return Files.walk(path, depth).map(path::relativize);
    }
}
