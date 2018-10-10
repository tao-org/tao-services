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
import ro.cs.tao.services.commons.config.ConfigurationFileProcessor;
import ro.cs.tao.spi.ServiceRegistry;
import ro.cs.tao.spi.ServiceRegistryManager;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.Set;
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
        /*Logger.getLogger(StartupBase.class.getName()).info(String.format("Configuration files will be read from %s",
                                                                         configDirectory.toString()));*/
        System.out.println(String.format("Configuration files will be read from %s", configDirectory.toString()));
        if (!Files.exists(configDirectory)) {
            Files.createDirectory(configDirectory);
        }
        ServiceRegistry<ConfigurationFileProcessor> serviceRegistry =
                ServiceRegistryManager.getInstance().getServiceRegistry(ConfigurationFileProcessor.class);
        Set<ConfigurationFileProcessor> configurationFileProcessors = serviceRegistry.getServices();
        for (ConfigurationFileProcessor processor : configurationFileProcessors) {
            allProperties.putAll(processor.processFile(configDirectory));
        }
        try {
            Field field = ConfigurationManager.class.getDeclaredField("configFolder");
            field.setAccessible(true);
            field.set(null, configDirectory);
            field = ConfigurationManager.class.getDeclaredField("settings");
            field.setAccessible(true);
            ((Properties) field.get(ConfigurationManager.getInstance())).putAll(allProperties);
        } catch (Exception e){
            throw new RuntimeException(e);
        }
        return allProperties;
    }

    protected static Class[] detectLaunchers(Class startupClass) {
        ServiceRegistry<ServiceLauncher> registry = ServiceRegistryManager.getInstance().getServiceRegistry(ServiceLauncher.class);
        Set<ServiceLauncher> launchers = registry.getServices();
        //Logger.getLogger(StartupBase.class.getName()).info("Detected service launchers: " + String.join(",", launchers.stream().map(l -> l.getClass().getSimpleName()).sorted().collect(Collectors.toList())));
        System.out.println("Detected service launchers: " + String.join(",", launchers.stream().map(l -> l.getClass().getSimpleName()).sorted().collect(Collectors.toList())));
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
//        Logger.getLogger(StartupBase.class.getName()).info(String.format("Development mode is %s",
//                                                                         Boolean.parseBoolean(ConfigurationManager.getInstance().getValue("tao.dev.mode", "false")) ?
//                                                                                 "ON" : "OFF"));
        System.out.println(String.format("Development mode is %s",
                                         Boolean.parseBoolean(ConfigurationManager.getInstance().getValue("tao.dev.mode", "false")) ?
                                                 "ON" : "OFF"));
        app.run(args);
    }

    public static Path homeDirectory() { return home.getDir().getParentFile().toPath(); }

    public static Properties properties() { return allProperties; }
}
