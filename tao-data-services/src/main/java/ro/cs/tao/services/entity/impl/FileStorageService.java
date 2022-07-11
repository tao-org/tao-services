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

import org.springframework.core.io.FileSystemResource;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.eodata.AuxiliaryData;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.eodata.VectorData;
import ro.cs.tao.eodata.enums.Visibility;
import ro.cs.tao.persistence.AuxiliaryDataProvider;
import ro.cs.tao.persistence.EOProductProvider;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.VectorDataProvider;
import ro.cs.tao.quota.QuotaException;
import ro.cs.tao.quota.UserQuotaManager;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.model.FileObject;
import ro.cs.tao.services.storage.BaseStorageService;
import ro.cs.tao.utils.FileUtilities;
import ro.cs.tao.workspaces.Repository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileStorageService extends BaseStorageService<MultipartFile, FileSystemResource> {

    private static final String PROTOCOL = "file";
    private static final Set<String> SIMPLE_PRODUCT_EXTENSIONS = new HashSet<String>() {{
       add(".tif"); add(".nc"); add(".hd5");
    }};
    private EOProductProvider productProvider;
    private VectorDataProvider vectorDataProvider;
    private AuxiliaryDataProvider auxiliaryDataProvider;

    public FileStorageService() { super(); }

    public void setProductProvider(EOProductProvider productProvider) {
        this.productProvider = productProvider;
    }

    public void setVectorDataProvider(VectorDataProvider vectorDataProvider) {
        this.vectorDataProvider = vectorDataProvider;
    }

    public void setAuxiliaryDataProvider(AuxiliaryDataProvider auxiliaryDataProvider) {
        this.auxiliaryDataProvider = auxiliaryDataProvider;
    }

    @Override
    public boolean isIntendedFor(String protocol) {
        return PROTOCOL.equalsIgnoreCase(protocol);
    }

    @Override
    public Path createFolder(String folderRelativePath, boolean userOnly) throws IOException {
        String cleanPath = StringUtils.cleanPath(folderRelativePath);
        return Files.createDirectories(Paths.get(repository().resolve(cleanPath)));
    }

    @Override
    public void storeUserFile(MultipartFile file, String relativeFolder, String description) throws Exception {
        checkParameters(file, relativeFolder);
        storeFile(file, Paths.get(repository().resolve(relativeFolder)),
                  description, SessionStore.currentContext().getPrincipal());
        
        // Update user processing quota
        UserQuotaManager.getInstance().updateUserProcessingQuota(SessionStore.currentContext().getPrincipal());
    }

    @Override
    public void storeFile(InputStream stream, long length,String relativeFolder, String description) throws Exception {
        storeFile(stream, Paths.get(repository().resolve(relativeFolder)));
        // Update user processing quota
        UserQuotaManager.getInstance().updateUserProcessingQuota(SessionStore.currentContext().getPrincipal());
    }

    @Override
    public boolean exists(String path) throws Exception {
        return Files.exists(Paths.get(repository().resolve(path)));
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
        Path filePath = Paths.get(repository().resolve(fileName));
        if (fileName.startsWith(SystemVariable.USER_FILES.value())) {
            //Path filePath = uploadPath.getParent().resolve(fileName);
            Files.delete(filePath);
            try {
                auxiliaryDataProvider.delete(SessionStore.currentContext().getWorkspace().relativize(filePath).toString());
            } catch (Exception e) {
                Logger.getLogger(FileStorageService.class.getName()).warning(String.format("File %s was not found in database", fileName));
            }
        } else { // it has to be a product folder
            //Path filePath = SessionStore.currentContext().getWorkspace().resolve(fileName);
            List<EOProduct> products = productProvider.getByLocation(filePath.toUri().toString());
            if (products == null || products.size() == 0) {
                List<VectorData> vectorProducts = vectorDataProvider.getByLocation(filePath.toUri().toString());
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
                        vectorDataProvider.delete(product);
                    } catch (PersistenceException e) {
                        throw new IOException(String.format("Failed to remove vector product %s. Reason: %s",
                                                            product.getName(), e.getMessage()));
                    }
                } else {
                    if (Files.isDirectory(filePath)) {
                        FileUtilities.deleteTree(filePath);
                    } else {
                        Files.delete(filePath);
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
                    productProvider.delete(product);
                } catch (PersistenceException e) {
                    throw new IOException(String.format("Failed to remove raster product %s. Reason: %s",
                                                        product.getName(), e.getMessage()));
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
    public void move(String source, String destination) throws  IOException {
        if (source == null || source.isEmpty() || destination == null || destination.isEmpty()) {
            throw new IOException("Invalid argument (empty)");
        }
        if (destination.contains("..")) {
            // This is a security check
            throw new IOException( "Cannot move file with relative path outside user directory " + source);
        }
        Repository repository = repository();
        Path sourcePath = Paths.get(repository.resolve(source));
        Path destinationPath = Paths.get(repository.resolve(destination));
        if (destination.startsWith("files")) {
            FileUtilities.move(sourcePath, destinationPath);
            try {
                Path workspaceCurrentUser = SessionStore.currentContext().getWorkspace();
                AuxiliaryData auxiliaryData = auxiliaryDataProvider.getByLocation(sourcePath.toUri().toString());
                if (auxiliaryData != null) {
                    auxiliaryData.setLocation(workspaceCurrentUser.resolve(destinationPath).toUri().toString());
                    auxiliaryDataProvider.update(auxiliaryData);
                }
            } catch (Exception e) {
                Logger.getLogger(FileStorageService.class.getName()).warning(String.format("File %s was not found in database", source));
            }
        } else { //it's a product
            String destinationURI = destinationPath.toUri().toString();
            List<EOProduct> products = productProvider.getByLocation(sourcePath.toUri().toString());

            if (products == null || products.size() == 0) { // we might have vector products
                List<VectorData> vectorProducts = vectorDataProvider.getByLocation(sourcePath.toUri().toString());
                if (vectorProducts.size() > 1) {
                    throw new IOException("Cannot move vector products");
                }
                if (vectorProducts.size() == 1) {
                    VectorData product = vectorProducts.get(0);
                    if (Visibility.PUBLIC.equals(product.getVisibility())) {
                        throw new IOException("Cannot move a public product. Please reduce its visibility first.");
                    }
                    FileUtilities.move(sourcePath, destinationPath);
                    try {
                        product.setLocation(destinationURI);
                        vectorDataProvider.update(product);
                    } catch (PersistenceException | URISyntaxException e) {
                        throw new IOException(String.format("Failed to move vector product %s. Reason: %s",
                                product.getName(), e.getMessage()));
                    }
                }
            } else { // we might have raster products
                /*if (products.size() > 1) {
                    throw new IOException("Cannot move raster products");
                }*/
                EOProduct product = products.get(0);
                /*if (Visibility.PUBLIC.equals(product.getVisibility())) {
                    throw new IOException("Cannot move a public product. Please reduce its visibility first.");
                }*/
                FileUtilities.move(sourcePath, destinationPath);
                try {
                    product.setLocation(destinationURI);
                    productProvider.update(product);
                } catch (PersistenceException | URISyntaxException e) {
                    throw new IOException(String.format("Failed to move vector product %s. Reason: %s",
                            product.getName(), e.getMessage()));
                }
            }
            //TODO: no need to update the user quota, right?
        }
    }

    @Override
    public List<FileObject> listUserWorkspace() throws IOException {
        return listFiles("/", null, null, 1);
    }

    @Override
    public List<FileObject> listTree(String fromPath) throws IOException {
        return listFiles(fromPath, null, null, 10);
    }

    @Override
    public List<FileObject> listFiles(String fromPath, Set<String> exclusions, String lastItem, int depth) throws IOException {
        Repository repository = repository();
        final Path workspaceRoot = Paths.get(repository.root());
        final Path root = Paths.get(repository.resolve(fromPath));
        //final int depth = 10;//root.getNameCount() - workspaceRoot.getNameCount() == 1 ? 1 : 10;
        final List<Path> list = list(root, depth);
        if (exclusions != null) {
            ListIterator<Path> iterator = list.listIterator();
            int nc;
            while (iterator.hasNext()) {
                Path current = iterator.next();
                for (String exc : exclusions) {
                    Path excPath = Paths.get(exc);
                    nc = excPath.getNameCount();
                    if (current.getNameCount() < nc) {
                        continue;
                    }
                    if (current.startsWith(exc)) {
                        iterator.remove();
                    }
                }
            }
        }
        list.removeIf(p -> p.endsWith(".aux.xml"));
        final String[] pathArray = list.stream().map(p -> p.toUri().toString()).toArray(String[]::new);
        final Map<Path, Map<String, String>> auxAttributes = new HashMap<>();
        final List<AuxiliaryData> auxData = auxiliaryDataProvider.list(repository.getUserName(), pathArray);
        for (AuxiliaryData auxiliaryData : auxData) {
            auxAttributes.put(Paths.get(URI.create(auxiliaryData.getLocation())),
                              auxiliaryData.toAttributeMap());
        }
        final List<EOProduct> productList = productProvider.getByLocation(pathArray);
        final Map<Path, Map<String, String>> productAttributes = new HashMap<>();
        for (EOProduct product : productList) {
            String entryPoint = product.getEntryPoint();
            if (entryPoint != null) {
                int idx = entryPoint.lastIndexOf('.');
                if (idx > 0 && SIMPLE_PRODUCT_EXTENSIONS.contains(entryPoint.substring(idx))) {
                    productAttributes.put(Paths.get(URI.create(product.getLocation()).resolve(entryPoint)),
                                          product.toAttributeMap());
                } else {
                    productAttributes.put(Paths.get(URI.create(product.getLocation())),
                                          product.toAttributeMap());
                }
            } else {
                productAttributes.put(Paths.get(URI.create(product.getLocation())),
                                      product.toAttributeMap());
            }
        }
        final List<VectorData> vectors = vectorDataProvider.getByLocation(pathArray);
        final Map<Path, Map<String, String>> vectorAttributes = new HashMap<>();
        for (VectorData vectorData : vectors) {
            vectorAttributes.put(Paths.get(URI.create(vectorData.getLocation())),
                                 vectorData.toAttributeMap());
        }
        final List<FileObject> fileObjects = new ArrayList<>();
        if (repository.isRoot(fromPath)) {
            fileObjects.add(repositoryRootNode(repository));
        }
        final int filesNameIndex = root.getNameCount() + 1;
        long size;
        Map<String, String> currentAttributeMap = null;
        for (Path realPath : list) {
            //Path realPath = realRoot.resolve(path);
            try {
                size = Files.size(realPath);
            } catch (IOException e) {
                size = -1;
            }
            final boolean isInFiles = realPath.getNameCount() > filesNameIndex && realPath.getName(filesNameIndex).toString().equals("files");
            final Path path = workspaceRoot.relativize(realPath);
            String pathToRecord = path.toString().replace("\\", "/");
            if (Files.isDirectory(realPath)) {
                pathToRecord += "/";
            }
            final FileObject fileObject = new FileObject(PROTOCOL, pathToRecord, Files.isDirectory(realPath), size);
            if (isInFiles) {
                if (auxAttributes.containsKey(realPath)) {
                    fileObject.setAttributes(auxAttributes.get(realPath));
                }
            } else {
                final Path absPath = realPath.toAbsolutePath();
                if (productAttributes.containsKey(absPath)) {
                    currentAttributeMap = productAttributes.get(absPath);
                    fileObject.setProductName(currentAttributeMap.get("name"));
                    currentAttributeMap.remove("name");
                    //currentAttributeMap.remove("formatType");
                    currentAttributeMap.remove("width");
                    currentAttributeMap.remove("height");
                    currentAttributeMap.remove("pixelType");
                    currentAttributeMap.remove("sensorType");
                    currentAttributeMap.remove("size");
                    fileObject.setAttributes(currentAttributeMap);
                } else if (productAttributes.containsKey(absPath.getParent()) && currentAttributeMap != null) {
                    fileObject.setAttributes(currentAttributeMap);
                } else if (vectorAttributes.containsKey(absPath)) {
                    fileObject.setAttributes(vectorAttributes.get(absPath));
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

    @Override
    public FileSystemResource download(String path) throws IOException {
        FileSystemResource resource = new FileSystemResource(path);
        if (resource.exists() || resource.isReadable()) {
            return resource;
        } else {
            throw new IOException("Could not read file: " + path);
        }
    }

    @Override
    public void streamToZip(String rootPath, ZipOutputStream stream) throws IOException {
        Path root = Paths.get(rootPath);
        if (Files.exists(root) && Files.isDirectory(root)) {
        List<Path> paths = FileUtilities.listTree(root);
            for (Path current : paths) {
                if (Files.isRegularFile(current)) {
                    String zipPath = root.relativize(current).toString();
                    ZipEntry entry = new ZipEntry(zipPath);
                    entry.setSize(Files.size(current));
                    entry.setTime(System.currentTimeMillis());
                    stream.putNextEntry(entry);
                    StreamUtils.copy(Files.newInputStream(current), stream);
                    stream.closeEntry();
                } else {
                    if (!current.equals(root)) {
                        String zipPath = root.relativize(current) + File.separator;
                        ZipEntry entry = new ZipEntry(zipPath);
                        stream.putNextEntry(entry);
                        stream.closeEntry();
                    }
                }
            }
        } else {
            throw new IOException("Not a folder");
        }
    }

    private List<FileObject> getResults(Long workflowId, Long jobId) {
        if ((workflowId == null) == (jobId == null)) {
            throw new IllegalArgumentException("Exactly one of [workflowId] or [jobId] should be passed");
        }
        final List<FileObject> results = new ArrayList<>();
        final List<EOProduct> products = workflowId != null ?
                productProvider.getWorkflowOutputs(workflowId) : productProvider.getJobOutputs(jobId);
        final int tokensToSkip = Paths.get(SystemVariable.ROOT.value()).getNameCount() + 1;
        for (EOProduct product : products) {
            results.add(fromEOProduct(product, tokensToSkip));
        }
        return results;
    }

    private FileObject fromEOProduct(EOProduct product, int tokensToSkip) {
        final Path productPath = Paths.get(URI.create(product.getLocation()));
        Path relativePath = productPath.getName(tokensToSkip);
        for (int i = 0; i < productPath.getNameCount(); i++) {
            if (i > tokensToSkip) {
                relativePath = relativePath.resolve(productPath.getName(i));
            }
        }
        final Map<String, String> attributeMap = product.toAttributeMap();
        final FileObject fileObject = new FileObject(PROTOCOL, relativePath.toString(),
                                                     Files.isDirectory(productPath),
                                                     Long.parseLong(attributeMap.get("size")));
        fileObject.setProductName(product.getName());
        attributeMap.remove("name");
        attributeMap.remove("formatType");
        attributeMap.remove("width");
        attributeMap.remove("height");
        attributeMap.remove("pixelType");
        attributeMap.remove("sensorType");
        attributeMap.remove("size");
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

    private void storeFile(MultipartFile file, Path uploadPath, String description, Principal principal) throws IOException, PersistenceException {
        if (file == null || file.getOriginalFilename() == null) {
            return;
        }
        Path filePath;
        try (InputStream stream = wrapStream(file.getInputStream())) {
            Files.createDirectories(uploadPath);
            // Resolve filename when coming from IE
            filePath = uploadPath.resolve(Paths.get(file.getOriginalFilename()).getFileName());
            Files.copy(stream, filePath, StandardCopyOption.REPLACE_EXISTING);
        }
        String userName = principal.getName();
        String location = filePath.toUri().toString(); //workspacePath.relativize(filePath).toString();
        List<AuxiliaryData> listData = auxiliaryDataProvider.list(userName, location);
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
        auxiliaryDataProvider.save(data);
    }

    private void storeFile(InputStream stream, Path uploadPath) throws IOException {
        if (stream == null) {
            return;
        }
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        // Resolve filename when coming from IE
        try (InputStream inputStream = wrapStream(stream)) {
            Files.copy(inputStream, uploadPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private List<Path> list(Path path, int depth) throws IOException {
        try (Stream<Path> stream = Files.walk(path, depth)) {
            return stream.filter(p -> !p.equals(path) && !p.toString().endsWith(".png") && !p.toString().endsWith(".png.aux.xml"))
                         .collect(Collectors.toList());
        }
    }
}
