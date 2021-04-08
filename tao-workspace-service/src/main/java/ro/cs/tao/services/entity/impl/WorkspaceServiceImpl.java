package ro.cs.tao.services.entity.impl;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.execution.monitor.MemoryUnit;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.UserProvider;
import ro.cs.tao.services.interfaces.WorkspaceService;
import ro.cs.tao.utils.FileUtilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import static java.nio.file.StandardWatchEventKinds.*;

@Service("workspaceService")
public class WorkspaceServiceImpl implements WorkspaceService {

    @Autowired
    UserProvider userProvider;

    private static final Logger logger = Logger.getLogger(WorkspaceServiceImpl.class.getName());

    private WatchService watcher;
    private Map<WatchKey, Path> keys;
    private final String WORKING_DIR = "working_dir";

    @Autowired
    public WorkspaceServiceImpl() {
        try {
            watcher = FileSystems.getDefault().newWatchService();
            this.keys = new HashMap<>();
        } catch (IOException e) {
            logger.severe(String.format("Cannot create  new watcher service. Reason: %s",
                    ExceptionUtils.getStackTrace(e)));
        }
    }

    /**
     * Register the given directory with the WatchService
     * @param dir - directory to be registered
     */
    private void registerDirectory(Path dir) throws IOException {
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        keys.put(key, dir);
    }

    /**
     * Register the given directory, and all its sub-directories, with the WatchService.
     * @param start - the path to the root directory
     */
    @Override
    public void walkAndRegisterDirectories(final Path start) throws IOException {
        // register directory and sub-directories
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                registerDirectory(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Watch for any changes in any registered directories.
     * If no change has occurred yet, wait.
     */
    @Override
    public void watch() {
        WatchKey key;
        try { // wait for key to be signalled
            key = watcher.take();
        } catch (InterruptedException x) {
            return;
        }
        Path directory = keys.get(key);
        if (directory == null) {
            return;
        }

        for (WatchEvent<?> event : key.pollEvents()) {
            WatchEvent.Kind kind = event.kind();
            Path name = ((WatchEvent<Path>)event).context();
            Path child = directory.resolve(name);

            System.out.format("%s: %s\n", event.kind().name(), child);

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

        // reset key and remove from set if directory no longer accessible
        boolean valid = key.reset();
        if (!valid) {
            keys.remove(key);
            // all directories are inaccessible
            if (keys.isEmpty()) {
                return;
            }
        }
    }

    @Override
    public void updateUserInputQuota(String userName) {
        try { // update user quota
            int usedSpace = getUsedSpace(userName);
            userProvider.updateInputQuota(userName, usedSpace);
        } catch (PersistenceException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the user name from the path
     * e.g.: mnt\tao\working_dir\simpleuser should return simpleuser
     * @param path - current given path
     * @return username or null
     */
    private String getUserName(Path path) {
        StringTokenizer dirTokenizer = new StringTokenizer(path.toString(), File.separator);
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
     * @throws IOException
     */
    private int getUsedSpace(String username) throws IOException {
        final String userWorkspace = Paths.get(SystemVariable.ROOT.value()).resolve(username).toString();
        // compute used space in MB
        final long usedSpace = FileUtilities.folderSize(new File(userWorkspace).toPath()) / ((long) MemoryUnit.MEGABYTE.value());
        return Math.toIntExact(usedSpace);
    }
}
