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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.persistence.RepositoryProvider;
import ro.cs.tao.quota.QuotaException;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.factory.StorageServiceFactory;
import ro.cs.tao.services.interfaces.ProductService;
import ro.cs.tao.services.interfaces.StorageService;
import ro.cs.tao.workspaces.Repository;
import ro.cs.tao.workspaces.RepositoryType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

@RestController
@RequestMapping("/product")
public class ProductController extends DataEntityController<EOProduct, String, ProductService> {

    @Autowired
    private RepositoryProvider repositoryProvider;

    /**
     * Retrieve a list of EOProducts given their names
     * @param nameList  The list of product names
     */
    @RequestMapping(value = "", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getByNames(@RequestParam("name") String nameList) {
        if (nameList == null || nameList.isEmpty()) {
            return prepareResult("[name] parameter is empty");
        }
        return prepareResult(service.getByNames(nameList.split(",")));
    }
    /**
     * Inspects a folder from the local workspace for products not present in the database and imports them.
     *
     * @param sourceDir     The source folder
     */
    @RequestMapping(value = "/inspect", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> inspect(@RequestParam("sourceDir") String sourceDir) {
        if (sourceDir == null) {
            return prepareResult("Source directory not found", ResponseStatus.FAILED);
        }
        asyncExecute(() -> {
            try {
                this.service.inspect(getLocalRepository(), sourceDir);
            } catch (Exception e) {
                error("Inspection of products in %s failed. Reason: %s", sourceDir, e.getMessage());
            }
        });
        return prepareResult(String.format("Inspection of %s started", sourceDir), ResponseStatus.SUCCEEDED);
    }
    /**
     * Imports the products from the given source folder into the TAO workspace.
     *
     * @param sourceDir     The source folder
     * @param linkOnly      If <code>true</code>, only symlinks to the original products will be created
     *                      in the workspace. Otherwise, the whole products will be copied locally
     */
    @RequestMapping(value = "/import", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> importProducts(@RequestParam("sourceDir") String sourceDir,
                                                             @RequestParam("linkOnly") boolean linkOnly) {
        if (sourceDir == null) {
            return prepareResult("Source directory not found", ResponseStatus.FAILED);
        }
        try {
            return prepareResult(this.service.importProducts(sourceDir, linkOnly));
        } catch (QuotaException | IOException e) {
            return handleException(e);
        }
    }
    /**
     * Checks if the database contains products with the given names.
     * @param names     The list of product names to check
     */
    @RequestMapping(value = "/check", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> checkExistingProducts(@RequestParam("names") String[] names) {
        return prepareResult(this.service.checkExisting(names));
    }
    /**
     * Returns the preview file of a product.
     *
     * @param fileName  The preview file name
     */
    @RequestMapping(value = "/preview", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> downloadPreview(@RequestParam("fileName") String fileName) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            FileSystemResource file = loadAsResource(fileName + ".png");
            String result = Base64.getEncoder().encodeToString(Files.readAllBytes(Paths.get(file.getPath())));
            responseEntity = prepareResult(result);
        } catch (IOException ex) {
            responseEntity = handleException(ex);
        }
        return responseEntity;
    }

    private FileSystemResource loadAsResource(String fileName) throws IOException {
        if (fileName == null || fileName.isEmpty()) {
            throw new IOException("[fileName] cannot be null or empty");
        }
        final Path filePath = Paths.get(getLocalRepository().resolve(fileName)).toAbsolutePath();
        if (!Files.exists(filePath)) {
            throw new IOException(String.format("File '%s' does not exist", filePath));
        }
        return getLocalRepositoryService().download(filePath.toString());
    }

    private Repository getLocalRepository() {
        return repositoryProvider.getByUser(currentUser()).stream().filter(w -> w.getType() == RepositoryType.LOCAL).findFirst().get();
    }

    private StorageService<MultipartFile, FileSystemResource> getLocalRepositoryService() {
        return StorageServiceFactory.getInstance(getLocalRepository());
    }
}
