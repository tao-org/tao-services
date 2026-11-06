package ro.cs.tao.services.commons.config;

import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.utils.Tuple;
import ro.cs.tao.utils.executors.MemoryUnit;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Defines an interface for processing files by mapping predefined resource paths to a target
 * directory and ensuring that these files exist in the specified location.
 *
 * Implementations of this interface must define the `getFileMap` method, which provides a
 * mapping of resource identifiers (keys) to their corresponding resource paths (values).
 * The `processFiles` method uses this mapping to copy resources from their source location
 * to the target directory if they do not already exist.
 *
 * The default implementation of `processFiles` performs the following tasks:
 * - Reads the file mapping provided by the implementing class's `getFileMap` method.
 * - Checks if each target file exists in the specified directory.
 * - If a file is missing, it retrieves the corresponding resource, reads its content, and
 *   writes it to the target directory.
 * - Returns a list of paths for all processed files in the target directory.
 *
 * Methods:
 * - `getFileMap`: Abstract method that must be implemented to provide a map of file names
 *   to their resource paths.
 * - `processFiles`: Default method for handling file processing, which handles the retrieval,
 *   validation, and copying of resources and returns the resulting file paths.
 *
 * Error Handling:
 * - If a resource cannot be found or read during processing, an error is logged, but the
 *   method will continue processing other files.
 */
public interface FileProcessor {
    Map<String, Tuple<String, FileTime>> getFileMap();
    default List<Path> processFiles(Path configDirectory) throws IOException {
        final Map<String, Tuple<String, FileTime>> map = getFileMap();
        List<Path> files = null;
        if (map != null) {
            files = new ArrayList<>();
            for (Map.Entry<String, Tuple<String, FileTime>> entry : map.entrySet()) {
                final Path configFile = configDirectory.resolve(entry.getKey());
                try {
                    if (!Files.exists(configFile) || !configFile.toString().endsWith(".properties")) {
                        Files.createDirectories(configFile.getParent());
                        final Tuple<String, FileTime> value = entry.getValue();
                        // Only overwrite if newer
                        if (!Files.exists(configFile) || value.getKeyTwo().compareTo(Files.getLastModifiedTime(configFile)) > 0) {
                            final byte[] buffer = new byte[(int) MemoryUnit.KB.value()];
                            try (BufferedInputStream is = new BufferedInputStream(ConfigurationManager.class.getResourceAsStream(value.getKeyOne()));
                                 OutputStream os = new BufferedOutputStream(Files.newOutputStream(configFile,
                                                                                                  StandardOpenOption.CREATE,
                                                                                                  StandardOpenOption.TRUNCATE_EXISTING,
                                                                                                  StandardOpenOption.WRITE))) {
                                int read;
                                while ((read = is.read(buffer)) != -1) {
                                    os.write(buffer, 0, read);
                                }
                                os.flush();
                                Logger.getLogger(getClass().getName()).finest(configFile.getFileName() + " updated");
                            } catch (Exception ex) {
                                Logger.getLogger(getClass().getName()).severe("Cannot read or find resource " + value.getKeyOne());
                            }
                        }
                    }
                    files.add(configFile);
                } catch (Exception ex) {
                    Logger.getLogger(getClass().getName()).severe(ex.getMessage());
                }
            }
        }
        return files;
    }
}
