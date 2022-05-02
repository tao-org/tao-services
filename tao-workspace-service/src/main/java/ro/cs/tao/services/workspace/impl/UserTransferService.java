package ro.cs.tao.services.workspace.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.core.io.FileSystemResource;
import ro.cs.tao.ProgressListener;
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.messaging.ProgressNotifier;
import ro.cs.tao.messaging.Topic;
import ro.cs.tao.services.factory.StorageServiceFactory;
import ro.cs.tao.services.interfaces.StorageService;
import ro.cs.tao.services.model.FileObject;
import ro.cs.tao.services.workspace.model.TransferableItem;
import ro.cs.tao.utils.ExceptionUtils;
import ro.cs.tao.utils.executors.BlockingQueueWorker;
import ro.cs.tao.workspaces.Repository;

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


    UserTransferService(Principal user) {
        this.logger = Logger.getLogger(TransferService.class.getName());
        this.queue = new LinkedBlockingDeque<>();
        this.queueWorker = new BlockingQueueWorker<>(this.queue, this::processRequest);
        this.principal = user;
        this.stateFile = Paths.get(SystemVariable.ROOT.value()).resolve("transfer").resolve(user.getName()).resolve("transfer_queue.json");
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
            final Repository srcWorkspace = item.getSourceWorkspace();
            final StorageService sourceService = StorageServiceFactory.getInstance(srcWorkspace);
            final Repository dstWorkspace = item.getDestinationWorkspace();
            final StorageService destinationService = StorageServiceFactory.getInstance(dstWorkspace);
            destinationService.setProgressListener(this.progressListener);
            final FileObject fileObject = item.getSource();
            final String srcPath = srcWorkspace.resolve(fileObject.getRelativePath());
            this.progressListener.subActivityStarted(srcPath);
            InputStream sourceStream = null;
            try {
                final Object source = sourceService.download(srcPath);
                if (source instanceof FileSystemResource) {
                    sourceStream = ((FileSystemResource) source).getInputStream();
                } else {
                    sourceStream = (InputStream) source;
                }
                logger.finest("Begin transferring file " + srcPath);
                if (item.isForce() || !destinationService.exists(item.getDestinationPath())) {
                    destinationService.storeFile(sourceStream, fileObject.getSize(), item.getDestinationPath(), srcPath);
                } else {
                    logger.fine(String.format("File %s exists at the destination and no overwrite flag is set", srcPath));
                }
            } finally {
                if (sourceStream != null) {
                    sourceStream.close();
                }
            }
            logger.finest("End transferring file " + srcPath);
            if (item.isMove()) {
                sourceService.remove(fileObject.getRelativePath());
                logger.finest("Removed file " + srcPath);
            }
            this.progressListener.subActivityEnded(srcPath);
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
                final ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                mapper.writerFor(requests.getClass()).writeValue(stream, requests);
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
                    final ObjectMapper mapper = new ObjectMapper();
                    mapper.registerModule(new JavaTimeModule());
                    final List<TransferableItem> requests = mapper.readerForListOf(TransferableItem.class)
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
