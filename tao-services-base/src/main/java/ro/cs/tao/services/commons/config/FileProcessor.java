package ro.cs.tao.services.commons.config;

import ro.cs.tao.configuration.TaoConfigurationProvider;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public interface FileProcessor {
    String getFileName();
    String getFileResourceLocation();
    default Path processFile(Path configDirectory) throws IOException {
        Path configFile = configDirectory.resolve(getFileName());
        if (!Files.exists(configFile)) {
            byte[] buffer = new byte[1024];
            try (BufferedInputStream is = new BufferedInputStream(TaoConfigurationProvider.class.getResourceAsStream(getFileResourceLocation()));
                 OutputStream os = new BufferedOutputStream(Files.newOutputStream(configFile))) {
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
        }
        return configFile;
    }
}
