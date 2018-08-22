/*
 * Copyright (C) 2017 CS ROMANIA
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package ro.cs.tao.services.commons.config;

import ro.cs.tao.configuration.ConfigurationManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public interface ConfigurationFileProcessor {
    String getConfigFileName();
    String getConfigFileResourceLocation();
    default Properties processFile(Path configDirectory) throws IOException {
        Path configFile = configDirectory.resolve(getConfigFileName());
        if (!Files.exists(configFile)) {
            byte[] buffer = new byte[1024];
            try (BufferedInputStream is = new BufferedInputStream(ConfigurationManager.class.getResourceAsStream(getConfigFileResourceLocation()));
                 OutputStream os = new BufferedOutputStream(Files.newOutputStream(configFile))) {
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
        }
        Properties properties = new Properties();
        properties.load(Files.newInputStream(configFile));
        performAdditionalConfiguration(configDirectory, properties);
        return properties;
    }
    default void performAdditionalConfiguration(Path configDirectory, Properties properties) {
    }
}
