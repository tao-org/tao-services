package ro.cs.tao.services.entity.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.eodata.enums.Visibility;
import ro.cs.tao.persistence.EOProductProvider;
import ro.cs.tao.quota.QuotaException;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.ProductService;
import ro.cs.tao.services.interfaces.StorageService;
import ro.cs.tao.utils.FileUtilities;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Controller
@RequestMapping("/workspace")
public class WorkspaceController extends DataEntityController<EOProduct, String, ProductService> {

    @Autowired
    private StorageService<MultipartFile> storageService;

    @Autowired
    private EOProductProvider productProvider;

    // product endpoints

    @RequestMapping(value = "/product", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getByNames(@RequestParam("name") String nameList) {
        if (nameList == null || nameList.isEmpty()) {
            return prepareResult("[name] parameter is empty");
        }
        return prepareResult(service.getByNames(nameList.split(",")));
    }

    @RequestMapping(value = "/product/inspect", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> inspect(@RequestParam("sourceDir") String sourceDir) {
        if (sourceDir == null) {
            return prepareResult("Source directory not found", ResponseStatus.FAILED);
        }
        try {
            return prepareResult(this.service.inspect(Paths.get(sourceDir)));
        } catch (IOException e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/product/import", method = RequestMethod.POST, produces = "application/json")
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

    @RequestMapping(value = "/product/check", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> checkExistingProducts(@RequestParam("names") String[] names) {
        return prepareResult(this.service.checkExisting(names));
    }

    // file endpoints

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

    @RequestMapping(value = "/files", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public ResponseEntity<ServiceResponse<?>> toggleVisibility(@RequestParam("folder") String folder,
                                                               @RequestParam("visibility") Visibility visibility) {
        ResponseEntity<ServiceResponse<?>> responseEntity = null;
        try {
            Path path = Paths.get(SystemVariable.USER_WORKSPACE.value()).getParent().resolve(folder);
            if (Files.isDirectory(path)) {
                List<EOProduct> eoProducts = productProvider.getByLocation(path.toUri().toString());
                for (EOProduct eoProduct : eoProducts) {
                    eoProduct.setVisibility(visibility);
                    try {
                        productProvider.save(eoProduct);
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

    @RequestMapping(value = "/downloads", method = RequestMethod.GET, produces = { "application/octet-stream", "application/json" } )
    public void download(@RequestParam("folder") String folder, HttpServletResponse response) {
        final Path path = Paths.get(SystemVariable.ROOT.value(), folder);
        if (Files.exists(path) && Files.isDirectory(path)) {
            try {
                List<Path> paths = FileUtilities.listTree(path);
                response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + path.getFileName().toString() + ".zip");
                response.setStatus(HttpServletResponse.SC_OK);
                try (ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream())) {
                    for (Path p : paths) {
                        if (Files.isRegularFile(p)) {
                            String zipPath = path.relativize(p).toString();
                            ZipEntry entry = new ZipEntry(zipPath);
                            entry.setSize(Files.size(p));
                            entry.setTime(System.currentTimeMillis());
                            zipOutputStream.putNextEntry(entry);
                            StreamUtils.copy(Files.newInputStream(p), zipOutputStream);
                            zipOutputStream.closeEntry();
                        } else {
                            if (!path.equals(p)) {
                                String zipPath = path.relativize(p).toString() + File.separator;
                                ZipEntry entry = new ZipEntry(zipPath);
                                zipOutputStream.putNextEntry(entry);
                                zipOutputStream.closeEntry();
                            }
                        }
                    }
                    zipOutputStream.finish();
                }
            } catch (IOException ex) {
                try {
                    warn(ex.getMessage());
                    response.sendError(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
                } catch (IOException e) {
                    error(e.getMessage());
                }
            }
        } else {
            try {
                response.sendError(HttpStatus.BAD_REQUEST.value(), String.format("%s does not exist or it is not a folder", folder));
            } catch (IOException e) {
                error(e.getMessage());
            }
        }
    }

    @RequestMapping(value = "/download", method = RequestMethod.GET, produces = { "application/octet-stream", "application/json" } )
    public @ResponseBody ResponseEntity<?> download(@RequestParam("fileName") String fileName) {
        ResponseEntity<?> responseEntity;
        try {
            Resource file = loadAsResource(fileName);
            responseEntity = ResponseEntity.ok()
                    .contentLength(file.contentLength())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getFilename())
                    .body(file);
        } catch (IOException ex) {
            responseEntity = handleException(ex);
        }
        return responseEntity;
    }

    @RequestMapping(value = "/file/preview", method = RequestMethod.GET, produces = "application/json")
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

    @RequestMapping(value = "/file", method = RequestMethod.DELETE, produces = "application/json")
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

    @RequestMapping(value = "/file", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> move(@RequestParam("source") String source,
                                                  @RequestParam("destination") String destination) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            storageService.move(source, destination);
            responseEntity = prepareResult("The file has been moved successfully", ResponseStatus.SUCCEEDED);
        } catch (Exception ex) {
            responseEntity = handleException(ex);
        }
        return responseEntity;
    }

    private FileSystemResource loadAsResource(String fileName) throws IOException {
        if (fileName == null || fileName.isEmpty()) {
            throw new IOException("[fileName] cannot be null or empty");
        }
        final Path filePath = Paths.get(SystemVariable.ROOT.value(), fileName);
        if (!Files.exists(filePath)) {
            throw new IOException(String.format("File '%s' does not exist", filePath));
        }
        FileSystemResource resource = new FileSystemResource(filePath);
        //InputStreamResource resource = new InputStreamResource(Files.newInputStream(filePath));
        //Resource resource = new UrlResource(filePath.toUri());
        if (resource.exists() || resource.isReadable()) {
            return resource;
        } else {
            throw new IOException("Could not read file: " + fileName);
        }
    }
}


