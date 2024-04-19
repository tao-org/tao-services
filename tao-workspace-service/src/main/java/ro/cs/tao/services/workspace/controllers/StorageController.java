package ro.cs.tao.services.workspace.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.datasource.util.Zipper;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.eodata.enums.Visibility;
import ro.cs.tao.messaging.Messaging;
import ro.cs.tao.messaging.ProgressNotifier;
import ro.cs.tao.messaging.Topic;
import ro.cs.tao.persistence.*;
import ro.cs.tao.quota.QuotaException;
import ro.cs.tao.quota.UserQuotaManager;
import ro.cs.tao.security.SystemPrincipal;
import ro.cs.tao.security.UserPrincipal;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.entity.controllers.DataEntityController;
import ro.cs.tao.services.factory.StorageServiceFactory;
import ro.cs.tao.services.interfaces.ProductService;
import ro.cs.tao.services.interfaces.StorageService;
import ro.cs.tao.services.model.FileObject;
import ro.cs.tao.services.model.ItemAction;
import ro.cs.tao.services.workspace.impl.TransferService;
import ro.cs.tao.services.workspace.model.SubscriptionBean;
import ro.cs.tao.services.workspace.model.TransferRequest;
import ro.cs.tao.services.workspace.model.TransferableItem;
import ro.cs.tao.subscription.DataSubscription;
import ro.cs.tao.utils.*;
import ro.cs.tao.utils.executors.BlockingQueueWorker;
import ro.cs.tao.utils.executors.MemoryUnit;
import ro.cs.tao.utils.executors.monitoring.ProgressListener;
import ro.cs.tao.workspaces.Repository;
import ro.cs.tao.workspaces.RepositoryType;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/files/")
@Tag(name = "Workspaces", description = "Operations related to file operations on user workspaces")
public class StorageController extends DataEntityController<EOProduct, String, ProductService> {

    private static final String tokenJSON = "{ \"repository\":\"%s\", \"fileName\":%s, \"folder\":%s, \"user\":\"%s\" }";
    private static final Pattern maliciousPathRegEx = Pattern.compile("\\.\\.\\/|\\.\\.|\\/\\/|\\\\\\\\|\\/\\.\\/|\\\\\\.\\\\|%|;");

    private static final Map<String, Integer> maliciousAttempts = new HashMap<>();

    @Autowired
    private EOProductProvider productProvider;

    @Autowired
    private RepositoryProvider repositoryProvider;

    @Autowired
    private TransferService transferService;

    @Autowired
    private DataSubscriptionProvider dataSubscriptionProvider;

    @Autowired
    private UserProvider userProvider;

    private static final BlockingQueue<Tuple<Repository, String>> deletionQueue;

    static {
        deletionQueue = new HashedBlockingQueue<>(Tuple::getKeyTwo);
        BlockingQueueWorker<Tuple<Repository, String>> queueWorker = new BlockingQueueWorker<>(deletionQueue, new DeleteOperation(), 1, "delete-worker", 2000);
        queueWorker.start();
    }

    /**
     * List all configuration keys.
     *
     * @param filter    Filter to be applied on key names
     */
    @GetMapping(value = "config", produces = MediaType.APPLICATION_JSON_VALUE)
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
     * Lists the content of a remote workspace that is linked to the current user.
     *
     * @param workspaceId       The workspace identifier
     *
     * @param lastItem          [Optional] The item to start from (in case of paged results)
     */
    @PostMapping(value = "persistent", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> markPersistent(@RequestParam("workspaceId") String workspaceId) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            if (StringUtilities.isNullOrEmpty(workspaceId)) {
                throw new IOException("Not allowed");
            }
            repositoryProvider.setUserPersistentRepository(currentUser(), workspaceId);
            responseEntity = prepareResult("Repository " + workspaceId + " will be used for final workflow results");
        } catch (Exception ex) {
            responseEntity = handleException(ex);
        }

