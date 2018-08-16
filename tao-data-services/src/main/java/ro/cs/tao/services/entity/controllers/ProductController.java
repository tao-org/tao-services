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

import org.apache.commons.io.FileUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.eodata.MetadataInspector;
import ro.cs.tao.eodata.enums.Visibility;
import ro.cs.tao.products.sentinels.Sentinel2ProductHelper;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.ProductService;
import ro.cs.tao.spi.ServiceRegistryManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/product")
public class ProductController extends DataEntityController<EOProduct, ProductService> {

    @RequestMapping(value = "/import", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> importProducts(@RequestParam("sourceDir") String sourceDir) {
        ResponseEntity<ServiceResponse<?>> response;
        Path sourcePath = Paths.get(sourceDir);
        if (Files.exists(sourcePath)) {
            Set<MetadataInspector> services = ServiceRegistryManager.getInstance()
                    .getServiceRegistry(MetadataInspector.class)
                    .getServices();
            MetadataInspector inspector = null;
            if (services != null) {
                inspector = services.stream().findFirst().get();
            }
            if (inspector != null) {
                try {
                    List<Path> folders = Files.walk(sourcePath, 1).collect(Collectors.toList());
                    int count = 0;
                    Path publicFolder = Paths.get(SystemVariable.SHARED_WORKSPACE.value());
                    for (Path folder : folders) {
                        try {
                            if (Files.isDirectory(folder) && !folder.equals(sourcePath)) {
                                Path targetPath;
                                if (!folder.toString().startsWith(publicFolder.toString())) {
                                    targetPath = publicFolder.resolve(folder.getFileName());
                                    if (!Files.exists(targetPath)) {
                                        FileUtils.copyDirectory(folder.toFile(), targetPath.toFile(), true);
                                    }
                                } else {
                                    targetPath = folder;
                                }
                                MetadataInspector.Metadata metadata = null;
                                String metadataFile = null;
                                try {
                                    Sentinel2ProductHelper helper = Sentinel2ProductHelper.createHelper(targetPath.getFileName().toString());
                                    metadataFile = helper.getMetadataFileName();
                                    metadata = inspector.getMetadata(targetPath.resolve(metadataFile));
                                } catch (Exception ignored) {
                                    metadata = inspector.getMetadata(targetPath);
                                }
                                if (metadata != null) {
                                    EOProduct product = metadata.toProductDescriptor(targetPath);
                                    if (metadataFile != null) {
                                        product.setEntryPoint(metadataFile);
                                    }
                                    product.setUserName(SessionStore.currentContext().getPrincipal().getName());
                                    product.setVisibility(Visibility.PUBLIC);
                                    product = service.save(product);
                                    logger.fine("Imported product " + product.getName());
                                    count++;
                                }
                            }
                        } catch (Exception e1) {
                            logger.warning(String.format("Import for %s failed. Reason: %s", folder, e1.getMessage()));
                        }
                    }
                    response = prepareResult("Imported " + count + " products", ResponseStatus.SUCCEEDED);
                } catch (Exception e) {
                    response = handleException(e);
                }
            } else {
                response = prepareResult("No product inspector found", ResponseStatus.FAILED);
            }
        } else {
            response = prepareResult("Source directory not found", ResponseStatus.FAILED);
        }
        return response;
    }

}
