package ro.cs.tao.services.entity.impl;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.services.interfaces.WorkspaceService;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static java.nio.file.StandardWatchEventKinds.*;

@Service("workspaceService")
public class WorkspaceServiceImpl implements WorkspaceService {

    private static final Logger logger = Logger.getLogger(WorkspaceServiceImpl.class.getName());

    private WatchService watcher;
    private Map<WatchKey, Path> keys;

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
     */
    private void registerDirectory(Path dir) throws IOException {
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        keys.put(key, dir);
    }

    /**
     * Register the given directory, and all its sub-directories, with the WatchService.
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

    @Override
    public boolean hasKeys() {
        return !this.keys.isEmpty();
    }

    @Override
    public void watch() {
        WatchKey key;
        try { // wait for key to be signalled
            key = watcher.take();
        } catch (InterruptedException x) {
            return;
        }
        Path dir = keys.get(key);
        if (dir == null) {
            return;
        }

        for (WatchEvent<?> event : key.pollEvents()) {
            WatchEvent.Kind kind = event.kind();

            // Context for directory entry event is the file name of entry
            Path name = ((WatchEvent<Path>)event).context();
            Path child = dir.resolve(name);

            // print out event
            System.out.format("%s: %s\n", event.kind().name(), child);

            // if directory is created, and watching recursively, then register it and its sub-directories
            if (kind == ENTRY_CREATE) {
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
}
