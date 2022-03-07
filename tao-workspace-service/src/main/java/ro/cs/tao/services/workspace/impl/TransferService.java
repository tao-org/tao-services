package ro.cs.tao.services.workspace.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.services.entity.impl.FileStorageService;
import ro.cs.tao.services.interfaces.StorageService;
import ro.cs.tao.services.model.FileObject;
import ro.cs.tao.services.workspace.model.TransferRequest;
import ro.cs.tao.spi.ServiceRegistry;
import ro.cs.tao.spi.ServiceRegistryManager;
import ro.cs.tao.topology.Workspace;
import ro.cs.tao.topology.WorkspaceType;
import ro.cs.tao.utils.ExceptionUtils;
import ro.cs.tao.utils.executors.BlockingQueueWorker;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Logger;

@Service("transferService")
public class TransferService {
    private final BlockingQueue<TransferRequest> queue;
    //private final ExecutorService asyncCallbackWorker;
    private final Path stateFile = Paths.get(SystemVariable.ROOT.value()).resolve("transfer").resolve("transfer_queue.json");
    private Timer retryTimer;
    private final BlockingQueueWorker<TransferRequest> queueWorker;
    private final Object lock = new Object();
    private final Logger logger;

    @Autowired
    private FileStorageService fileStorageService;

    private TransferService() {
        this.logger = Logger.getLogger(TransferService.class.getName());
        this.queue = new LinkedBlockingDeque<>();
        this.queueWorker = new BlockingQueueWorker<>(this.queue, this::processRequest);
        //this.asyncCallbackWorker = Executors.newSingleThreadExecutor();
        restoreState();
        queueWorker.start();
    }

    /**
     * Submits a transfer request to the service queue.
     * @param request   The request
     */
    public void request(TransferRequest request) {
        try {
            this.queue.put(request);
        } catch (InterruptedException e) {
            logger.severe(e.getMessage());
        }
    }

    public StorageService getService(Workspace workspace) throws Exception {
        StorageService service;
        if (workspace.getType() == WorkspaceType.LOCAL) {
            service = fileStorageService;
        } else {
            final ServiceRegistry<StorageService> registry = ServiceRegistryManager.getInstance().getServiceRegistry(StorageService.class);
            final Set<StorageService> services = registry.getServices();
            service = services.stream().filter(s -> s.isIntendedFor(workspace.getUrlPrefix())).findFirst().orElse(null);
            if (service == null) {
                throw new IllegalArgumentException("No remote file repository for the given protocol is available");
            }
            // Don't forget that a SPI produces a single instance, and we don't want to re-use it
            service = service.getClass().newInstance();
            service.setup(workspace.getParameters());
        }
        return service;
    }

    private Void processRequest(TransferRequest item) {
        //this.asyncCallbackWorker.execute(() -> {
            try {
                final StorageService sourceService = getService(item.getSourceWorkspace());
                final Workspace dstWorkspace = item.getDestinationWorkspace();
                final StorageService destinationService = getService(dstWorkspace);
                final FileObject fileObject = item.getSource();
                final Object source = sourceService.download(Paths.get(fileObject.getRelativePath()));
                final InputStream sourceStream;
                if (source instanceof FileSystemResource) {
                    sourceStream = ((FileSystemResource) source).getInputStream();
                } else {
                    sourceStream = (InputStream) source;
                }
                logger.finest("Begin transferring file " + fileObject.getRelativePath());
                destinationService.storeFile(sourceStream, fileObject.getSize(), item.getDestinationPath(), fileObject.getRelativePath());
                logger.finest("End transferring file " + fileObject.getRelativePath());
                saveState();
                final TransferRequest next = this.queue.peek();
                if (next == null || !item.getBatch().equals(next.getBatch())) {
                    //TODO Should notify potential listeners
                    logger.fine("Transfer batch [" + item.getBatch() + "] completed");
                }
            } catch (Exception e) {
                logger.severe(ExceptionUtils.getStackTrace(logger, e));
                this.queueWorker.setPaused(true);
                initRetryTimer();
            }
        //});
        return null;
    }

    private void initRetryTimer() {
        if (this.retryTimer != null) {
            this.retryTimer.cancel();
        }
        this.retryTimer = new Timer();
        this.retryTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                TransferService.this.queueWorker.setPaused(false);
            }
        }, 300 * 1000);
    }

    private void saveState() {
        try {
            Files.createDirectories(this.stateFile.getParent());
            if (Files.exists(this.stateFile)) {
                synchronized (this.lock) {
                    Files.delete(this.stateFile);
                    final TransferRequest[] requests = this.queue.toArray(new TransferRequest[0]);
                    try (OutputStream stream = Files.newOutputStream(this.stateFile)) {
                        new ObjectMapper().writerFor(requests.getClass()).writeValue(stream, requests);
                        stream.flush();
                    }
                    logger.finest(String.format("Transfer queue state saved (%d items)", requests.length));
                }
            }
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
    }

    private void restoreState() {
        try {
            Files.createDirectories(this.stateFile.getParent());
            if (Files.exists(this.stateFile)) {
                synchronized (this.lock) {
                    this.queue.clear();
                    try (InputStream stream = Files.newInputStream(this.stateFile)) {
                        final List<TransferRequest> requests = new ObjectMapper().readerForListOf(TransferRequest.class)
                                                                                 .readValue(stream);
                        if (requests != null) {
                            this.queue.addAll(requests);
                            logger.finest(String.format("Transfer queue state restored (%d items)", requests.size()));
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
    }
}