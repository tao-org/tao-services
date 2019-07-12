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
import ro.cs.tao.eodata.AuxiliaryData;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.eodata.VectorData;
import ro.cs.tao.eodata.enums.Visibility;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.quota.QuotaException;
import ro.cs.tao.quota.UserQuotaManager;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.security.SystemPrincipal;
import ro.cs.tao.security.SystemSessionContext;
import ro.cs.tao.services.interfaces.StorageService;
import ro.cs.tao.services.model.FileObject;
import ro.cs.tao.utils.FileUtilities;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service("storageService")
public class FileStorageService implements StorageService<MultipartFile> {

    @Autowired
    private PersistenceManager persistenceManager;

    public FileStorageService() { }

    @Override
    public Path createFolder(String folderRelativePath, boolean userOnly) throws IOException {
        String cleanPath = StringUtils.cleanPath(folderRelativePath);
        Path basePath = userOnly ?
                SessionStore.currentContext().getUploadPath() : Paths.get(SystemVariable.SHARED_FILES.value());
        Path newFolder = basePath.resolve(cleanPath);
        return Files.createDirectories(newFolder);
    }

    @Override
    public void storeUserFile(MultipartFile file, String relativeFolder, String description) throws Exception {
        checkParameters(file, relativeFolder);
        storeFile(file, SessionStore.currentContext().getUploadPath(),
                  SessionStore.currentContext().getWorkspace(),
                  relativeFolder, description, SessionStore.currentContext().getPrincipal());
        
        // Update user processing quota
        UserQuotaManager.getInstance().updateUserProcessingQuota(SessionStore.currentContext().getPrincipal());
    }

