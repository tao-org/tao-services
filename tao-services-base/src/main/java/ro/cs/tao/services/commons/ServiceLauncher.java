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

package ro.cs.tao.services.commons;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.services.commons.config.FileProcessor;
import ro.cs.tao.services.commons.menu.ServiceEntry;
import ro.cs.tao.utils.Tuple;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

/**
 * Defines a contract for launching a service within the application.
 * Specialisation of this class are responsible for providing
 * a unique name that represents the service being launched.
 *
 * This class is typically extended by classes that serve
 * as entry points for different modular services in the application.
 */
public abstract class ServiceLauncher implements FileProcessor, ApplicationListener {

    protected Logger logger;

    public ServiceLauncher() {
        logger = Logger.getLogger(getClass().getName());
    }

    /**
     * Retrieves the unique identifier or name of the service associated with this implementation.
     * The service name is typically used to distinguish this service from others within the application context.
     *
     * @return the unique name of the service as a {@code String}
     */
    public abstract String serviceName();

    /**
     * Retrieves a sorted list of service entries. The entries are obtained
     * from the {@code exportedEntries} method and are sorted in ascending order
     * based on their {@code position}.
     *
     * @return a {@code List<ServiceEntry>} containing the sorted service entries
     */
    public List<ServiceEntry> getEntries() {
        final List<ServiceEntry> entries = exportedEntries();
        if (!entries.isEmpty()) {
            entries.sort(Comparator.comparing(ServiceEntry::getPosition));
        }
        return entries;
    }

    /**
     * Creates a list of service entries that are exported by the implementation.
     * The returned list typically consists of {@code ServiceEntry} objects,
     * each representing a UI menu entry with specific attributes such as name, role,
     * category, and position.
     *
     * @return a {@code List<ServiceEntry>} containing the exported service entries
     */
    protected List<ServiceEntry> exportedEntries() {
        return List.of();
    }

    @Override
    public Map<String, Tuple<String, FileTime>> getFileMap() {
        final String jarFilePath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        final String folderPath = "web/";
        final Map<String, Tuple<String, FileTime>> files = new LinkedHashMap<>();
        try (JarFile jarFile = new JarFile(jarFilePath)) {
            final Enumeration<JarEntry> entries = jarFile.entries();
            JarEntry entry;
            while (entries.hasMoreElements()) {
                entry = entries.nextElement();
                if (entry.getName().startsWith(folderPath) && !entry.isDirectory()) {
                    files.put(entry.getName().replace(folderPath,""),
                              new Tuple<>("/" + entry.getName(), entry.getLastModifiedTime()));
                }
            }
        } catch (IOException e) {
            logger.severe(e.getMessage());
        }
        return files;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            final Path sitePath = Path.of(ConfigurationManager.getInstance().getValue("site.path"));
            try {
                processFiles(sitePath);
            } catch (IOException e) {
                logger.warning(e.getMessage());
            }
        }
    }
}
