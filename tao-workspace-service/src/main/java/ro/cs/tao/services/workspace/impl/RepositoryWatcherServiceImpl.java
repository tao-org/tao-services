package ro.cs.tao.services.workspace.impl;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.UserProvider;
import ro.cs.tao.security.SystemPrincipal;
import ro.cs.tao.services.interfaces.RepositoryWatcherService;
import ro.cs.tao.user.User;
import ro.cs.tao.user.UserStatus;
import ro.cs.tao.utils.FileUtilities;
import ro.cs.tao.utils.executors.MemoryUnit;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Logger;

import static java.nio.file.StandardWatchEventKinds.*;

@Service("repositoryWatcherService")
public class RepositoryWatcherServiceImpl implements RepositoryWatcherService {

    private static final Logger logger = Logger.getLogger(RepositoryWatcherServiceImpl.class.getName());
    private static final int BOUND = 10;
    private static final String WORKSPACE_DIR;
    private static final String ROOT;

    private BlockingQueue<String> eventsQueue;
    private WatchService watcher;
    private Map<WatchKey, Path> keys;

    static {
        ROOT = SystemVariable.ROOT.value().replace("\\", "/");
        WORKSPACE_DIR = ROOT.substring(ROOT.lastIndexOf('/') + 1);
    }

    @Autowired
    private UserProvider userProvider;

    public RepositoryWatcherServiceImpl() {
        try {
            this.watcher = FileSystems.getDefault().newWatchService();
            this.keys = new HashMap<>();
            this.eventsQueue = new LinkedBlockingDeque<>(BOUND);
        } catch (IOException e) {
            logger.severe(String.format("Cannot create new watcher service. Reason: %s",
                    ExceptionUtils.getStackTrace(e)));
        }
    }

    @Override
    public void startWatching() {
        List<User> activeUsers = userProvider.list(UserStatus.ACTIVE);
        if (activeUsers != null) {
            for (User user : activeUsers) {
                registerUser(user.getId());
            }
        }
        try {
            registerDirectory(Paths.get(SystemVariable.ROOT.value()));
        } catch (IOException e) {
            logger.warning(e.getMessage());
        }
        final WatcherMonitor watcherMonitor = new WatcherMonitor();
        watcherMonitor.setName("WatcherMonitor");
        watcherMonitor.start();
        final QueueMonitor monitor = new QueueMonitor();
        monitor.setName("QueueMonitor");
        monitor.start();
        logger.finest("Workspace service initialization completed");
    }

    @Override
    public void stopWatching() {
        try {
            this.watcher.close();
        } catch (IOException ignored) {
        }
    }

    @Override
    public void registerUser(String userId) {
        if (!SystemPrincipal.instance().getName().equals(userId)) {
            final Path rootPath = Paths.get(ROOT);
            try {
                Path userWorkspace = FileUtilities.ensureExists(rootPath.resolve(userId));
                walkAndRegisterDirectories(userWorkspace);
                int usedSpace = getUsedSpace(userId);
                updateUserInputQuota(userId, usedSpace);
            } catch (IOException e) {
                logger.severe(String.format("Failed to watch user workspace [user=%s, reason=%s]%n", userId, e.getMessage()));
            }
        }
    }

    @Override
    public void unregisterUser(String userId) {
        if (!SystemPrincipal.instance().getName().equals(userId)) {
            final Path rootPath = Paths.get(ROOT);
            try {
                Path userWorkspace = FileUtilities.ensureExists(rootPath.resolve(userId));
                walkAndUnregisterDirectories(userWorkspace);
            } catch (IOException e) {
                logger.severe(String.format("Failed to watch user workspace [user=%s, reason=%s]%n", userId, e.getMessage()));
            }
        }
    }

    /**
     * Update the actual input quota of a user
     * @param userId - the user id for which the update needs to be done
     */
    private void updateUserInputQuota(String userId, int usedSpace) {
        if (!SystemPrincipal.instance().getName().equals(userId)) {
            try { // update user quota
                userProvider.updateInputQuota(userId, usedSpace);
                logger.finest(String.format("Input quota update [%s:%dMB]", userId, usedSpace));
            } catch (PersistenceException e) {
                logger.severe(e.getMessage());
            }
        }
    }

    /**
     * Register the given directory with the WatchService
     * @param dir - directory to be registered
     */
    private void registerDirectory(Path dir) throws IOException {
        final WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        keys.put(key, dir);
    }

