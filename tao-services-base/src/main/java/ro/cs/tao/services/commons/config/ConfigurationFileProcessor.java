/*
 * Copyright (C) 2018 CS ROMANIA
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

import ro.cs.tao.utils.FileUtilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

public interface ConfigurationFileProcessor extends FileProcessor {
    default Properties processConfigFiles(Path configDirectory) throws IOException {
        List<Path> configFiles = processFiles(configDirectory);
        final Properties properties = new Properties();
        for (Path configFile : configFiles) {
            if (".properties".equals(FileUtilities.getExtension(configFile))) {
                Properties fileProps = new Properties();
                fileProps.load(Files.newInputStream(configFile));
                properties.putAll(fileProps);
            }
        }
        performAdditionalConfiguration(configDirectory, properties);
        return properties;
    }
    default void performAdditionalConfiguration(Path configDirectory, Properties properties) {
    }
}
