package ro.cs.tao.services.commons.config;

import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.utils.executors.MemoryUnit;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
    Map<String, String> getFileMap();
    default List<Path> processFiles(Path configDirectory) throws IOException {
        final Map<String, String> map = getFileMap();
        List<Path> files = null;
        if (map != null) {
            files = new ArrayList<>();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                final Path configFile = configDirectory.resolve(entry.getKey());
                if (!Files.exists(configFile)) {
                    byte[] buffer = new byte[(int) MemoryUnit.KB.value()];
                    try (BufferedInputStream is = new BufferedInputStream(ConfigurationManager.class.getResourceAsStream(entry.getValue()));
                         OutputStream os = new BufferedOutputStream(Files.newOutputStream(configFile))) {
                        int read;
                        while ((read = is.read(buffer)) != -1) {
                            os.write(buffer, 0, read);
                        }
                        os.flush();
                    } catch (Exception ex) {
                        Logger.getLogger(getClass().getName()).severe("Cannot read or find resource " + entry.getValue());
                    }
                }
                files.add(configFile);
            }
        }
        return files;
    }
}
