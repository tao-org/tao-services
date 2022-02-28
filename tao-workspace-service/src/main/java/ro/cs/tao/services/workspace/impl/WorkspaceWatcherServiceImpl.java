package ro.cs.tao.services.workspace.impl;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.execution.monitor.MemoryUnit;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.UserProvider;
import ro.cs.tao.security.SystemPrincipal;
import ro.cs.tao.services.interfaces.WorkspaceWatcherService;
import ro.cs.tao.user.User;
import ro.cs.tao.user.UserStatus;
import ro.cs.tao.utils.FileUtilities;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Logger;

import static java.nio.file.StandardWatchEventKinds.*;

@Service("workspaceWatcherService")
public class WorkspaceWatcherServiceImpl implements WorkspaceWatcherService {

    private static final Logger logger = Logger.getLogger(WorkspaceWatcherServiceImpl.class.getName());
    private static final String WORKING_DIR = "working_dir";
    private static final int BOUND = 10;

    private BlockingQueue<String> eventsQueue;
    private WatchService watcher;
    private Map<WatchKey, Path> keys;

    @Autowired
    private UserProvider userProvider;

    public WorkspaceWatcherServiceImpl() {
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
                if (!SystemPrincipal.instance().getName().equals(user.getUsername())) {
                    registerUser(user.getUsername());
                }
            }
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
    public void registerUser(String userName) {
        final Path rootPath = Paths.get(SystemVariable.ROOT.value());
        try {
            Path userWorkspace = FileUtilities.ensureExists(rootPath.resolve(userName));
            walkAndRegisterDirectories(userWorkspace);
            int usedSpace = getUsedSpace(userName);
            updateUserInputQuota(userName, usedSpace);
        } catch (IOException e) {
            logger.severe(String.format("Failed to watch user workspace [user=%s, reason=%s]%n", userName, e.getMessage()));
        }
    }

    @Override
    public void unregisterUser(String userName) {
        final Path rootPath = Paths.get(SystemVariable.ROOT.value());
        try {
            Path userWorkspace = FileUtilities.ensureExists(rootPath.resolve(userName));
            walkAndUnregisterDirectories(userWorkspace);
        } catch (IOException e) {
            logger.severe(String.format("Failed to watch user workspace [user=%s, reason=%s]%n", userName, e.getMessage()));
        }
    }

    /**
     * Update the actual input quota of a user
     * @param userName - the username for which the update needs to be done
     */
    private void updateUserInputQuota(String userName, int usedSpace) {
        try { // update user quota
            userProvider.updateInputQuota(userName, usedSpace);
            logger.finest(String.format("Input quota update [%s:%dMB]", userName, usedSpace));
        } catch (PersistenceException e) {
            logger.severe(e.getMessage());
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
     * Get the user name from the path
     * e.g.: mnt\tao\working_dir\simple should return simple
     * @param path - current given path
     * @return username
     */
    private String getUserName(String path) {
        final StringTokenizer dirTokenizer = new StringTokenizer(path, File.separator);
        while (dirTokenizer.hasMoreElements()) {
            String nextDirectory = dirTokenizer.nextToken();
            if (nextDirectory.equalsIgnoreCase(WORKING_DIR)) {
                return dirTokenizer.nextToken();
            }
        }
        return null;
    }

    /**
     * Get the used space for a given user workspace
     * @param username - name of the user
     * @return memory in MB taken up by the current user
     * @throws IOException if the folder is not found
     */
    private int getUsedSpace(String username) throws IOException {
        final String userWorkspace = Paths.get(SystemVariable.ROOT.value()).resolve(username).toString();
        final long usedSpace = FileUtilities.folderSize(new File(userWorkspace).toPath()) / ((long) MemoryUnit.MEGABYTE.value());
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
                            if (Files.isDirectory(child)) {
                                walkAndRegisterDirectories(child);
                            }
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
                    final String userName = getUserName(entry);
                    final int usedSpace = getUsedSpace(userName);
                    updateUserInputQuota(userName, usedSpace);
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
