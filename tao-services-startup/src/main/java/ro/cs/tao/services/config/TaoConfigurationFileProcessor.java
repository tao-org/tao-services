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

package ro.cs.tao.services.config;

import ro.cs.tao.services.commons.config.ConfigurationFileProcessor;
import ro.cs.tao.utils.Tuple;

import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of the {@code ConfigurationFileProcessor} interface for processing
 * specific configuration files related to the TAO application.
 *
 * This class provides the mapping of configuration file names to their corresponding
 * resource paths and last modified times. It ensures that the specified
 * configuration files can be processed and made available in the desired target
 * directory.
 *
 * The file map includes predefined paths for the following configuration files:
 * - `application.properties`: Maps to a Spring configuration file located at
 *   `/config/spring.properties`.
 * - `tao.properties`: Maps to a TAO-specific configuration file located at
 *   `/ro/cs/tao/configuration/tao.properties`.
 *
 * Both entries in the file map include their associated last modified timestamps.
 * These timestamps can be used to determine whether to overwrite existing files
 * based on their modification times.
 *
 * Features:
 * - Retrieves a map of configuration file names to their respective resource paths
 *   and last modified times.
 * - Enables integration with the default functionality defined in the
 *   {@code ConfigurationFileProcessor} and {@code FileProcessor} interfaces.
 *
 * Thread-Safety:
 * - This class is not thread-safe as it uses a non-thread-safe {@code HashMap}.
 *
 * Error Handling:
 * - The methods rely on valid file mappings and may fail if the provided resource paths
 *   or timestamps are invalid.
 *
 * See Also:
 * - {@code ConfigurationFileProcessor}: Interface providing default configuration file processing methods.
 * - {@code FileProcessor}: Parent interface defining file mapping and processing methods.
 */
public class TaoConfigurationFileProcessor implements ConfigurationFileProcessor {

    @Override
    public Map<String, Tuple<String, FileTime>> getFileMap() {
        return new HashMap<String, Tuple<String, FileTime>>() {{
            put("application.properties", new Tuple<>("/config/spring.properties",
                                                      FileTime.from(LocalDateTime.now().toInstant(ZoneOffset.UTC))));
            put("tao.properties", new Tuple<>("/ro/cs/tao/configuration/tao.properties",
                                              FileTime.from(LocalDateTime.now().toInstant(ZoneOffset.UTC))));
        }};
    }

}
