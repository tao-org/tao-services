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
import ro.cs.tao.datasource.util.Zipper;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.eodata.enums.Visibility;
import ro.cs.tao.persistence.EOProductProvider;
import ro.cs.tao.persistence.RepositoryProvider;
import ro.cs.tao.security.SystemPrincipal;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.entity.controllers.DataEntityController;
import ro.cs.tao.services.factory.StorageServiceFactory;
import ro.cs.tao.services.interfaces.ProductService;
import ro.cs.tao.services.interfaces.StorageService;
import ro.cs.tao.services.model.FileObject;
import ro.cs.tao.services.model.ItemAction;
import ro.cs.tao.services.workspace.impl.TransferService;
import ro.cs.tao.services.workspace.model.TransferRequest;
import ro.cs.tao.services.workspace.model.TransferableItem;
import ro.cs.tao.utils.Crypto;
import ro.cs.tao.utils.HashedBlockingQueue;
import ro.cs.tao.utils.StringUtilities;
import ro.cs.tao.utils.Tuple;
import ro.cs.tao.utils.executors.BlockingQueueWorker;
import ro.cs.tao.workspaces.Repository;
import ro.cs.tao.workspaces.RepositoryType;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.zip.ZipOutputStream;

@Controller
@RequestMapping("/files/")
public class StorageController extends DataEntityController<EOProduct, String, ProductService> {

    private static final String tokenJSON = "{ \"repository\":\"%s\", \"fileName\":%s, \"folder\":%s, \"user\":\"%s\" }";

    @Autowired
    private EOProductProvider productProvider;

    @Autowired
    private RepositoryProvider repositoryProvider;

    @Autowired
    private TransferService transferService;

    private static final BlockingQueue<Tuple<Repository, String>> deletionQueue;

    static {
        deletionQueue = new HashedBlockingQueue<>(Tuple::getKeyTwo);
        BlockingQueueWorker<Tuple<Repository, String>> queueWorker = new BlockingQueueWorker<>(deletionQueue, new DeleteOperation(), 1, "delete-worker");
        queueWorker.start();
    }

