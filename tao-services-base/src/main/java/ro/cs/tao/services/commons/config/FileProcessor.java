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
                    byte[] buffer = new byte[MemoryUnit.KB.value().intValue()];
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
