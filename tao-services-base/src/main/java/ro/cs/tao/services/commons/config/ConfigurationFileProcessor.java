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

/**
 * Interface for processing configuration files in a specified directory.
 *
 * This interface extends the {@code FileProcessor} interface to provide additional
 * functionality for processing configuration files and aggregating their properties.
 * It introduces default methods for handling configuration-specific processing,
 * such as loading `.properties` files and performing custom configuration tasks.
 *
 * The main features of this interface include:
 *
 * - Aggregating properties from configuration files with the `.properties` file extension
 *   located in a specified directory.
 * - Allowing subclasses to define custom behavior for additional configuration
 *   via the {@code performAdditionalConfiguration} method.
 *
 * Subclasses must implement the {@code getFileMap} method inherited from the
 * {@code FileProcessor} interface to define the mapping of expected configuration
 * files and their associated resource paths.
 *
 * Methods:
 * - {@code processConfigFiles}: Default method to process all configuration files
 *   in a given directory. Filters for `.properties` files, loads their content into
 *   a {@code Properties} object, and allows for additional processing via a hook method.
 * - {@code performAdditionalConfiguration}: Default hook method providing a way for subclasses
 *   to perform implementation-specific configuration tasks. This method is a no-op by
 *   default and can be overridden as needed.
 *
 * Error Handling:
 * - If any I/O error occurs while accessing the configuration files, it will throw an
 *   {@code IOException}.
 */
public interface ConfigurationFileProcessor extends FileProcessor {
    /**
     * Processes all configuration files in the specified directory, aggregates their properties,
     * and performs additional configuration steps as defined by the implementation.
     *
     * This method filters files in the given directory for those with a `.properties` extension,
     * loads their content into a {@code Properties} object, and allows for further implementation-specific
     * configuration to be applied.
     *
     * @param configDirectory the directory containing the configuration files to process
     * @return a {@code Properties} object containing all aggregated properties from the configuration files
     * @throws IOException if an I/O error occurs while reading the configuration files
     */
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

    /**
     * Performs additional configuration processing specific to an implementation.
     * This method acts as a hook for subclasses to define their own
     * configuration processing logic after the initial configuration files
     * have been loaded and processed.
     *
     * @param configDirectory the directory where configuration files are located
     * @param properties the aggregated properties loaded from the configuration files
     */
    default void performAdditionalConfiguration(Path configDirectory, Properties properties) {
    }
}