    /**
     * List all configuration keys.
     *
     * @param filter    Filter to be applied on key names
     */
    @GetMapping(value = "config", produces = "application/json")
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
    @GetMapping(value = "local/", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> list() {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            responseEntity = prepareResult(getLocalRepositoryService(currentUser()).listUserWorkspace());
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
    @GetMapping(value = "local", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> list(@RequestParam("folder") String relativeFolder,
                                                   @RequestParam(name = "last", required = false) String lastItem) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            final StorageService repositoryService = getLocalRepositoryService(currentUser());
            if (relativeFolder == null || relativeFolder.trim().isEmpty() || relativeFolder.equals(".")) {
                responseEntity = prepareResult(repositoryService.listUserWorkspace());
            } else {
                responseEntity = prepareResult(repositoryService.listFiles(relativeFolder, null, lastItem, 1));
            }
        } catch (IOException ex) {
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
    @PostMapping(value = "upload/", produces = "application/json")
    public ResponseEntity<?> uploadUser(@RequestParam("file") MultipartFile file,
                                        @RequestParam("folder") String folder,
                                        @RequestParam("desc") String description) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            getLocalRepositoryService(currentUser()).storeUserFile(file, folder, description);
            responseEntity = prepareResult("Upload succeeded", ResponseStatus.SUCCEEDED);
        } catch (Exception ex) {
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
    @PostMapping(value = "move/", produces = "application/json")
    @Deprecated
    public ResponseEntity<ServiceResponse<?>> move(@RequestParam("source") String source,
                                                   @RequestParam("destination") String destination) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            getLocalRepositoryService(currentUser()).move(source, destination);
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
    @GetMapping(value = "output", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> listOutputs(@RequestParam("workflowId") long workflowId) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            responseEntity = prepareResult(getLocalRepositoryService(currentUser()).getWorkflowResults(workflowId));
        } catch (IOException ex) {
            responseEntity = handleException(ex);
        }

        return responseEntity;
    }
    //endregion

    //region Public workspace
    /**
     * Toggles the visibility (public or private) of a folder. A private folder is visible only to the user who created it.
     * @param folder        The folder
     * @param visibility    The new visibility
     */
    @PostMapping(value = "toggle", produces = "application/json")
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
    @GetMapping(value = "browse", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> listWorkspace(@RequestParam("workspace") String workspaceId,
                                                            @RequestParam(name = "folder", required = false) String relativeFolder,
                                                            @RequestParam(name = "last", required = false) String lastItem) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            if (StringUtils.isEmpty(relativeFolder)) {
                relativeFolder = "/";
            }
            if (relativeFolder.startsWith(".") || relativeFolder.startsWith("..")) {
                throw new IOException("Not allowed");
            }
            final Repository workspace = getOwnedWorkspace(workspaceId);
            final StorageService service = StorageServiceFactory.getInstance(workspace);
            final List<FileObject> list = service.listFiles(relativeFolder, null, lastItem, 1);
            // remove items that were submitted to the deletion queue but not yet deleted
            list.removeIf(f -> deletionQueue.contains(f.getRelativePath()));
            if (workspace.root().equals(relativeFolder) && list.isEmpty()) {
                list.add(service.emptyFolderItem());
            }
            responseEntity = prepareResult(list);
        } catch (Exception ex) {
            responseEntity = handleException(ex);
        }

        return responseEntity;
    }
    /**
     * Creates a folder in the user workspace
     *
     * @param folder    The name of the folder
     */
    @PostMapping(value = "folder/", produces = "application/json")
    public ResponseEntity<?> createUserFolder(@RequestParam("workspace") String workspaceId,
                                              @RequestParam("folder") String folder) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            if (StringUtils.isEmpty(folder) || folder.startsWith(".") || folder.startsWith("..")) {
                throw new IOException("The folder must be relative and should not start with '.' or '..'");
            }
            final Repository workspace = getOwnedWorkspace(workspaceId);
            final StorageService service = getRepositoryService(workspace);
            final Path createdPath = service.createFolder(folder.startsWith("/") ? folder.substring(1) : folder, true);
            if (createdPath == null) {
                throw new IOException("Folder was not created");
            }
            responseEntity = prepareResult(String.format("Folder %s created", folder), ResponseStatus.SUCCEEDED);
        } catch (Exception ex) {
            responseEntity = handleException(ex);
        }
        return responseEntity;
    }
    /**
     * Deletes a file from the given user workspace
     *
     * @param workspaceId   The workspace identifier
     * @param file          The name of the file or folder to be deleted
     */
    @DeleteMapping(produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> delete(@RequestParam("workspace") String workspaceId,
                                                     @RequestParam("file") String file) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            if (StringUtils.isEmpty(file) || file.startsWith(".") || file.startsWith("..") || file.startsWith("/")) {
                throw new IOException("The folder must be relative and should not start with '.' or '..'");
            }
            final Repository workspace = getOwnedWorkspace(workspaceId);
            deletionQueue.put(new Tuple<>(workspace, file));
            responseEntity = prepareResult(String.format("%s marked for deletion", file), ResponseStatus.SUCCEEDED);
        } catch (Exception ex) {
            responseEntity = handleException(ex);
        }
        return responseEntity;
    }
    /**
     * Requests a download url for a file or a folder from one of the user workspaces
     *
     * @param repositoryId  The repository identifier
     * @param fileName  The file name (exclusive with 'folder')
     * @param folder    The folder name (exclusive with 'file')
     */
    @GetMapping(value = "/download/{repository}", produces = "application/json")
    public ResponseEntity<?> requestDownload(@PathVariable("repository") String repositoryId,
                                             @RequestParam(name = "fileName", required = false) String fileName,
                                             @RequestParam(name = "folder", required = false) String folder) {
        try {
            final Repository repository = repositoryProvider.get(repositoryId);
            if (repository == null) {
                throw new IllegalArgumentException("[repository] Invalid value");
            }
            List<Repository> userRepositories = repositoryProvider.getByUser(currentUser());
            if (userRepositories.stream().noneMatch(r -> repositoryId.equals(r.getId()))) {
                throw new IllegalArgumentException("Operation not allowed");
            }
            final boolean isFile = StringUtilities.isNullOrEmpty(folder);
            if (!(StringUtilities.isNullOrEmpty(fileName) || isFile)) {
                throw new IllegalArgumentException("Either [fileName] or [folder] (but not both) must be supplied");
            }
            final String user = currentUser();
            if (repository.getType() == RepositoryType.LOCAL) {
                fileName = StringUtilities.isNullOrEmpty(fileName) ? null : "\"" + (fileName.startsWith(user) ? fileName : user + "/" + fileName) + "\"";
                folder = StringUtilities.isNullOrEmpty(folder) ? null : "\"" + (folder.startsWith(user) ? folder : user + "/" + folder) + "\"";
            } else {
                fileName = StringUtilities.isNullOrEmpty(fileName) ? null : "\"" + fileName + "\"";
                folder = StringUtilities.isNullOrEmpty(folder) ? null : "\"" + folder + "\"";
            }
            final String json = String.format(tokenJSON, repositoryId, fileName, folder, user);
            return prepareResult("/files/get?token=" + URLEncoder.encode(Crypto.encrypt(json, SystemPrincipal.instance().getName()), "UTF8"));
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
    @GetMapping(value = "/get", produces = "application/octet-stream")
    public void download(@RequestParam("token") String token, HttpServletResponse response) {
        try {
            final String decrypted = Crypto.decrypt(token, SystemPrincipal.instance().getName());
            final HashMap<String, String> values = new ObjectMapper().reader(HashMap.class).readValue(decrypted);
            final String repositoryId = values.get("repository");
            final String fileName = values.get("fileName");
            final String folder = values.get("folder");
            final String user = values.get("user");
            final Repository repository = repositoryProvider.get(repositoryId);
            final boolean isFile = StringUtilities.isNullOrEmpty(folder);
            switch (repository.getType()) {
                case LOCAL:
                    if (isFile) {
                        Resource file = loadAsResource(user, fileName);
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
                        final Path path = Paths.get(repository.resolve(folder));
                        if (Files.exists(path) && Files.isDirectory(path)) {
                            try {
                                response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
                                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + path.getFileName().toString() + ".zip");
                                response.setStatus(HttpServletResponse.SC_OK);
                                try (ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream())) {
                                    getLocalRepositoryService(user).streamToZip(path.toString(), zipOutputStream);
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
                default:
                    StorageService client = StorageServiceFactory.getInstance(repository.getUrlPrefix());
                    client.associate(repository);
                    final Path path;
                    if (isFile) {
                        //path = Paths.get(fileName.replace(repository.getUrlPrefix() + "://", ""));
                        String strPath = repository.resolve(fileName);
                        if (repository.getType() == RepositoryType.FTP || repository.getType() == RepositoryType.FTPS) {
                            strPath = strPath.replace(repository.root(), "");
                        } else {
                            strPath = strPath.replace(repository.getUrlPrefix() + "://", "");
                        }
                        path = Paths.get(strPath);
                        try (InputStream stream = (InputStream) client.download(path.toString())) {
                            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
                            response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + path.getFileName());
                            response.setStatus(HttpServletResponse.SC_OK);
                            IOUtils.copy(stream, response.getOutputStream());
                            response.flushBuffer();
                        }
                    } else {
                        //path = Paths.get(folder.replace(repository.getUrlPrefix() + "://", ""));
                        path = Paths.get(repository.resolve(folder).replace(repository.getUrlPrefix() + "://", ""));
                        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
                        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + path.getFileName().toString() + ".zip");
                        response.setStatus(HttpServletResponse.SC_OK);
                        try (ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream())) {
                            client.streamToZip(path.toString(), zipOutputStream);
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
     * @param request           The request structure for transferring a file or a folder.
     *                          It consists in:
     *                          - the source workspace identifier
     *                          - the destination workspace identifier
     *                          - the list of file objects to transfer
     *                          - an optional flag to move or copy the files (default is copy)
     *                          - an optional flag to overwrite existing files (default is not to overwrite)
     *
     */
    @PostMapping(value = "/transfer/", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> transfer(@RequestBody TransferRequest request) {
        try {
            if (request == null) {
                throw new IllegalArgumentException("Empty body");
            }
            final String user = currentUser();
            final List<FileObject> requestedFiles = request.getFileObjects();
            final Repository srcWorkspace = getOwnedWorkspace(request.getSourceWorkspace());
            final Repository dstWorkspace = getOwnedWorkspace(request.getDestinationWorkspace());
            if (dstWorkspace.isReadOnly()) {
                throw new AccessDeniedException("Destination is read-only");
            }
            int count = 0;
            long size = 0;
            String dstFolder = request.getDestinationPath();
            if (!dstFolder.endsWith("/")) {
                dstFolder += "/";
            }
            String targetFolder;
            final boolean isMove = request.getMove() != null && request.getMove();
            final boolean overwriteExisting = request.getOverwrite() != null && request.getOverwrite();
            final StorageService service = getRepositoryService(srcWorkspace);
            for (FileObject fileObject : requestedFiles) {
                String objectPath = fileObject.getRelativePath();
                size = fileObject.getSize();
                targetFolder = dstFolder;
                if (fileObject.isFolder()) {
                    // Keep only the last part of the folder path (i.e. folder name)
                    targetFolder += srcWorkspace.fileName(objectPath);
                    final List<FileObject> files = service.listTree(fileObject.getRelativePath());
                    //files.forEach(f -> f.setRelativePath(f.getRelativePath().replaceFirst(srcWorkspace.root() + "/", "")));
                    //files.forEach(f -> f.setRelativePath());
                    final List<TransferableItem> items = new ArrayList<>();
                    for (FileObject fObject : files) {
                        if (!fObject.isFolder()) {
                            String path = srcWorkspace.relativize(objectPath, fObject.getRelativePath());
                            path = targetFolder + (path.startsWith("/") ? "" : (targetFolder.endsWith("/") ? "" : "/") + path);
                            items.add(new TransferableItem(user, objectPath, srcWorkspace, fObject, dstWorkspace, path, isMove, overwriteExisting));
                            size += fObject.getSize();
                            count++;
                        }
                    }
                    queueFiles(items.toArray(new TransferableItem[0]));
                } else {
                    queueFile(objectPath, fileObject, srcWorkspace, dstWorkspace, targetFolder + srcWorkspace.fileName(objectPath), isMove, overwriteExisting);
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

    /**
     * Decompresses a local archive (zip or tar.gz) into a local folder
     * @param path          The archive path
     * @param destination   (Optional) The destination folder
     * @return              The name of the extracted folder
     */
    @PostMapping(value = "/extract", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> uncompress(@RequestParam("archive") String path,
                                                         @RequestParam(name = "folder", required = false) String destination) {
        try {
            if (StringUtilities.isNullOrEmpty(path)) {
                throw new IllegalArgumentException("Not a file");
            }
            if (!path.toLowerCase().endsWith(".zip") && !path.toLowerCase().endsWith(".tar.gz")) {
                throw new IllegalArgumentException("Unsupported archive type");
            }
            final Repository workspace = getLocalWorkspace(currentUser());
            final String resolvedPath = workspace.resolve(path);
            if (StringUtilities.isNullOrEmpty(destination)) {
                destination = resolvedPath.substring(0, resolvedPath.lastIndexOf('/'));
            }
            final Path out;
            if (path.toLowerCase().endsWith(".zip")) {
                out = Zipper.decompressZip(Paths.get(resolvedPath).toAbsolutePath(), Paths.get(destination).toAbsolutePath(), false);
            } else {
                out = Zipper.decompressTarGz(Paths.get(resolvedPath).toAbsolutePath(), Paths.get(destination).toAbsolutePath(), false);
            }
            return prepareResult(out.getFileName());
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping(value = "/read", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> readAsText(@RequestParam("repositoryId") String repositoryId,
                                                         @RequestParam("file") String file,
                                                         @RequestParam(required = false, name = "lines") Integer lines,
                                                         @RequestParam(required = false, name = "skip") Integer skipLines) {
        try {
            if (StringUtilities.isNullOrEmpty(file)) {
                throw new IllegalArgumentException("Not a file");
            }
            final Repository repository = repositoryProvider.get(repositoryId);
            if (repository == null) {
                throw new IllegalArgumentException("Invalid repository");
            }
            final StorageService storageService = getRepositoryService(repository);
            final int nLines = lines != null ? lines : 20;
            final int nSkip = skipLines != null ? skipLines : 0;
            String bucketName = repository.bucketName();
            String bucketRelative = repository.relativizeToBucket(file);
            return prepareResult(storageService.readAsText(storageService.download(bucketName
                                                                                   + (bucketRelative.startsWith("/") || bucketRelative.startsWith(repository.getUrlPrefix())
                                                                                      ? "" : "/")
                                                                                   + bucketRelative),
                                                           nLines, nSkip));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * Executes an action on the selected file
     * @param action        The action name
     * @param file          The file onto which to perform the action
     * @return              A message indicating if the action was performed or not
     */
    @PostMapping(value = "/action", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> doAction(@RequestParam("action") String action,
                                                       @RequestParam(name = "file") String file) {
        try {
            if (StringUtilities.isNullOrEmpty(file)) {
                throw new IllegalArgumentException("Not a file");
            }
            final Repository workspace = getLocalWorkspace(currentUser());
            final StorageService<MultipartFile, FileSystemResource> repositoryService = getLocalRepositoryService(currentUser());
            final List<ItemAction> actions = repositoryService.getRegisteredActions();
            if (actions != null) {
                final ItemAction itemAction = actions.stream().filter(a -> a.name().equals(action)).findFirst().orElse(null);
                if (itemAction != null) {
                    final String resolvedPath = workspace.resolve(file);
                    try {
                        itemAction.doAction(Paths.get(resolvedPath));
                        return prepareResult(actions + " completed", ResponseStatus.SUCCEEDED);
                    } catch (Exception e) {
                        return handleException(e);
                    }
                }
            }
            return prepareResult("No action to perform", ResponseStatus.SUCCEEDED);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    private void queueFile(String batchId, FileObject file, Repository source, Repository destination, String folder, boolean move, boolean overwrite) {
        final TransferableItem request = new TransferableItem(currentUser(), batchId, source, file, destination, folder, move, overwrite);
        transferService.request(request);
    }

    private void queueFiles(TransferableItem[] items) {
        transferService.request(items);
    }

    private FileSystemResource loadAsResource(String user, String fileName) throws IOException {
        if (fileName == null || fileName.isEmpty()) {
            throw new IOException("[fileName] cannot be null or empty");
        }
        final Path filePath = Paths.get(SystemVariable.ROOT.value(), fileName).toAbsolutePath();
        if (!Files.exists(filePath)) {
            throw new IOException(String.format("File '%s' does not exist", filePath));
        }
        return getLocalRepositoryService(user).download(filePath.toString());
    }

    private Repository getOwnedWorkspace(String workspaceId) throws IOException {
        final Repository workspace = repositoryProvider.get(workspaceId);
        if (workspace == null) {
            throw new IOException("Workspace does not exist");
        }
        if (!currentUser().equals(workspace.getUserName())) {
            throw new AccessDeniedException("Operation not allowed");
        }
        return workspace;
    }

    private static StorageService getRepositoryService(Repository workspace) {
        StorageService instance = StorageServiceFactory.getInstance(workspace);
        instance.associate(workspace);
        return instance;
    }

    private StorageService<MultipartFile, FileSystemResource> getLocalRepositoryService(String user) {
        return getRepositoryService(repositoryProvider.getByUser(user).stream().filter(w -> w.getType() == RepositoryType.LOCAL).findFirst().get());
    }

    private Repository getLocalWorkspace(String user) {
        return repositoryProvider.getByUser(user).stream().filter(w -> w.getType() == RepositoryType.LOCAL).findFirst().get();
    }

    private String sanitizePath(String path) {
        if (StringUtilities.isNullOrEmpty(path)) {
            return "/";
        } else {
            return path.replace("\\", "/");
        }
    }

    private static class DeleteOperation implements Function<Tuple<Repository, String>, Void> {

        @Override
        public Void apply(Tuple<Repository, String> item) {
            final StorageService service = getRepositoryService(item.getKeyOne());
            String[] list = item.getKeyTwo().split(",");
            for (String f : list) {
                try {
                    service.remove(f);
                } catch (IOException e) {
                    Logger.getLogger(StorageController.class.getName()).warning(e.getMessage());
                }
            }
            return null;
        }
    }
}