        return responseEntity;
    }
    /**
     * Lists the content of the user local workspace.
     */
    @GetMapping(value = "local/", produces = MediaType.APPLICATION_JSON_VALUE)
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
    @GetMapping(value = "local", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> list(@RequestParam("folder") String relativeFolder,
                                                   @RequestParam(name = "last", required = false) String lastItem) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            checkPath(relativeFolder);
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
    @PostMapping(value = "upload/", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> uploadUser(@RequestParam("file") MultipartFile file,
                                        @RequestParam("folder") String folder,
                                        @RequestParam("desc") String description) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            checkPath(folder);
            checkFile(file.getOriginalFilename());
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
    @PostMapping(value = "move/", produces = MediaType.APPLICATION_JSON_VALUE)
    @Deprecated
    public ResponseEntity<ServiceResponse<?>> move(@RequestParam("source") String source,
                                                   @RequestParam("destination") String destination) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            checkPath(source);
            checkPath(destination);
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
    @GetMapping(value = "output", produces = MediaType.APPLICATION_JSON_VALUE)
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
    @PostMapping(value = "toggle", produces = MediaType.APPLICATION_JSON_VALUE)
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
    @GetMapping(value = "browse", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> listWorkspace(@RequestParam("workspace") String workspaceId,
                                                            @RequestParam(name = "folder", required = false) String relativeFolder,
                                                            @RequestParam(name = "last", required = false) String lastItem) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            if (StringUtils.isEmpty(relativeFolder)) {
                relativeFolder = "/";
            }
            checkPath(relativeFolder);
            final Repository workspace = getOwnedWorkspace(currentUser(), workspaceId);
            final StorageService service = StorageServiceFactory.getInstance(workspace);
            // exclude items that were submitted to the deletion queue but not yet deleted
            final Set<String> exclusions = deletionQueue.stream()
                                                        .filter(e -> e.getKeyOne().equals(workspace))
                                                        .map(Tuple::getKeyTwo)
                                                        .collect(Collectors.toSet());
            final List<FileObject> list = service.listFiles(relativeFolder, null, lastItem, 1, exclusions);
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
    @PostMapping(value = "folder/", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createUserFolder(@RequestParam("workspace") String workspaceId,
                                              @RequestParam("folder") String folder) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            checkPath(folder);
            final Repository workspace = getOwnedWorkspace(currentUser(), workspaceId);
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
     * Deletes one or more files from the given user workspace
     *
     * @param workspaceId   The workspace identifier
     * @param file          The name of the file(s) or folder(s) to be deleted
     */
    @DeleteMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> delete(@RequestParam("workspace") String workspaceId,
                                                     @RequestParam("file") String file) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            String[] files = file.split(",");
            final Repository workspace = getOwnedWorkspace(currentUser(), workspaceId);
            for (String f : files) {
                checkPath(f);
                if (f.startsWith("/")) {
                    throw new IOException("The path must be relative [" + f + "]");
                }
                deletionQueue.put(new Tuple<>(workspace, f));
            }
            responseEntity = prepareResult(String.format("%d items marked for deletion", files.length), ResponseStatus.SUCCEEDED);
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
    @GetMapping(value = "/download/{repository}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> requestDownload(@PathVariable("repository") String repositoryId,
                                             @RequestParam(name = "fileName", required = false) String fileName,
                                             @RequestParam(name = "folder", required = false) String folder) {
        try {
            if (!StringUtilities.isNullOrEmpty(fileName) && !StringUtilities.isNullOrEmpty(folder)) {
                throw new IllegalArgumentException("Either 'fileName' or 'folder' must be set, but not both");
            }
            final boolean isFile = StringUtilities.isNullOrEmpty(folder);
            if (isFile) {
                checkPath(fileName);
            } else {
                checkPath(folder);
            }
            final Repository repository = repositoryProvider.get(repositoryId);
            if (repository == null) {
                throw new IllegalArgumentException("[repository] Invalid value");
            }
            final String user = currentUser();
            List<Repository> userRepositories = repositoryProvider.getByUser(user);
            if (userRepositories.stream().noneMatch(r -> repositoryId.equals(r.getId()))) {
                throw new IllegalArgumentException("Operation not allowed");
            }
            fileName = StringUtilities.isNullOrEmpty(fileName) ? null : "\"" + fileName + "\"";
            folder = StringUtilities.isNullOrEmpty(folder) ? null : "\"" + folder + "\"";
            final String json = String.format(tokenJSON, repositoryId, fileName, folder, user);
            return prepareResult("/files/get?token=" + URLEncoder.encode(Crypto.encrypt(json, SystemPrincipal.instance().getName()),
                                                                         StandardCharsets.UTF_8));
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
            if (isFile) {
                checkPath(fileName);
            } else {
                checkPath(folder);
            }
            switch (repository.getType()) {
                case LOCAL:
                    //asyncExecute(() -> {
                        try {
                            if (isFile) {
                                Resource file = loadAsResource(user, fileName);
                                response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
                                response.setContentLengthLong(file.contentLength());
                                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getFilename());
                                response.setStatus(HttpStatus.OK.value());
                                byte[] buffer = new byte[MemoryUnit.MB.value().intValue()];
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
                        } catch (IOException e) {
                            error(e.getMessage());
                        }
                    //});
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
     * Shares a file or a folder from the user workspace
     *
     * @param workspaceId   The identifier of the workspace the file or folder is in
     * @param file          The path of the file or of the folder, relative to the workspace root.
     *                      The file or folder can be part of only one share.
     * @param name          The name of the share. Name must be unique for a user
     */
    @PostMapping(value = "share", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> shareFile(@RequestParam("workspace") String workspaceId,
                                       @RequestParam("file") String file,
                                       @RequestParam("name") String name) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            checkPath(file);
            final Repository workspace = getOwnedWorkspace(currentUser(), workspaceId);
            if (!workspace.isSystem()) {
                throw new IOException("This repository doesn't support sharing files or folders");
            }
            final String path = workspace.resolve(file);
            DataSubscription subscription = dataSubscriptionProvider.get(currentUser(), workspaceId, file);
            // Already shared
            if (subscription != null) {
                throw new Exception(file + " is already shared");
            }
            subscription = dataSubscriptionProvider.get(currentUser(), name);
            // Unique share name
            if (subscription != null) {
                throw new Exception("A share with the name '" + name + "' already exists");
            }
            final String checkSum = getRepositoryService(workspace).computeHash(path);
            subscription = dataSubscriptionProvider.get(checkSum);
            // Checksum verification
            if (subscription != null) {
                throw new Exception("An dataset with the same contents is already shared [" + subscription.getName() + "]");
            }
            subscription = new DataSubscription();
            subscription.setName(StringUtilities.isNullOrEmpty(name) ? workspace.fileName(path) : name);
            subscription.setUserId(currentUser());
            subscription.setRepositoryId(workspaceId);
            subscription.setDataRootPath(path);
            subscription.setCreated(LocalDateTime.now());
            subscription.setCheckSum(checkSum);
            subscription = dataSubscriptionProvider.save(subscription);
            if (subscription.getId() > 0) {
                responseEntity = prepareResult(String.format("Share %s created", name), ResponseStatus.SUCCEEDED);
            } else {
                throw new Exception(String.format("%s was not shared", file));
            }
        } catch (Exception ex) {
            responseEntity = handleException(ex);
        }
        return responseEntity;
    }
    /**
     * Removes a share for the current user.
     * The share must belong to the current user.
     *
     * @param name    The name of the share to be removed.
     */
    @PostMapping(value = "unshare", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> unshareFile(@RequestParam("name") String name) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            if (StringUtils.isEmpty(name)) {
                throw new IOException("Invalid share name");
            }
            final DataSubscription subscription = dataSubscriptionProvider.get(currentUser(), name);
            if (subscription == null) {
                throw new IOException(String.format("%s is not shared by the current user", name));
            }
            dataSubscriptionProvider.delete(subscription);
            responseEntity = prepareResult(String.format("Share %s removed", subscription.getName()), ResponseStatus.SUCCEEDED);
        } catch (Exception ex) {
            responseEntity = handleException(ex);
        }
        return responseEntity;
    }
    /**
     * List the shared data of the current user
     *
     * @param userId    (optional) The user identifier for which to list the shared data.
     *                  This parameter is considered only for administrators.
     */
    @GetMapping(value = "share/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> listSharedFiles(@RequestParam(name = "userId", required = false) String userId) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        if (!isCurrentUserAdmin() && !StringUtilities.isNullOrEmpty(userId) && !userId.equals(currentUser())) {
            return prepareResult("Operation not permitted", HttpStatus.UNAUTHORIZED);
        }
        String user = StringUtilities.isNullOrEmpty(userId)
                      ? isCurrentUserAdmin()
                        ? null
                        : currentUser()
                      : userId;
        try {
            final List<SubscriptionBean> list = new ArrayList<>();
            final List<DataSubscription> shares = user != null ? dataSubscriptionProvider.getByUser(user) : dataSubscriptionProvider.list();
            if (shares != null) {
                list.addAll(shares.stream().map(s -> new SubscriptionBean(s)).collect(Collectors.toList()));
            }
            responseEntity = prepareResult(list);
        } catch (Exception ex) {
            responseEntity = handleException(ex);
        }
        return responseEntity;
    }

    /**
     * Copies a shared dataset to the persistent storage of the current user.
     *
     * @param subscriptionId    The id of the shared dataset
     */
    @PostMapping(value = "share/subscribe", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> subscribeToShare(@RequestParam(name = "id") long subscriptionId,
                                              @RequestParam(name = "userId", required = false) String userId) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            if (!StringUtilities.isNullOrEmpty(userId) && !isCurrentUserAdmin() && !currentUser().equals(userId)) {
                throw new IllegalArgumentException("Operation not allowed");
            }
            final DataSubscription subscription = dataSubscriptionProvider.get(subscriptionId);
            if (subscription == null) {
                throw new IOException("The specified share does not exist");
            }
            final String uId = userId != null ? userId : currentUser();
            if (uId.equals(subscription.getUserId())) {
                throw new IOException("Cannot subscribe to owned shared data");
            }
            final Repository targetRepository = repositoryProvider.getUserPersistentRepository(uId);
            if (targetRepository == null) {
                throw new IOException("User doesn't have a persistent storage repository");
            }
            final Repository sourceRepository = repositoryProvider.get(subscription.getRepositoryId());
            TransferRequest request = new TransferRequest();
            request.setSourceWorkspace(subscription.getRepositoryId());
            request.setDestinationWorkspace(targetRepository.getId());
            request.setDestinationPath(subscription.getName());
            request.setMove(false);
            request.setOverwrite(true);
            request.setFromAnotherUser(true);
            String relativePath = subscription.getDataRootPath().replace(sourceRepository.root() + "/", "");
            final StorageService storageService = getRepositoryService(sourceRepository);
            request.setFileObjects(storageService.listTree(relativePath));
            transfer(request);
            subscription.setSubscribersCount(subscription.getSubscribersCount() + 1);
            dataSubscriptionProvider.update(subscription);
            responseEntity = prepareResult("Started copying " + subscription.getName() + " in the " + targetRepository.getName() + " repository");
        } catch (Exception ex) {
            responseEntity = handleException(ex);
        }
        return responseEntity;
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
     *                          - an optional filter (regex) for matching remote items
     *
     */
    @PostMapping(value = "/transfer/", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> transfer(@RequestBody TransferRequest request) {
        try {
            if (request == null) {
                throw new IllegalArgumentException("Empty body");
            }
            final List<FileObject> fileObjects = request.getFileObjects();
            if (fileObjects == null || fileObjects.isEmpty()) {
                throw new IllegalArgumentException("Empty file list");
            }
            checkPath(request.getDestinationPath());
            fileObjects.forEach(f -> checkPath(f.getRelativePath()));
            final String currentUser = currentUser();
            final boolean isFromAnother = request.getFromAnotherUser() != null && request.getFromAnotherUser();
            asyncExecute(() -> {
                if (request == null) {
                    throw new IllegalArgumentException("Empty body");
                }
                final List<FileObject> requestedFiles = fileObjects;
                final Repository srcWorkspace = isFromAnother
                                                ? getWorkspace(request.getSourceWorkspace())
                                                : getOwnedWorkspace(currentUser, request.getSourceWorkspace());
                final Repository dstWorkspace = getOwnedWorkspace(currentUser, request.getDestinationWorkspace());
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
                Pattern pattern = null;
                if (request.getFilter() != null) {
                    try {
                        pattern = Pattern.compile(request.getFilter());
                    } catch (Throwable t) {
                        Logger.getLogger(StorageController.class.getName()).warning(t.getMessage());
                    }
                }
                for (FileObject fileObject : requestedFiles) {
                    String objectPath = fileObject.getRelativePath();
                    size = fileObject.getSize();
                    targetFolder = dstFolder;
                    if (fileObject.isFolder()) {
                        // Keep only the last part of the folder path (i.e. folder name)
                        targetFolder += srcWorkspace.fileName(objectPath);
                        final List<FileObject> files = service.listTree(fileObject.getRelativePath());
                        final List<TransferableItem> items = new ArrayList<>();
                        for (FileObject fObject : files) {
                            if (!fObject.isFolder()) {
                                String path = srcWorkspace.relativize(objectPath, fObject.getRelativePath());
                                path = targetFolder + (path.startsWith("/") ? "" : (targetFolder.endsWith("/") ? "" : "/") + path);
                                if (pattern == null || pattern.matcher(path).find()) {
                                    items.add(new TransferableItem(currentUser, objectPath, srcWorkspace, fObject, dstWorkspace, path, isMove, overwriteExisting));
                                    size += fObject.getSize();
                                    count++;
                                }
                            }
                        }
                        queueFiles(items.toArray(new TransferableItem[0]));
                    } else {
                        queueFile(objectPath, fileObject, srcWorkspace, dstWorkspace, targetFolder + srcWorkspace.fileName(objectPath), isMove, overwriteExisting);
                        count = 1;
                    }
                }
                return String.format("Transfer queued (%d files, %d bytes)", count, size);
            }, message -> Messaging.send(currentUser, Topic.INFORMATION.getCategory(), message), null);
            return prepareResult("Analysing remote items", ResponseStatus.SUCCEEDED);
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
    @PostMapping(value = "/extract", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> uncompress(@RequestParam("archive") String path,
                                                         @RequestParam(name = "folder", required = false) String destination) {
        try {
            checkPath(path);
            if (!path.toLowerCase().endsWith(".zip") && !path.toLowerCase().endsWith(".tar.gz")) {
                throw new IllegalArgumentException("Unsupported archive type");
            }
            final Repository workspace = getLocalWorkspace(currentUser());
            final String resolvedPath = workspace.resolve(path);
            if (StringUtilities.isNullOrEmpty(destination)) {
                destination = resolvedPath.substring(0, resolvedPath.lastIndexOf('/'));
            } else {
                checkPath(destination);
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

    /**
     * Retrieves blocks of text from a file.
     *
     * @param repositoryId  The identifier of the repository holding the file
     * @param file      The file path, relative to the root of the repository
     * @param lines     (optional) How many lines of text to retrieve. Default is 20.
     * @param skipLines (optional) The offset (i.e. line number) to start from. Default is 0.
     */
    @GetMapping(value = "/read", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> readAsText(@RequestParam("repositoryId") String repositoryId,
                                                         @RequestParam("file") String file,
                                                         @RequestParam(required = false, name = "lines") Integer lines,
                                                         @RequestParam(required = false, name = "skip") Integer skipLines) {
        try {
            checkPath(file);
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
     * @param item          The file onto which to perform the action
     * @param destination   (Optional) The destination of the action result
     * @return              A message indicating if the action was performed or not
     */
    @PostMapping(value = "/action", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> doAction(@RequestParam("action") String action,
                                                       @RequestParam(name = "item") String item,
                                                       @RequestParam(name = "destination", required = false) String destination) {
        try {
            if (StringUtilities.isNullOrEmpty(item)) {
                throw new IllegalArgumentException("No item provided");
            }
            final String[] items = item.split(";");
            final String successMessage = String.format("%s [%s] completed", action, item);
            final String user = currentUser();
            final TriConsumer<String, Exception, String> callback = (u, e, s) -> {
                String topic;
                String message;
                if (e == null) {
                    topic = Topic.INFORMATION.getCategory();
                    message = s;
                } else {
                    topic = Topic.WARNING.getCategory();
                    message = e.getMessage();
                }

                Messaging.send(user, topic, message);
            };
            final boolean oneArg = StringUtilities.isNullOrEmpty(destination);
            if (!oneArg) {
                checkPath(destination);
            }
            asyncExecute(() -> {
                if (StringUtilities.isNullOrEmpty(item)) {
                    throw new IllegalArgumentException("Not a file");
                }
                final Repository workspace = getLocalWorkspace(user);
                final StorageService<MultipartFile, FileSystemResource> repositoryService = getLocalRepositoryService(user);
                final List<ItemAction> actions = repositoryService.getRegisteredActions();
                if (actions != null) {
                    final ItemAction itemAction = actions.stream().filter(a -> a.name().equals(action)).findFirst().orElse(null);
                    if (itemAction != null) {
                        try {
                            checkPath(item);
                            final Path resolvedPath = Paths.get(workspace.resolve(item));
                            final ProgressListener listener = new ProgressNotifier(currentPrincipal(),
                                                                                   this,
                                                                                   Topic.TRANSFER_PROGRESS,
                                                                                   new HashMap<>() {{
                                                                                       put("Repository", workspace.getId());
                                                                                   }});
                            itemAction.setActionUser(user);
                            itemAction.setProgressListener(listener);
                            if (oneArg) {
                                itemAction.doAction(resolvedPath);
                            } else {
                                final Path[] paths = Arrays.stream(items).map(Paths::get).toArray(Path[]::new);
                                itemAction.doAction(paths, Paths.get(destination));
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }, successMessage, callback);
            return prepareResult(action + " started", ResponseStatus.SUCCEEDED);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * Renames a file or a folder
     * @param item          The file or folder to rename
     * @param newName       The new name
     * @return              A message indicating if the action was performed or not
     */
    @PostMapping(value = "/rename", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> rename(@RequestParam(name = "item") String item,
                                                     @RequestParam(name = "name") String newName) {
        final String successMessage = String.format("Renaming [%s] completed", item);
        final String user = currentUser();
        checkFile(newName);
        final TriConsumer<String, Exception, String> callback = (u, e, s) -> {
            String topic;
            String message;
            if (e == null) {
                topic = Topic.INFORMATION.getCategory();
                message = s;
            } else {
                topic = Topic.WARNING.getCategory();
                message = e.getMessage();
            }

            Messaging.send(user, topic, message);
        };
        asyncExecute(() -> {
            if (StringUtilities.isNullOrEmpty(item)) {
                throw new IllegalArgumentException("Not a file");
            }
            final StorageService<MultipartFile, FileSystemResource> repositoryService = getLocalRepositoryService(user);
            try {
                repositoryService.rename(item, newName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, successMessage, callback);
        return prepareResult(String.format("Renaming %s started", item), ResponseStatus.SUCCEEDED);
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
        final Repository repository = getLocalWorkspace(user);
        final Path path = Paths.get(repository.resolve(fileName)).toAbsolutePath();
        if (!Files.exists(path)) {
            throw new IOException(String.format("File '%s' does not exist", path));
        }
        return getLocalRepositoryService(user).download(path.toString());
    }

    private Repository getOwnedWorkspace(String user, String workspaceId) throws IOException {
        final Repository workspace = repositoryProvider.get(workspaceId);
        if (workspace == null) {
            throw new IOException("Workspace does not exist");
        }
        if (!user.equals(workspace.getUserId())) {
            throw new AccessDeniedException("Operation not allowed");
        }
        return workspace;
    }

    private Repository getPersistentWorkspace(String user, String workspaceId) throws IOException {
        final Repository workspace = repositoryProvider.getUserPersistentRepository(user);
        if (workspace == null) {
            throw new IOException("Workspace does not exist");
        }
        return workspace;
    }

    private Repository getWorkspace(String workspaceId) throws IOException {
        final Repository workspace = repositoryProvider.get(workspaceId);
        if (workspace == null) {
            throw new IOException("Workspace does not exist");
        }
        return workspace;
    }

    private static StorageService getRepositoryService(Repository workspace) {
        StorageService instance = StorageServiceFactory.getInstance(workspace);
        instance.associate(workspace);
        return instance;
    }

    private StorageService<MultipartFile, FileSystemResource> getLocalRepositoryService(String user) {
        return getRepositoryService(repositoryProvider.getUserSystemRepositories(user).stream().filter(w -> w.getType() == RepositoryType.LOCAL).findFirst().get());
    }

    private Repository getLocalWorkspace(String user) {
        return repositoryProvider.getUserSystemRepositories(user).stream().filter(w -> w.getType() == RepositoryType.LOCAL).findFirst().get();
    }

    private String sanitizePath(String path) {
        if (StringUtilities.isNullOrEmpty(path)) {
            return "/";
        } else {
            return path.replace("\\", "/");
        }
    }

    private void checkPath(String path) {
        IllegalArgumentException ex = null;
        if (StringUtilities.isNullOrEmpty(path)) {
            ex = new IllegalArgumentException("Empty path");
        }
        if (path.startsWith(".") || maliciousPathRegEx.matcher(path).find()) {
            ex = new IllegalArgumentException("Not allowed");
        }
        if (ex != null) {
            final String userId = currentUser();
            warn("User [%s] submitted a malicious path: %s", userId, path);
            incrementAttempts(userId);
            throw ex;
        }
    }

    private void checkFile(String fileName) {
        if (!StringUtilities.isNullOrEmpty(fileName)) {
            if (fileName.endsWith(".exe") || fileName.endsWith(".bat")) {
                final String userId = currentUser();
                warn("User [%s] submitted a suspicious file: %s", userId, fileName);
                incrementAttempts(userId);
                throw new IllegalArgumentException("Not allowed");
            }
        }
    }

    private void incrementAttempts(String userId) {
        Integer count = maliciousAttempts.putIfAbsent(userId, 0);
        if (count != null) {
            count = maliciousAttempts.put(userId, count++);
        }
        if (count != null && count == 3) {
            try {
                userProvider.disable(userId);
                error("Account " + userId + " was disabled due to multiple malicious attempts");
            } catch (PersistenceException e) {
                warn(e.getMessage());
            }
            maliciousAttempts.remove(userId);
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
            try {
                UserQuotaManager.getInstance().updateUserInputQuota(new UserPrincipal(item.getKeyOne().getUserId()));
            } catch (QuotaException e) {
                Logger.getLogger(DeleteOperation.class.getName()).warning(e.getMessage());
            }
            return null;
        }
    }
}


