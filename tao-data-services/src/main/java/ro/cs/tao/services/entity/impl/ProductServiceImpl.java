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

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.eodata.enums.Visibility;
import ro.cs.tao.eodata.metadata.DecodeStatus;
import ro.cs.tao.eodata.metadata.MetadataInspector;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.interfaces.ProductService;
import ro.cs.tao.spi.ServiceRegistryManager;
import ro.cs.tao.utils.FileUtilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service("productService")
public class ProductServiceImpl extends EntityService<EOProduct> implements ProductService {

    @Autowired
    private PersistenceManager persistenceManager;

    @Override
    protected void validateFields(EOProduct entity, List<String> errors) {

    }

    @Override
    public EOProduct findById(String id) throws PersistenceException {
        return null;
    }

    @Override
    public List<EOProduct> list() {
        List<EOProduct> products = null;
        try {
            products = persistenceManager.getEOProducts();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return products;
    }

    @Override
    public EOProduct save(EOProduct object) {
        try {
            return persistenceManager.saveEOProduct(object);
        } catch (PersistenceException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public EOProduct update(EOProduct object) throws PersistenceException {
        return null;
    }

    @Override
    public void delete(String id) throws PersistenceException {

    }

    @Override
    public List<EOProduct> inspect(Path sourcePath) throws IOException {
        if (sourcePath == null || !Files.exists(sourcePath)) {
            throw new IOException("Source directory not found");
        }
        Set<MetadataInspector> services = ServiceRegistryManager.getInstance()
                .getServiceRegistry(MetadataInspector.class)
                .getServices();
        MetadataInspector inspector;
        if (services == null) {
            throw new IOException("No product inspector found");
        }
        List<EOProduct> results = new ArrayList<>();
        try {
            List<Path> folders = Files.walk(sourcePath, 1).collect(Collectors.toList());
            Path publicFolder = Paths.get(SystemVariable.SHARED_WORKSPACE.value());
            for (Path folder : folders) {
                try {
                    if (Files.isDirectory(folder) && !folder.equals(sourcePath)) {
                        Path targetPath;
                        if (!folder.toString().startsWith(publicFolder.toString())) {
                            targetPath = publicFolder.resolve(folder.getFileName());
                            if (!Files.exists(targetPath)) {
                                Logger.getLogger(ProductService.class.getName()).fine(String.format("Copying %s to %s",
                                                                                                    folder, publicFolder.toFile()));
                                FileUtils.copyDirectoryToDirectory(folder.toFile(), publicFolder.toFile());
                                FileUtilities.ensurePermissions(targetPath);
                            }
                        } else {
                            targetPath = folder;
                        }
                        inspector = services.stream()
                                .filter(i -> DecodeStatus.INTENDED == i.decodeQualification(targetPath))
                                .findFirst()
                                .orElse(services.stream()
                                                .filter(i -> DecodeStatus.SUITABLE == i.decodeQualification(targetPath))
                                                .findFirst()
                                                .orElse(null));
                        if (inspector == null) {
                            continue;
                        }
                        MetadataInspector.Metadata metadata = inspector.getMetadata(targetPath);
                        if (metadata != null) {
                            EOProduct product = metadata.toProductDescriptor(targetPath);
                            product.setEntryPoint(metadata.getEntryPoint());
                            product.setUserName(SessionStore.currentContext().getPrincipal().getName());
                            product.setVisibility(Visibility.PUBLIC);
                            if (metadata.getAquisitionDate() != null) {
                                product.setAcquisitionDate(Date.from(metadata.getAquisitionDate().atZone(ZoneId.systemDefault()).toInstant()));
                            }
                            if (metadata.getSize() != null) {
                                product.setApproximateSize(metadata.getSize());
                            }
                            if (metadata.getProductId() != null) {
                                product.setId(metadata.getProductId());
                            }
                            results.add(product);
                        }
                    }
                } catch (Exception e1) {
                    Logger.getLogger(ProductService.class.getName()).warning(String.format("Import for %s failed. Reason: %s", folder, e1.getMessage()));
                }
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
        return results;
    }

    @Override
    public int importProducts(List<EOProduct> products) {
        if (products == null) {
            return 0;
        }
        int count = 0;
        for (EOProduct product : products) {
            try {
                persistenceManager.saveEOProduct(product);
                count++;
            } catch (PersistenceException e) {
                e.printStackTrace();
            }
        }
        return count;
    }

    @Override
    public int importProducts(String sourcePath, boolean linkOnly) throws IOException {
        Path srcPath = null;
        if (sourcePath == null || !Files.exists((srcPath = Paths.get(sourcePath)))) {
            throw new IOException("Source directory not found");
        }
        Set<MetadataInspector> services = ServiceRegistryManager.getInstance()
                .getServiceRegistry(MetadataInspector.class)
                .getServices();
        MetadataInspector inspector;
        if (services == null) {
            throw new IOException("No product inspector found");
        }
        int count = 0;
        try {
            List<Path> folders = Files.walk(srcPath, 1).collect(Collectors.toList());
            Path publicFolder = Paths.get(SystemVariable.SHARED_WORKSPACE.value());
            for (Path folder : folders) {
                try {
                    if (Files.isDirectory(folder) && !folder.equals(srcPath)) {
                        Path targetPath;
                        if (!folder.toString().startsWith(publicFolder.toString())) {
                            targetPath = publicFolder.resolve(folder.getFileName());
                            if (!Files.exists(targetPath)) {
                                Logger.getLogger(ProductService.class.getName()).fine(String.format("Copying %s to %s",
                                                                                                    folder, publicFolder.toFile()));
                                if (linkOnly) {
                                    FileUtilities.link(folder, targetPath);
                                } else {
                                    FileUtils.copyDirectoryToDirectory(folder.toFile(), publicFolder.toFile());
                                    FileUtilities.ensurePermissions(targetPath);
                                }
                            }
                        } else {
                            targetPath = folder;
                        }
                        inspector = services.stream()
                                .filter(i -> DecodeStatus.INTENDED == i.decodeQualification(targetPath))
                                .findFirst()
                                .orElse(services.stream()
                                                .filter(i -> DecodeStatus.SUITABLE == i.decodeQualification(targetPath))
                                                .findFirst()
                                                .orElse(null));
                        if (inspector == null) {
                            continue;
                        }
                        MetadataInspector.Metadata metadata = inspector.getMetadata(targetPath);
                        if (metadata != null) {
                            EOProduct product = metadata.toProductDescriptor(targetPath);
                            product.setEntryPoint(metadata.getEntryPoint());
                            product.setUserName(SessionStore.currentContext().getPrincipal().getName());
                            product.setVisibility(Visibility.PUBLIC);
                            if (metadata.getAquisitionDate() != null) {
                                product.setAcquisitionDate(Date.from(metadata.getAquisitionDate().atZone(ZoneId.systemDefault()).toInstant()));
                            }
                            if (metadata.getSize() != null) {
                                product.setApproximateSize(metadata.getSize());
                            }
                            if (metadata.getProductId() != null) {
                                product.setId(metadata.getProductId());
                            }

                            persistenceManager.saveEOProduct(product);
                            count++;
                        }
                    }
                } catch (Exception e1) {
                    Logger.getLogger(ProductService.class.getName()).warning(String.format("Import for %s failed. Reason: %s", folder, e1.getMessage()));
                }
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
        return count;
    }

    @Override
    public List<String> checkExisting(String... names) {
        return persistenceManager.getExistingProductNames(names);
    }
}