    @Override
    public void storePublicFile(MultipartFile file, String relativeFolder, String description) throws Exception {
        checkParameters(file, relativeFolder);
        storeFile(file, Paths.get(SystemVariable.SHARED_FILES.value()),
                  Paths.get(SystemVariable.SHARED_WORKSPACE.value()),
                  relativeFolder, description, SystemSessionContext.instance().getPrincipal());
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
                    FileUtilities.deleteTree(filePath);
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
                FileUtilities.deleteTree(filePath);
                try {
                    persistenceManager.remove(product);
                } catch (PersistenceException e) {
                    throw new IOException(e);
                }
            }
        }
        
        try {
			// update user's processing quota
			UserQuotaManager.getInstance().updateUserProcessingQuota(SessionStore.currentContext().getPrincipal());
		} catch (QuotaException e) {
			throw new IOException(e);
		}
    }

    @Override
    public List<FileObject> listPublicWorkspace() throws IOException {
        final List<FileObject> files = new ArrayList<>();
        final String name = SystemPrincipal.instance().getName();
        files.addAll(listProducts(name));
        files.addAll(listUploaded(name));
        return files;
    }

    @Override
    public List<FileObject> listUserWorkspace(String userName) throws IOException {
        final List<FileObject> files = new ArrayList<>();
        files.addAll(listProducts(userName));
        files.addAll(listUploaded(userName));
        return files;
    }

    @Override
    public List<FileObject> listUploaded() throws IOException {
        return listUploaded(SystemPrincipal.instance().getName());
    }

    @Override
    public List<FileObject> listUploaded(String principalName) throws IOException {
        final Path realRoot = SystemSessionContext.instance().getPrincipal().getName().equals(principalName) ?
                Paths.get(SystemVariable.SHARED_FILES.value()) :
                SessionStore.currentContext().getUploadPath();
        return listFiles(realRoot, null);
    }

    @Override
    public List<FileObject> listFiles(Path realRoot, Set<Path> exclusions) throws IOException {
        final Path root = Paths.get(SystemVariable.ROOT.value()).toAbsolutePath();
        realRoot = realRoot.toAbsolutePath();
        final Path startPath = root.relativize(realRoot);
        final String principalName = realRoot.getName(root.getNameCount()).toString();
        final int depth = realRoot.getNameCount() - root.getNameCount() == 1 ? 1 : 10;
        List<Path> list = list(realRoot, depth);
        if (exclusions != null) {
            ListIterator<Path> iterator = list.listIterator();
            int nc;
            while (iterator.hasNext()) {
                Path current = iterator.next();
                for (Path exc : exclusions) {
                    nc = exc.getNameCount();
                    if (current.getNameCount() < nc) {
                        continue;
                    }
                    if (current.startsWith(exc)) {
                        iterator.remove();
                    }
                }
            }
        }
        String[] pathArray = list.stream().map(p -> p.toUri().toString()).toArray(String[]::new);
        final Map<Path, Map<String, String>> auxAttributes = new HashMap<>();
        List<AuxiliaryData> auxData = persistenceManager.getAuxiliaryData(principalName,
                                                                          pathArray);
        for (AuxiliaryData auxiliaryData : auxData) {
            auxAttributes.put(Paths.get(URI.create(auxiliaryData.getLocation())),
                              auxiliaryData.toAttributeMap());
        }
        final List<EOProduct> productList = persistenceManager.getEOProducts(pathArray);
        final Map<Path, Map<String, String>> productAttributes = new HashMap<>();
        for (EOProduct product : productList) {
            productAttributes.put(Paths.get(URI.create(product.getLocation())),
                                  product.toAttributeMap());
        }
        List<VectorData> vectors = persistenceManager.getVectorDataProducts(pathArray);
        final Map<Path, Map<String, String>> vectorAttributes = new HashMap<>();
        for (VectorData vectorData : vectors) {
            vectorAttributes.put(Paths.get(URI.create(vectorData.getLocation())),
                                 vectorData.toAttributeMap());
        }
        List<FileObject> fileObjects = new ArrayList<>(list.size());
        final int filesNameIndex = root.getNameCount() + 1;
        long size;
        Map<String, String> currentAttributeMap = null;
        for (Path realPath : list) {
            final FileObject fileObject;
            //Path realPath = realRoot.resolve(path);
            try {
                size = Files.size(realPath);
            } catch (IOException e) {
                size = -1;
            }
            boolean isInFiles = realPath.getNameCount() > filesNameIndex && realPath.getName(filesNameIndex).toString().equals("files");
            final Path path = realRoot.relativize(realPath);
            final String pathToRecord = (isInFiles ?
                    realPath.subpath(filesNameIndex, realPath.getNameCount()).toString() :
                    startPath.resolve(path).toString()).replace("\\", "/");
            if (isInFiles) {
                if (auxAttributes.containsKey(realPath)) {
                    fileObject = new FileObject(pathToRecord, Files.isDirectory(realPath), size);
                    fileObject.setAttributes(auxAttributes.get(realPath));
                } else  {
                    fileObject = new FileObject(pathToRecord, Files.isDirectory(realPath), size);
                }
            } else {
                if (productAttributes.containsKey(realPath)) {
                    fileObject = new FileObject(pathToRecord, Files.isDirectory(realPath), 0);
                    currentAttributeMap = productAttributes.get(realPath);
                    fileObject.setProductName(currentAttributeMap.get("name"));
                    currentAttributeMap.remove("name");
                    //currentAttributeMap.remove("formatType");
                    currentAttributeMap.remove("width");
                    currentAttributeMap.remove("height");
                    currentAttributeMap.remove("pixelType");
                    currentAttributeMap.remove("sensorType");
                    fileObject.setAttributes(currentAttributeMap);
                } else if (productAttributes.containsKey(realPath.getParent()) && currentAttributeMap != null) {
                    fileObject = new FileObject(pathToRecord, Files.isDirectory(realPath), size);
                    fileObject.setAttributes(currentAttributeMap);
                } else if (vectorAttributes.containsKey(realPath)) {
                    fileObject = new FileObject(pathToRecord, Files.isDirectory(realPath), size);
                    fileObject.setAttributes(vectorAttributes.get(realPath));
                } else {
                    fileObject = new FileObject(pathToRecord, Files.isDirectory(realPath), size);
                }
            }
            fileObjects.add(fileObject);
        }
        return fileObjects;
    }

    @Override
    public List<FileObject> getJobResults(long jobId) throws IOException {
        return getResults(null, jobId);
    }

    @Override
    public List<FileObject> getWorkflowResults(long workflowId) throws IOException {
        return getResults(workflowId, null);
    }

    private List<FileObject> listProducts(String principalName) throws IOException {
        boolean isSystem = SystemSessionContext.instance().getPrincipal().getName().equals(principalName);
        final Path realRoot = isSystem ? Paths.get(SystemVariable.SHARED_WORKSPACE.value()) : SessionStore.currentContext().getWorkspace();
        Set<Path> exclusions = new HashSet<>();
        exclusions.add(realRoot.resolve("files").toAbsolutePath());
        List<FileObject> list = listFiles(realRoot, exclusions);
        if (isSystem) {
            // add products published by other users (for system account, it means all other users)
            List<EOProduct> products = persistenceManager.getOtherPublishedProducts(principalName);
            final String publicPath = Paths.get(SystemVariable.SHARED_WORKSPACE.value()).toUri().toString();
            final int tokensToSkip = realRoot.getNameCount();
            if (products != null) {
                products.removeIf(p -> p.getLocation().startsWith(publicPath));
                for (EOProduct product : products) {
                    list.add(fromEOProduct(product, tokensToSkip));
                }
            }
        }
        return list;
    }

    private List<FileObject> getResults(Long workflowId, Long jobId) throws IOException {
        if ((workflowId == null) == (jobId == null)) {
            throw new IllegalArgumentException("Exactly one of [workflowId] or [jobId] should be passed");
        }
        List<FileObject> results = new ArrayList<>();
        /*List<FileObject> list = listProducts(SessionStore.currentContext().getPrincipal().getName());
        if (list.size() > 0) {
            List<String> outputKeys = workflowId != null ? persistenceManager.getJobsOutputKeys(workflowId) :
                                                           persistenceManager.getJobOutputKeys(jobId);
            if (outputKeys != null && outputKeys.size() > 0) {
                Set<String> keys = new LinkedHashSet<>(outputKeys);
                for (FileObject fileObject: list) {
                    String stringPath = fileObject.getRelativePath();
                    if (stringPath.indexOf('-') > 0 && stringPath.indexOf('-', stringPath.indexOf('-') + 1) > 0 &&
                            keys.contains(stringPath.substring(0, stringPath.indexOf('-', stringPath.indexOf('-', 0) + 1)))) {
                        results.add(fileObject);
                    }
                }
            }
        }*/
        List<EOProduct> products = workflowId != null ?
                persistenceManager.getWorkflowOutputs(workflowId) : persistenceManager.getJobOutputs(jobId);
        int tokensToSkip = Paths.get(SystemVariable.ROOT.value()).getNameCount() + 1;
        for (EOProduct product : products) {
            results.add(fromEOProduct(product, tokensToSkip));
        }
        return results;
    }

    private FileObject fromEOProduct(EOProduct product, int tokensToSkip) {
        Path productPath = Paths.get(URI.create(product.getLocation()));
        Path relativePath = productPath.getName(tokensToSkip);
        for (int i = 0; i < productPath.getNameCount(); i++) {
            if (i > tokensToSkip) {
                relativePath = relativePath.resolve(productPath.getName(i));
            }
        }
        FileObject fileObject = new FileObject(relativePath.toString(), Files.isDirectory(productPath), 0);
        Map<String, String> attributeMap = product.toAttributeMap();
        fileObject.setProductName(product.getName());
        attributeMap.remove("name");
        attributeMap.remove("formatType");
        attributeMap.remove("width");
        attributeMap.remove("height");
        attributeMap.remove("pixelType");
        attributeMap.remove("sensorType");
        fileObject.setAttributes(attributeMap);
        return fileObject;
    }

    private void checkParameters(MultipartFile file, String relativeFolder) throws IOException {
        if (file == null) {
            throw new NullPointerException("[file]");
        }
        if (file.isEmpty()) {
            throw new IOException("Failed to store empty file " + file.getOriginalFilename());
        }
        if (relativeFolder.contains("..")) {
            // This is a security check
            throw new IOException( "Cannot store file with relative path outside user directory [" + relativeFolder + "]");
        }
    }

    private void storeFile(MultipartFile file, Path uploadPath, Path workspacePath,
                           String relativeFolder, String description, Principal principal) throws IOException, PersistenceException {
        if (file == null || file.getOriginalFilename() == null) {
            return;
        }
        Path filePath;
        try (InputStream inputStream = file.getInputStream()) {
            Path userPath = uploadPath.resolve(relativeFolder);
            Files.createDirectories(userPath);
            // Resolve filename when coming from IE
            filePath = userPath.resolve(Paths.get(file.getOriginalFilename()).getFileName());
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        }
        String userName = principal.getName();
        String location = filePath.toUri().toString(); //workspacePath.relativize(filePath).toString();
        List<AuxiliaryData> listData = persistenceManager.getAuxiliaryData(userName, location);
        AuxiliaryData data;
        if (listData != null && listData.size() == 1) {
            data = listData.get(0);
        } else {
            data = new AuxiliaryData();
            data.setId(UUID.randomUUID().toString());
        }
        data.setLocation(location);
        data.setDescription(description);
        data.setUserName(userName);
        data.setCreated(LocalDateTime.now());
        data.setModified(data.getCreated());
        persistenceManager.saveAuxiliaryData(data);
    }

    private List<Path> list(Path path, int depth) throws IOException {
        return Files.walk(path, depth)
                    .filter(p -> !p.toString().endsWith(".png") && !p.toString().endsWith(".png.aux.xml"))
                    //.map(path::relativize)
                    .collect(Collectors.toList());
    }
}
