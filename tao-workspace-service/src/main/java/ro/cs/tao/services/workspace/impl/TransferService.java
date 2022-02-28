package ro.cs.tao.services.workspace.impl;

import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import ro.cs.tao.services.interfaces.StorageService;
import ro.cs.tao.services.model.FileObject;
import ro.cs.tao.services.workspace.model.TransferRequest;
import ro.cs.tao.spi.ServiceRegistry;
import ro.cs.tao.spi.ServiceRegistryManager;
import ro.cs.tao.topology.Workspace;
import ro.cs.tao.utils.ExceptionUtils;
import ro.cs.tao.utils.executors.BlockingQueueWorker;

import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Logger;

@Service("transferService")
public class TransferService {
    private static final int TIMEOUT = 300;
    private final BlockingQueue<TransferRequest> queue;
    private final ExecutorService asyncCallbackWorker;
    private final Logger logger;

    private TransferService() {
        this.logger = Logger.getLogger(TransferService.class.getName());
        this.queue = new LinkedBlockingDeque<>();
        BlockingQueueWorker<TransferRequest> queueWorker = new BlockingQueueWorker<>(this.queue, this::processRequest);
        this.asyncCallbackWorker = Executors.newSingleThreadExecutor();
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

    private Void processRequest(TransferRequest item) {
        this.asyncCallbackWorker.execute(() -> {
            try {
                final StorageService sourceService = getService(item.getSourceWorkspace());
                final StorageService destinationService = getService(item.getDestinationWorkspace());
                final FileObject fileObject = item.getSource();
                final Object source = sourceService.download(Paths.get(fileObject.getRelativePath()));
                final InputStream sourceStream;
                if (source instanceof FileSystemResource) {
                    sourceStream = ((FileSystemResource) source).getInputStream();
                } else {
                    sourceStream = (InputStream) source;
                }
                destinationService.storeFile(sourceStream, fileObject.getSize(), item.getDestinationPath(), fileObject.getRelativePath());
            } catch (Exception e) {
                logger.severe(ExceptionUtils.getStackTrace(logger, e));
            }
        });
        return null;
    }

    public StorageService getService(Workspace workspace) throws Exception {
        final ServiceRegistry<StorageService> registry = ServiceRegistryManager.getInstance().getServiceRegistry(StorageService.class);
        final Set<StorageService> services = registry.getServices();
        StorageService service = services.stream().filter(s -> s.isIntendedFor(workspace.getUrlPrefix())).findFirst().orElse(null);
        if (service == null) {
            throw new IllegalArgumentException("No remote file repository for the given protocol is available");
        }
        // Don't forget that a SPI produces a single instance, and we don't want to re-use it
        service = service.getClass().newInstance();
        service.setup(workspace.getParameters());
        return service;
    }
}