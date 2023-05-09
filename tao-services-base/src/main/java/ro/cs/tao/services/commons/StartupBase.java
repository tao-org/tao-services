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

import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.context.ApplicationListener;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.configuration.ConfigurationProvider;
import ro.cs.tao.services.commons.config.ConfigurationFileProcessor;
import ro.cs.tao.services.commons.config.FileProcessor;
import ro.cs.tao.spi.ServiceRegistry;
import ro.cs.tao.spi.ServiceRegistryManager;
import ro.cs.tao.utils.executors.Executor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public abstract class StartupBase implements ApplicationListener {
    private static ApplicationHome home;
    private static final Properties allProperties = new Properties();
    protected final ExecutorService backgroundWorker = Executors.newSingleThreadExecutor();

    protected static Properties initialize() throws IOException {
        home = new ApplicationHome();
        Path configDirectory = homeDirectory().resolve("config");
        Files.createDirectories(configDirectory);
        Path scriptsDirectory = homeDirectory().resolve("scripts");
        Files.createDirectories(scriptsDirectory);
        System.out.printf("Configuration files will be read from %s%n", configDirectory.toString());
        if (!Files.exists(configDirectory)) {
            Files.createDirectory(configDirectory);
        }
        ServiceRegistry<FileProcessor> serviceRegistry =
                ServiceRegistryManager.getInstance().getServiceRegistry(FileProcessor.class);
        Set<FileProcessor> configurationFileProcessors = serviceRegistry.getServices();
        for (FileProcessor processor : configurationFileProcessors) {
            if (processor instanceof ConfigurationFileProcessor) {
                allProperties.putAll(((ConfigurationFileProcessor) processor).processConfigFiles(configDirectory));
            } else {
                processor.processFiles(scriptsDirectory);
            }
        }
        if (ConfigurationManager.getInstance() == null) {
            final ServiceLoader<ConfigurationProvider> loader = ServiceLoader.load(ConfigurationProvider.class);
            final Iterator<ConfigurationProvider> iterator = loader.iterator();
            if (iterator.hasNext()) {
                ConfigurationManager.setConfigurationProvider(iterator.next());
            }
        }
        final ConfigurationProvider configurationProvider = ConfigurationManager.getInstance();
        configurationProvider.setValue(ConfigurationProvider.APP_HOME, homeDirectory().toString());
        configurationProvider.setConfigurationFolder(configDirectory);
        configurationProvider.setScriptsFolder(scriptsDirectory);
        configurationProvider.putAll(allProperties);
        configurationProvider.setSystemEnvironment(System.getenv());
        Executor.setEnvironment(System.getenv());
        StringBuilder builder = new StringBuilder();
        builder.append("Active logging levels: ");
        allProperties.entrySet().stream()
                .filter(e -> e.getKey().toString().startsWith("logging.level."))
                .map(e -> new AbstractMap.SimpleEntry<String, String>(e.getKey().toString(), e.getValue().toString()) {})
                .sorted(Map.Entry.comparingByKey()).forEach(e -> {
            builder.append(e.getKey().replace("logging.level.", "")).append(" -> ").append(e.getValue()).append(", ");
        });
        builder.setLength(builder.length() - 1);
        System.out.println(builder);
        return allProperties;
    }

    protected static Class[] detectLaunchers(Class startupClass) {
        ServiceRegistry<ServiceLauncher> registry = ServiceRegistryManager.getInstance().getServiceRegistry(ServiceLauncher.class);
        Set<ServiceLauncher> launchers = registry.getServices();
        System.out.println("Installed services: " + launchers.stream().map(ServiceLauncher::serviceName).sorted().collect(Collectors.joining(",")));
        List<Class> classes = launchers.stream().map(ServiceLauncher::getClass).collect(Collectors.toList());
        classes.add(0, startupClass);
        return classes.toArray(new Class[0]);
    }

    public static void run(Class startupClass, String[] args) throws IOException {
        SpringApplication app = new SpringApplicationBuilder()
                                        .profiles("server")
                                        .sources(detectLaunchers(startupClass))
                                        .build();
        app.setDefaultProperties(initialize());
        if (Boolean.parseBoolean(ConfigurationManager.getInstance().getValue("tao.dev.mode", "false"))) {
            System.out.println("Development mode is ON");
        }
        app.run(args);
    }

    public static Path homeDirectory() { return home.getDir().getParentFile().toPath(); }

    public static Properties properties() { return allProperties; }
}
