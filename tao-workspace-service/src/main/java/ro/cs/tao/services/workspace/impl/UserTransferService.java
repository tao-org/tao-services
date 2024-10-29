package ro.cs.tao.services.workspace.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.core.io.FileSystemResource;
import ro.cs.tao.ListenableInputStream;
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.messaging.Messaging;
import ro.cs.tao.messaging.ProgressNotifier;
import ro.cs.tao.messaging.Topic;
import ro.cs.tao.security.UserPrincipal;
import ro.cs.tao.services.factory.StorageServiceFactory;
import ro.cs.tao.services.interfaces.StorageService;
import ro.cs.tao.services.model.FileObject;
import ro.cs.tao.services.workspace.model.TransferableItem;
import ro.cs.tao.utils.ExceptionUtils;
import ro.cs.tao.utils.FileUtilities;
import ro.cs.tao.utils.executors.BlockingQueueWorker;
import ro.cs.tao.utils.executors.monitoring.ProgressListener;
import ro.cs.tao.workspaces.Repository;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Logger;

public class UserTransferService {

    private final BlockingQueue<TransferableItem> queue;
    private final Path stateFile;
    private final Principal principal;
    private Timer retryTimer;
    private final BlockingQueueWorker<TransferableItem> queueWorker;

    private final Map<String, Integer> batches;
    private final boolean saveState;
    private ProgressListener progressListener;
    private final Logger logger;


    UserTransferService(Principal user) {
        this.logger = Logger.getLogger(TransferService.class.getName());
        this.queue = new LinkedBlockingDeque<>();
        this.queueWorker = new BlockingQueueWorker<>(this.queue, this::processRequest, 1, "transfer-worker", 250);
        this.principal = user;
        this.stateFile = Paths.get(SystemVariable.ROOT.value()).resolve("transfer").resolve(user.getName()).resolve("transfer_queue.json");
        restoreState();
        this.queueWorker.setStatePersister(this::saveState);
        this.batches = new HashMap<>();
        this.saveState = Boolean.parseBoolean(ConfigurationManager.getInstance().getValue("allow.resumable.transfers", "false"));
        queueWorker.start();
    }

    /**
     * Submits a transfer request to the service queue.
     * @param requests   The request(s)
     */
    public void request(TransferableItem... requests) {
        for (TransferableItem request : requests) {
            final Integer value = this.batches.get(request.getBatch());
            this.batches.put(request.getBatch(), value != null ? value + 1 : 1);
        }
        this.queue.addAll(Arrays.asList(requests));
    }

    private double getBatchProgress(String batch) {
        double value;
        synchronized (this.batches) {
            Integer iCount = this.batches.get(batch);
            if (iCount == null) {
                value = 0.0;
            } else {
                value = 1.0 - ((double) this.queue.stream().map(TransferableItem::getBatch).count() / iCount.doubleValue());
            }
        }
        return value;
    }

    private Void processRequest(TransferableItem item) {
        //this.asyncCallbackWorker.execute(() -> {
        try {
            if (this.progressListener == null) {
                this.progressListener = new ProgressNotifier(this.principal, item.getBatch(),
                        Topic.TRANSFER_PROGRESS, new HashMap<>() {{
                    put("Repository", item.getDestinationRepository().getId());
                }});
                this.progressListener.started(item.getBatch());
            }
            final Repository srcWorkspace = item.getSourceRepository();
            final StorageService sourceService = StorageServiceFactory.getInstance(srcWorkspace);
            ProgressListener listener = this.progressListener.subProgressListener() != null
                                        ? this.progressListener.subProgressListener()
                                        : this.progressListener;
            sourceService.setProgressListener(listener);
            final Repository dstWorkspace = item.getDestinationRepository();
            final StorageService destinationService = StorageServiceFactory.getInstance(dstWorkspace);
            //destinationService.setProgressListener(this.progressListener);
            final FileObject fileObject = item.getSource();
            final Map<String, String> attributes = fileObject.getAttributes();
            String srcPath;
            if (attributes != null && (srcPath = attributes.get("remotePath")) != null) {
                if (!srcPath.startsWith("http")) {
                    srcPath = srcWorkspace.resolve(fileObject.getRelativePath());
                }
            } else {
                srcPath = srcWorkspace.resolve(fileObject.getRelativePath());
            }
            if (this.progressListener != null) {
            	this.progressListener.subActivityStarted(srcPath);
            }
            boolean hasException = false;
            if (srcWorkspace.getId().equals(dstWorkspace.getId())) {
                destinationService.move(srcPath, item.getDestinationPath());
            } else {
                InputStream sourceStream = null;
                try {
                    final Object source = sourceService.download(srcPath);
                    if (source instanceof FileSystemResource) {
                        sourceStream = ((FileSystemResource) source).getInputStream();
                    } else {
                        sourceStream = (InputStream) source;
                    }
                    logger.finest("Begin transferring file " + srcPath);
                    if (fileObject.getSize() > 0) {
                        sourceStream = new ListenableInputStream(sourceStream,
                                                                 fileObject.getSize(),
                                                                 listener);
                    }
                    if (item.isForce() || !destinationService.exists(item.getDestinationPath())) {
                        destinationService.storeFile(sourceStream, fileObject.getSize(), item.getDestinationPath(), srcPath);
                    } else {
                        logger.fine(String.format("File %s exists at the destination and no overwrite flag is set", srcPath));
                    }
                } catch (FileNotFoundException fnfex) {
                    final String msg = "File '" + srcPath + "' was not found in the [" + srcWorkspace.getName() + "] repository";
                    logger.warning(msg);
                    hasException = true;
                    Messaging.send(new UserPrincipal(item.getUser()), Topic.WARNING.value(), this, msg);
                } catch (RuntimeException ex) {
                    final String msg = "File '" + srcPath + "' was not transferred [reason: " + ex.getMessage() + "]";
                    logger.warning(msg);
                    hasException = true;
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
            }
            if (this.progressListener != null) {
            	this.progressListener.subActivityEnded(srcPath, hasException);
            }
            //decrement(item.getBatch());
            if (this.progressListener != null) {
                this.progressListener.notifyProgress(getBatchProgress(item.getBatch()));
            }
            final TransferableItem next = this.queue.peek();
            if (next == null || !item.getBatch().equals(next.getBatch())) {
                if (this.progressListener != null) {
                    this.progressListener.ended(hasException);
                    this.progressListener = null;
                }
                final String msg = "Transfer batch [" + item.getBatch() + "] completed" + (hasException ? " with errors" : "");
                Messaging.send(new UserPrincipal(item.getUser()),
                               hasException ? Topic.WARNING.value() : Topic.INFORMATION.value(),
                               this, msg);
                logger.finest(msg);
            }
        } catch (Exception e) {
            logger.severe(ExceptionUtils.getStackTrace(logger, e));
            if (this.progressListener != null) {
                this.progressListener.ended(false);
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
        if (this.saveState) {
            try {
                FileUtilities.createDirectories(this.stateFile.getParent());
                if (Files.notExists(this.stateFile)) {
                    Files.createFile(this.stateFile);
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
    }

    private synchronized void restoreState() {
        if (this.saveState) {
            try {
                FileUtilities.createDirectories(this.stateFile.getParent());
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
}
