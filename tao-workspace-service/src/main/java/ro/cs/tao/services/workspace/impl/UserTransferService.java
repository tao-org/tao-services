package ro.cs.tao.services.workspace.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.FileSystemResource;
import ro.cs.tao.ProgressListener;
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.messaging.ProgressNotifier;
import ro.cs.tao.messaging.Topic;
import ro.cs.tao.services.entity.impl.FileStorageService;
import ro.cs.tao.services.factory.StorageServiceFactory;
import ro.cs.tao.services.interfaces.StorageService;
import ro.cs.tao.services.model.FileObject;
import ro.cs.tao.services.workspace.model.TransferableItem;
import ro.cs.tao.utils.ExceptionUtils;
import ro.cs.tao.utils.executors.BlockingQueueWorker;
import ro.cs.tao.workspaces.Workspace;
import ro.cs.tao.workspaces.WorkspaceType;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Logger;

public class UserTransferService {

    private final BlockingQueue<TransferableItem> queue;
    private final Path stateFile;
    private final Principal principal;
    private Timer retryTimer;
    private final BlockingQueueWorker<TransferableItem> queueWorker;
    private ProgressListener progressListener;
    private final Logger logger;

    private final FileStorageService fileStorageService;

    UserTransferService(Principal user, FileStorageService fileService) {
        this.fileStorageService = fileService;
        this.logger = Logger.getLogger(TransferService.class.getName());
        this.queue = new LinkedBlockingDeque<>();
        this.queueWorker = new BlockingQueueWorker<>(this.queue, this::processRequest);
        this.principal = user;
        this.stateFile = Paths.get(SystemVariable.ROOT.value()).resolve(user.getName()).resolve("transfer").resolve("transfer_queue.json");
        restoreState();
        this.queueWorker.setStatePersister(this::saveState);
        queueWorker.start();
    }

    /**
     * Submits a transfer request to the service queue.
     * @param requests   The request(s)
     */
    public void request(TransferableItem... requests) {
        try {
            if (requests.length == 1) {
                this.queue.put(requests[0]);
            } else {
                this.queue.addAll(Arrays.asList(requests));
            }
        } catch (InterruptedException e) {
            logger.severe(e.getMessage());
        }
    }

    private Void processRequest(TransferableItem item) {
        //this.asyncCallbackWorker.execute(() -> {
        try {
            if (this.progressListener == null) {
                this.progressListener = new ProgressNotifier(this.principal, item.getBatch(), Topic.TRANSFER_PROGRESS);
                this.progressListener.started(item.getBatch());
            }
            final Workspace srcWorkspace = item.getSourceWorkspace();
            final StorageService sourceService = srcWorkspace.getType() == WorkspaceType.LOCAL
                    ? fileStorageService
                    : StorageServiceFactory.getInstance(srcWorkspace);
            final Workspace dstWorkspace = item.getDestinationWorkspace();
            final StorageService destinationService = dstWorkspace.getType() == WorkspaceType.LOCAL
                    ? fileStorageService
                    : StorageServiceFactory.getInstance(dstWorkspace);
            destinationService.setProgressListener(this.progressListener);
            final FileObject fileObject = item.getSource();
            final String relativePath = fileObject.getRelativePath();
            this.progressListener.subActivityStarted(relativePath);
            InputStream sourceStream = null;
            try {
                final Object source = sourceService.download(Paths.get(relativePath));
                if (source instanceof FileSystemResource) {
                    sourceStream = ((FileSystemResource) source).getInputStream();
                } else {
                    sourceStream = (InputStream) source;
                }
                logger.finest("Begin transferring file " + relativePath);
                if (item.isForce() || !destinationService.exists(dstWorkspace, item.getDestinationPath())) {
                    destinationService.storeFile(sourceStream, fileObject.getSize(), dstWorkspace, item.getDestinationPath(), relativePath);
                } else {
                    logger.fine(String.format("File %s exists at the destination and no overwrite flag is set", relativePath));
                }
            } finally {
                if (sourceStream != null) {
                    sourceStream.close();
                }
            }
            logger.finest("End transferring file " + relativePath);
            if (item.isMove()) {
                sourceService.remove(srcWorkspace, relativePath);
                logger.finest("Removed file " + relativePath);
            }
            this.progressListener.subActivityEnded(relativePath);
            final TransferableItem next = this.queue.peek();
            if (next == null || !item.getBatch().equals(next.getBatch())) {
                this.progressListener.ended();
                this.progressListener = null;
                logger.finest("Transfer batch [" + item.getBatch() + "] completed");
            }
        } catch (Exception e) {
            logger.severe(ExceptionUtils.getStackTrace(logger, e));
            if (this.progressListener != null) {
                this.progressListener.ended();
            }
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
                UserTransferService.this.queueWorker.setPaused(false);
            }
        }, 300 * 1000);
    }

    private void saveState(BlockingQueue<TransferableItem> queue) {
        try {
            Files.createDirectories(this.stateFile.getParent());
            if (Files.exists(this.stateFile)) {
                Files.delete(this.stateFile);
            }
            final TransferableItem[] requests = queue.toArray(new TransferableItem[0]);
            try (OutputStream stream = Files.newOutputStream(this.stateFile)) {
                new ObjectMapper().writerFor(requests.getClass()).writeValue(stream, requests);
                stream.flush();
            }
            logger.finest(String.format("Transfer queue state saved (%d items)", requests.length));
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
    }

    private synchronized void restoreState() {
        try {
            Files.createDirectories(this.stateFile.getParent());
            if (Files.exists(this.stateFile)) {
                this.queue.clear();
                try (InputStream stream = Files.newInputStream(this.stateFile)) {
                    final List<TransferableItem> requests = new ObjectMapper().readerForListOf(TransferableItem.class)
                            .readValue(stream);
                    if (requests != null) {
                        this.queue.addAll(requests);
                        logger.finest(String.format("Transfer queue state restored (%d items)", requests.size()));
                    }
                }
            }
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
    }
}
