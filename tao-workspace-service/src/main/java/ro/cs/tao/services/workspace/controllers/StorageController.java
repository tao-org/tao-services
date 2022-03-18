package ro.cs.tao.services.workspace.controllers;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.eodata.enums.Visibility;
import ro.cs.tao.persistence.EOProductProvider;
import ro.cs.tao.persistence.WorkspaceProvider;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.security.SystemPrincipal;
import ro.cs.tao.serialization.BaseSerializer;
import ro.cs.tao.serialization.SerializerFactory;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.entity.controllers.DataEntityController;
import ro.cs.tao.services.factory.StorageServiceFactory;
import ro.cs.tao.services.interfaces.ProductService;
import ro.cs.tao.services.interfaces.StorageService;
import ro.cs.tao.services.model.FileObject;
import ro.cs.tao.services.workspace.impl.TransferService;
import ro.cs.tao.services.workspace.model.TransferRequest;
import ro.cs.tao.topology.Workspace;
import ro.cs.tao.topology.WorkspaceType;
import ro.cs.tao.utils.Crypto;
import ro.cs.tao.utils.StringUtilities;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipOutputStream;

@Controller
@RequestMapping("/files")
public class StorageController extends DataEntityController<EOProduct, String, ProductService> {

    @Autowired
    private StorageService<MultipartFile, FileSystemResource> fileStorageService;

    @Autowired
    private EOProductProvider productProvider;

    @Autowired
    private WorkspaceProvider workspaceProvider;

    @Autowired
    private TransferService transferService;

    /**
     * List all configuration keys.
     *
     * @param filter    Filter to be applied on key names
     */
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

    //region Private workspace
    /**
     * Lists the content of the user local workspace.
     */
    @RequestMapping(value = "/local/", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> list() {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            responseEntity = prepareResult(fileStorageService.listUserWorkspace(currentUser()));
        } catch (IOException ex) {
            responseEntity = handleException(ex);
        }

        return responseEntity;
    }
    /**
     * Lists the content of a local workspace folder, optionally starting from the last loaded item (this would allow
     * for paginating the content if too large).
     *
     * @param relativeFolder    The folder relative to the root of the workspace
     * @param lastItem          [Optional] The last retrieved item to start from
     */
    @RequestMapping(value = "/local", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> list(@RequestParam("folder") String relativeFolder,
                                                   @RequestParam(name = "last", required = false) String lastItem) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            final String currentUser = currentUser();
            if (relativeFolder == null || relativeFolder.trim().isEmpty() || relativeFolder.equals(".")) {
                responseEntity = prepareResult(fileStorageService.listUserWorkspace(currentUser));
            } else {
                Path relativePath = Paths.get(relativeFolder);
                if (relativePath.getName(0).toString().equals(currentUser)) {
                    relativeFolder = relativePath.getNameCount() > 1 ? relativePath.subpath(1, relativePath.getNameCount()).toString() : "";
                }
                final Path lastPath = lastItem != null ? Paths.get(lastItem) : null;
                responseEntity = prepareResult(fileStorageService.listFiles(SessionStore.currentContext().getWorkspace().resolve(relativeFolder), null, lastPath));
            }
        } catch (IOException ex) {
            responseEntity = handleException(ex);
        }

