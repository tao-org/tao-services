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
package ro.cs.tao.services;

import org.springframework.boot.ApplicationHome;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.services.entity.DataServicesLauncher;
import ro.cs.tao.services.monitoring.MonitoringServiceLauncer;
import ro.cs.tao.services.orchestration.OrchestratorLauncher;
import ro.cs.tao.services.progress.ProgressReportLauncher;
import ro.cs.tao.services.query.DataQueryServiceLauncher;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * @author Cosmin Cara
 */

@SpringBootApplication
@EnableScheduling
public class TaoServicesStartup {
    private static final ApplicationHome home;

    static {
        home = new ApplicationHome(TaoServicesStartup.class);
        try {
            Path configDirectory = homeDirectory().resolve("config");
            if (!Files.exists(configDirectory)) {
                Files.createDirectories(configDirectory);
            }
            Field field = ConfigurationManager.class.getDeclaredField("configFolder");
            field.setAccessible(true);
            field.set(null, configDirectory);
            Path configFile = configDirectory.resolve("tao.properties");
            if (!Files.exists(configFile)) {
                byte[] buffer = new byte[1024];
                try (BufferedInputStream is = new BufferedInputStream(ConfigurationManager.class.getResourceAsStream("/ro/cs/tao/configuration/tao.properties"));
                     OutputStream os = new BufferedOutputStream(Files.newOutputStream(configFile))) {
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        os.write(buffer, 0, read);
                    }
                    os.flush();
                }
            }
            field = ConfigurationManager.class.getDeclaredField("settings");
            field.setAccessible(true);
            ((Properties) field.get(ConfigurationManager.getInstance())).load(Files.newInputStream(configFile));
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        //LifeCycleProcessor.activate();
        new SpringApplicationBuilder()
                .sources(TaoServicesStartup.class,
                         MonitoringServiceLauncer.class, DataServicesLauncher.class,
                         DataQueryServiceLauncher.class, ProgressReportLauncher.class,
                         OrchestratorLauncher.class)
                //.bannerMode(Banner.Mode.OFF)
                .build()
                .run(args);
    }

    private static Path homeDirectory() { return home.getDir().getParentFile().toPath(); }
}