    /**
     * Unregister the given directory from the WatchService
     * @param dir - directory to be unregistered
     */
    private void unregisterDirectory(Path dir) throws IOException {
        final Iterator<Map.Entry<WatchKey, Path>> iterator = keys.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<WatchKey, Path> next = iterator.next();
            if (next.getValue().equals(dir)) {
                next.getKey().cancel();
                iterator.remove();
                break;
            }
        }
    }

    /**
     * Register the given directory, and all its sub-directories, with the WatchService.
     * @param start - the path to the root directory
     */
    private void walkAndRegisterDirectories(final Path start) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                registerDirectory(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Unregister the given directory, and all its sub-directories, from the WatchService.
     * @param start - the path to the root directory
     */
    private void walkAndUnregisterDirectories(final Path start) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                unregisterDirectory(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Get the user id from the path
     * e.g.: mnt\tao\working_dir\simple should return simple
     * @param path - current given path
     * @return username
     */
    private String getUserId(String path) {
        final StringTokenizer dirTokenizer = new StringTokenizer(path.replace("\\", "/"), "/");
        while (dirTokenizer.hasMoreElements()) {
            String nextDirectory = dirTokenizer.nextToken();
            if (nextDirectory.equalsIgnoreCase(WORKSPACE_DIR)) {
                try {
                    return dirTokenizer.nextToken();
                } catch (NoSuchElementException e) {
                    logger.warning(e.getMessage());
                }
            }
        }
        return null;
    }

    /**
     * Get the used space for a given user workspace
     * @param userId - id of the user
     * @return memory in MB taken up by the current user
     * @throws IOException if the folder is not found
     */
    private int getUsedSpace(String userId) throws IOException {
        if (SystemPrincipal.instance().getName().equals(userId)) {
            return -1;
        }
        final Path userWorkspace = Paths.get(ROOT).resolve(userId);
        if (!Files.exists(userWorkspace)) {
            return -1;
        }
        final long usedSpace = FileUtilities.folderSize(userWorkspace) / MemoryUnit.MB.value();
        return Math.toIntExact(usedSpace);
    }

    private class WatcherMonitor extends Thread {
        /**
         * Watch for any changes in any registered directories.
         * If no change has occurred yet, wait.
         */
        @Override
        public void run() {
            while(true) {
                WatchKey key;
                try { // wait for key to be signalled
                    key = watcher.take();
                    Thread.sleep(5000);
                } catch (InterruptedException x) {
                    logger.finest("Workspace service interrupted");
                    return;
                } catch (ClosedWatchServiceException e) {
                    logger.fine("Workspace service closed");
                    return;
                }
                Path directory = keys.get(key);
                if (directory == null) {
                    continue;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    Path name = ((WatchEvent<Path>) event).context();
                    Path child = directory.resolve(name);

                    //logger.finest(String.format("Filesystem event %s: %s\n", event.kind().name(), child));

                    if (kind == ENTRY_CREATE) { // if directory is created, and watching recursively, then register it and its sub-directories
                        try {
                            if (directory.equals(Paths.get(ROOT))) {
                                final BasicFileAttributes attributes = Files.readAttributes(child, BasicFileAttributes.class);
                                if (attributes.isRegularFile() || Files.isRegularFile(child)) {
                                    logger.warning(String.format("File %s created outside users folders", child));
                                }
                            } else {

                                if (!Files.isSymbolicLink(child)) {
                                    final BasicFileAttributes attributes = Files.readAttributes(child, BasicFileAttributes.class);
                                    if (attributes.isDirectory() || Files.isDirectory(child)) {
                                        walkAndRegisterDirectories(child);
                                    }
                                }
                            }
                        } catch (NoSuchFileException | NotDirectoryException ignored) {
                        // a file may throw this exception if it's not fully "created"
                        } catch (IOException e) {
                            logger.severe(String.format("Cannot register directory %s. Reason: %s",
                                                        child, ExceptionUtils.getStackTrace(e)));
                        }
                    }
                }

                try {
                    eventsQueue.put(directory.toString());
                } catch (InterruptedException e) {
                    logger.severe(String.format("The thread %s has been interrupted. Reason: %s",
                            this.getName(), ExceptionUtils.getStackTrace(e)));
                }

                // reset key and remove from set if directory no longer accessible
                boolean valid = key.reset();
                if (!valid) {
                    keys.remove(key);
                    // all directories are inaccessible
                    if (keys.isEmpty()) {
                        break;
                    }
                }
            }
        }
    }

    private class QueueMonitor extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    String entry = eventsQueue.take();
                    final String userId = getUserId(entry);
                    if (userId != null) {
                        final int usedSpace = getUsedSpace(userId);
                        updateUserInputQuota(userId, usedSpace);
                    }/* else {
                        logger.warning("Cannot determine user from entry '" + entry + "'");
                    }*/
                } catch (InterruptedException e) {
                    logger.severe(String.format("The thread %s has been interrupted. Reason: %s",
                            this.getName(), ExceptionUtils.getStackTrace(e)));
                } catch (IOException | UncheckedIOException e) {
                    logger.warning(e.getMessage());
                }
            }
        }
    }

}