        return responseEntity;
    }
    /**
     * Lists the files uploaded by the user in his/her workspace.
     * These files should be found in the 'files' workspace folder.
     */
    @RequestMapping(value = "/local/uploaded/", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> listFiles() {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            responseEntity = prepareResult(fileStorageService.listUploaded(currentUser()));
        } catch (IOException ex) {
            responseEntity = handleException(ex);
        }

        return responseEntity;
    }
    /**
     * Creates a folder in the user workspace
     *
     * @param folder    The name of the folder
     */
    @RequestMapping(value = "/folder/", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<?> createUserFolder(@RequestParam("folder") String folder) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            fileStorageService.createFolder(folder, true);
            responseEntity = prepareResult(String.format("Folder %s created", folder), ResponseStatus.SUCCEEDED);
        } catch (Exception ex) {
            responseEntity = handleException(ex);
        }
        return responseEntity;
    }
    /**
     * Uploads a file into the given folder of the user workspace
     * @param file          The file to be uploaded
     * @param folder        The folder into which the file will be uploaded
     * @param description   A description of the file
     */
    @RequestMapping(value = "/upload/", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<?> uploadUser(@RequestParam("file") MultipartFile file,
                                        @RequestParam("folder") String folder,
                                        @RequestParam("desc") String description) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            fileStorageService.storeUserFile(file, folder, description);
            responseEntity = prepareResult("Upload succeeded", ResponseStatus.SUCCEEDED);
        } catch (Exception ex) {
            responseEntity = handleException(ex);
        }
        return responseEntity;
    }
    /**
     * Deletes a file from the user workspace
     *
     * @param fileName      The name of the file to be deleted
     */
    @RequestMapping(value = "/delete/", method = RequestMethod.DELETE, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> delete(@RequestParam("fileName") String fileName) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            fileStorageService.remove(fileName);
            responseEntity = prepareResult("Delete succeeded", ResponseStatus.SUCCEEDED);
        } catch (IOException ex) {
            responseEntity = handleException(ex);
        }
        return responseEntity;
    }
    /**
     * Moves a file or a product to another folder.
     *
     * @param source        The source file (or product folder)
     * @param destination   The destination folder
     */
    @RequestMapping(value = "/move/", method = RequestMethod.POST, produces = "application/json")
    @Deprecated
    public ResponseEntity<ServiceResponse<?>> move(@RequestParam("source") String source,
                                                   @RequestParam("destination") String destination) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            fileStorageService.move(source, destination);
            responseEntity = prepareResult("The file has been moved successfully", ResponseStatus.SUCCEEDED);
        } catch (Exception ex) {
            responseEntity = handleException(ex);
        }
        return responseEntity;
    }
    /**
     * Lists the outputs of a workflow.
     *
     * @param workflowId    The workflow identifier
     */
    @RequestMapping(value = "/output", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> listOutputs(@RequestParam("workflowId") long workflowId) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            responseEntity = prepareResult(fileStorageService.getWorkflowResults(workflowId));
        } catch (IOException ex) {
            responseEntity = handleException(ex);
        }

        return responseEntity;
    }
    //endregion

    //region Public workspace
    /**
     * Lists the content of the public (shared) workspace
     */
    @RequestMapping(value = "/public/", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> listPublic() {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            responseEntity = prepareResult(fileStorageService.listPublicWorkspace());
        } catch (IOException ex) {
            responseEntity = handleException(ex);
        }

        return responseEntity;
    }
    /**
     * Lists the content of the given folder from the public workspace
     * @param relativeFolder    The folder
     */
    @RequestMapping(value = "/public", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> listPublic(@RequestParam("folder") String relativeFolder) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            if (relativeFolder == null || relativeFolder.trim().isEmpty() || relativeFolder.equals(".")) {
                responseEntity = prepareResult(fileStorageService.listPublicWorkspace());
            } else {
                if (relativeFolder.startsWith("public")) {
                    relativeFolder = relativeFolder.substring(relativeFolder.indexOf('/') + 1);
                }
                responseEntity = prepareResult(fileStorageService.listFiles(Paths.get(SystemVariable.SHARED_WORKSPACE.value(), relativeFolder), null, null));
            }
        } catch (IOException ex) {
            responseEntity = handleException(ex);
        }

        return responseEntity;
    }
    /**
     * Lists the uploaded files that are visible to all users.
     */
    @RequestMapping(value = "/public/uploaded/", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> listPublicFiles() {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            responseEntity = prepareResult(fileStorageService.listUploaded());
        } catch (IOException ex) {
            responseEntity = handleException(ex);
        }

        return responseEntity;
    }
    /**
     * Creates a folder in the public workspace
     *
     * @param folder    The name of the folder
     */
    @RequestMapping(value = "/public/folder/", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<?> createPublicFolder(@RequestParam("folder") String folder) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            fileStorageService.createFolder(folder, false);
            responseEntity = prepareResult(String.format("Folder %s created", folder), ResponseStatus.SUCCEEDED);
        } catch (Exception ex) {
            responseEntity = handleException(ex);
        }
        return responseEntity;
    }
    /**
     * Uploads a file into the given folder of the public workspace
     * @param file          The file to be uploaded
     * @param folder        The folder into which the file will be uploaded
     * @param description   A description of the file
     */
    @RequestMapping(value = "/public/upload", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<?> uploadPublic(@RequestParam("file") MultipartFile file,
                                          @RequestParam("folder") String folder,
                                          @RequestParam("desc") String description) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            if (!isCurrentUserAdmin()) {
                throw new AccessDeniedException("The operation is permitted only for administrators");
            }
            fileStorageService.storePublicFile(file, folder, description);
            responseEntity = prepareResult("Upload succeeded", ResponseStatus.SUCCEEDED);
        } catch (Exception ex) {
            responseEntity = handleException(ex);
        }
        return responseEntity;
    }
    /**
     * Toggles the visibility (public or private) of a folder. A private folder is visible only to the user who created it.
     * @param folder        The folder
     * @param visibility    The new visibility
     */
    @RequestMapping(value = "/toggle", method = RequestMethod.POST, produces = "application/json")
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
    //endregion

    //region Remote workspaces
    /**
     * Lists the content of a remote workspace that is linked to the current user.
     *
     * @param workspaceId       The workspace identifier
     * @param relativeFolder    The folder to start with
     * @param lastItem          [Optional] The item to start from (in case of paged results)
     */
    @RequestMapping(value = "/browse", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> listRemote(@RequestParam("workspace") int workspaceId,
                                                         @RequestParam("folder") String relativeFolder,
                                                         @RequestParam(name = "last", required = false) String lastItem) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            if (StringUtils.isEmpty(relativeFolder) || relativeFolder.startsWith(".")) {
                throw new IOException("For remote files, the relative folder must begin with the bucket name");
            }
            final Workspace workspace = workspaceProvider.get(workspaceId);
            if (workspace == null) {
                throw new IOException("Workspace does not exist");
            }
            if (workspace.getType() == WorkspaceType.LOCAL) {
                return list(relativeFolder, lastItem);
            }
            Path relativePath = Paths.get(relativeFolder);
            if (relativePath.getName(0).toString().equals(currentUser())) {
                relativeFolder = relativePath.getNameCount() > 1 ? relativePath.subpath(1, relativePath.getNameCount()).toString() : "";
            }
            StorageService service = StorageServiceFactory.getInstance(workspace);
            final Path lastPath = lastItem != null ? Paths.get(lastItem) : null;
            responseEntity = prepareResult(service.listFiles(Paths.get(relativeFolder), null, lastPath));
        } catch (Exception ex) {
            responseEntity = handleException(ex);
        }

        return responseEntity;
    }
    /**
     * Requests a download url for a file or a folder from one of the user workspaces
     *
     * @param protocol  The workspace protocol
     * @param fileName  The file name (exclusive with 'folder')
     * @param folder    The folder name (exclusive with 'file')
     */
    @RequestMapping(value = "/download/{protocol}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> requestDownload(@PathVariable("protocol") String protocol,
                                             @RequestParam(name = "fileName", required = false) String fileName,
                                             @RequestParam(name = "folder", required = false) String folder) {
        try {
            final WorkspaceType workspaceType = WorkspaceType.fromPrefix(protocol);
            if (workspaceType == null) {
                throw new IllegalArgumentException("[protocol] Invalid value");
            }
            final boolean isFile = StringUtilities.isNullOrEmpty(folder);
            if (!(StringUtilities.isNullOrEmpty(fileName) || isFile)) {
                throw new IllegalArgumentException("Either [fileName] or [folder] (but not both) must be supplied");
            }
            final String user = currentUser();
            if (workspaceType == WorkspaceType.LOCAL) {
                fileName = StringUtilities.isNullOrEmpty(fileName) ? null : "\"" + (fileName.startsWith(user) ? fileName : user + "/" + fileName) + "\"";
                folder = StringUtilities.isNullOrEmpty(folder) ? null : "\"" + (folder.startsWith(user) ? folder : user + "/" + folder) + "\"";
            } else {
                fileName = StringUtilities.isNullOrEmpty(fileName) ? null : "\"" + fileName + "\"";
                folder = StringUtilities.isNullOrEmpty(folder) ? null : "\"" + folder + "\"";
            }
            final String json = "{ \"protocol\": \"" + protocol + "\", "
                    + " \"fileName\": " + fileName + ", "
                    + "\"folder\": " + folder + ", "
                    + "\"user\":\"" + user + "\" }";
            return prepareResult("/files/get?token=" + URLEncoder.encode(Crypto.encrypt(json, SystemPrincipal.instance().getName()),
                                                                         "UTF8"));
        } catch (Exception e) {
            return handleException(e);
        }
    }
    /**
     * Downloads a file or folder (as a zip) from a repository.
     *
     * @param token         The coded request
     * @param response      The servlet response to write to
     */
    @RequestMapping(value = "/get", method = RequestMethod.GET, produces = "application/octet-stream")
    public void download(@RequestParam("token") String token, HttpServletResponse response) {
        try {
            final String decrypted = Crypto.decrypt(token, SystemPrincipal.instance().getName());
            final HashMap<String, String> values = new ObjectMapper().reader(HashMap.class).readValue(decrypted);
            final String protocol = values.get("protocol");
            final String fileName = values.get("fileName");
            final String folder = values.get("folder");
            final String user = values.get("user");
            final WorkspaceType workspaceType = WorkspaceType.fromPrefix(protocol);
            final boolean isFile = StringUtilities.isNullOrEmpty(folder);
            switch (workspaceType) {
                case LOCAL:
                    if (isFile) {
                        Resource file = loadAsResource(fileName);
                        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
                        response.setContentLengthLong(file.contentLength());
                        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getFilename());
                        response.setStatus(HttpStatus.OK.value());
                        byte[] buffer = new byte[262144];
                        try (InputStream in = file.getInputStream()) {
                            int read;
                            while ((read = in.read(buffer)) > 0) {
                                response.getOutputStream().write(buffer, 0, read);
                            }
                        }
                    } else {
                        final Path path = Paths.get(SystemVariable.ROOT.value(), folder);
                        if (Files.exists(path) && Files.isDirectory(path)) {
                            try {
                                response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
                                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + path.getFileName().toString() + ".zip");
                                response.setStatus(HttpServletResponse.SC_OK);
                                try (ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream())) {
                                    this.fileStorageService.streamToZip(path, zipOutputStream);
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
                    break;
                case AWS:
                case SWIFT:
                    StorageService client = StorageServiceFactory.getInstance(protocol);
                    final List<Workspace> userWorkspaces = workspaceProvider.getByUser(user);
                    if (userWorkspaces == null) {
                        response.sendError(HttpStatus.BAD_REQUEST.value());
                    }
                    final Workspace userWorkspace = userWorkspaces.stream().filter(w -> w.getType().equals(workspaceType)).findFirst().orElse(null);
                    if (userWorkspace == null) {
                        response.sendError(HttpStatus.BAD_REQUEST.value());
                    }
                    client.setup(userWorkspace.getParameters());
                    final Path path;
                    if (isFile) {
                        path = Paths.get(fileName.replace("s3://", ""));
                        InputStream stream = (InputStream) client.download(path);
                        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
                        response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + path.getFileName());
                        response.setStatus(HttpServletResponse.SC_OK);
                        IOUtils.copy(stream, response.getOutputStream());
                        response.flushBuffer();
                    } else {
                        path = Paths.get(folder.replace("s3://", ""));
                        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
                        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + path.getFileName().toString() + ".zip");
                        response.setStatus(HttpServletResponse.SC_OK);
                        try (ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream())) {
                            client.streamToZip(path, zipOutputStream);
                            zipOutputStream.finish();
                        }
                    }
                    break;
            }
        } catch (IOException e) {
            error(e.getMessage());
        }
    }
    /**
     * Transfers one or more file objects from the source workspace to the destination workspace
     *
     * @param srcWksId          The source workspace identifier
     * @param jsonFileObject    The JSON representation of either a single file object or a list of file objects
     * @param dstWksId          The destination workspace identifier
     * @param dstFolder         The folder in the destination workspace where to transfer to
     */
    @RequestMapping(value = "/transfer/", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> transfer(@RequestParam("sourceWorkspace") int srcWksId,
                                                       @RequestParam("sourceObject") String jsonFileObject,
                                                       @RequestParam("destinationWorkspace") int dstWksId,
                                                       @RequestParam("destinationPath") String dstFolder,
                                                       @RequestParam(name = "move", required = false) Boolean move,
                                                       @RequestParam(name = "overwrite", required = false) Boolean overwrite) {
        try {
            List<FileObject> requestedFiles;
            try {
                final FileObject fileObject = new ObjectMapper().reader(FileObject.class).readValue(jsonFileObject);
                requestedFiles = new ArrayList<>();
                requestedFiles.add(fileObject);
            } catch (Exception e) {
                BaseSerializer<FileObject> serializer = SerializerFactory.create(FileObject.class, ro.cs.tao.serialization.MediaType.JSON);
                requestedFiles = serializer.deserialize(FileObject.class, jsonFileObject);
            }
            final Workspace srcWorkspace = workspaceProvider.get(srcWksId);
            final Workspace dstWorkspace = workspaceProvider.get(dstWksId);
            if (dstWorkspace.isReadOnly()) {
                throw new IllegalArgumentException("Destination is read-only");
            }
            int count = 0;
            long size = 0;
            if (!dstFolder.endsWith("/")) {
                dstFolder += "/";
            }
            final String targetRoot = dstWorkspace.getParameters().get(dstWorkspace.getType().rootKey()) + "/" + currentUser() + "/";
            String targetFolder;
            final boolean isMove = move != null && move;
            final boolean overwriteExisting = overwrite != null && overwrite;
            for (FileObject fileObject : requestedFiles) {
                String root = fileObject.getRelativePath();
                size = fileObject.getSize();
                targetFolder = dstFolder;
                if (fileObject.isFolder()) {
                    // Keep only the last part of the folder path (i.e. folder name)
                    final int idx = root.lastIndexOf('/', root.length() - 2);
                    if (idx > 0) {
                        targetFolder += root.substring(idx + 1);
                    }
                    final StorageService service = srcWorkspace.getType() == WorkspaceType.LOCAL
                            ? fileStorageService
                            : StorageServiceFactory.getInstance(srcWorkspace);
                    final List<FileObject> files = service.listTree(Paths.get(fileObject.getRelativePath()));
                    for (FileObject fObject : files) {
                        if (!fObject.isFolder()) {
                            final String path = targetRoot + targetFolder + fObject.getRelativePath().replace(root, "");
                            queueFile(root, fObject, srcWorkspace, dstWorkspace, path, isMove, overwriteExisting);
                            size += fObject.getSize();
                            count++;
                        }
                    }
                } else {
                    queueFile(root, fileObject, srcWorkspace, dstWorkspace,
                              dstWorkspace.getParameters().get(dstWorkspace.getType().rootKey())
                                      + "/" + currentUser() + "/" + targetFolder,
                              isMove, overwriteExisting);
                    count = 1;
                }
            }
            return prepareResult(String.format("Transfer queued (%d files, %d bytes)", count, size),
                                 ResponseStatus.SUCCEEDED);
        } catch (Exception e) {
            return handleException(e);
        }
    }
    //endregion

    private void queueFile(String batchId, FileObject file, Workspace source, Workspace destination, String folder, boolean move, boolean overwrite) {
        final TransferRequest request = new TransferRequest(batchId, source, file, destination, folder, move, overwrite);
        transferService.request(request);
    }

    private FileSystemResource loadAsResource(String fileName) throws IOException {
        if (fileName == null || fileName.isEmpty()) {
            throw new IOException("[fileName] cannot be null or empty");
        }
        final Path filePath = Paths.get(SystemVariable.ROOT.value(), fileName).toAbsolutePath();
        if (!Files.exists(filePath)) {
            throw new IOException(String.format("File '%s' does not exist", filePath));
        }
        return (FileSystemResource) this.fileStorageService.download(filePath);
    }
}


