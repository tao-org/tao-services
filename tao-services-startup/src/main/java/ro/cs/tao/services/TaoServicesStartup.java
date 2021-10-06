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
package ro.cs.tao.services;

import org.ggf.drmaa.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import ro.cs.tao.component.RuntimeOptimizer;
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.configuration.ConfigurationProvider;
import ro.cs.tao.datasource.DataSource;
import ro.cs.tao.eodata.OutputDataHandler;
import ro.cs.tao.eodata.metadata.MetadataInspector;
import ro.cs.tao.execution.Executor;
import ro.cs.tao.execution.model.TaskSelector;
import ro.cs.tao.messaging.Messaging;
import ro.cs.tao.messaging.Topic;
import ro.cs.tao.messaging.system.StartupCompletedMessage;
import ro.cs.tao.quota.QuotaManager;
import ro.cs.tao.security.SystemPrincipal;
import ro.cs.tao.services.commons.StartupBase;
import ro.cs.tao.services.startup.LifeCycleProcessor;
import ro.cs.tao.spi.ServiceRegistry;
import ro.cs.tao.spi.ServiceRegistryManager;
import ro.cs.tao.topology.docker.DockerImageInstaller;
import ro.cs.tao.utils.FileUtilities;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Cosmin Cara
 */

@SpringBootApplication
@EnableScheduling
@EnableWebMvc
public class TaoServicesStartup extends StartupBase {
    @Autowired
    private LifeCycleProcessor lifeCycleProcessor;

    private final static Logger logger = Logger.getLogger(TaoServicesStartup.class.getName());
    private static final Map<String, Class> plugins = new LinkedHashMap<String, Class>() {{
        put("Docker plugins", DockerImageInstaller.class);
        put("Datasource plugins", DataSource.class);
        put("DRMAA plugins", SessionFactory.class);
        put("Executor plugins", Executor.class);
        put("Metadata plugins", MetadataInspector.class);
        put("Product plugins", OutputDataHandler.class);
        put("Runtime plugins", RuntimeOptimizer.class);
        put("Orchestrator plugins", TaskSelector.class);
        put("Quota Manager plugins", QuotaManager.class);
    }};

    public static void main(String[] args) throws IOException {
        run(TaoServicesStartup.class, args);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            final ConfigurationProvider cfgManager = ConfigurationManager.getInstance();
            if (cfgManager == null) {
                throw new RuntimeException("Framework configuration unavailable");
            }
            cfgManager.setValue(ConfigurationProvider.APP_HOME, homeDirectory().toString());
            try {
                FileUtilities.ensureExists(Paths.get(SystemVariable.SHARED_WORKSPACE.value()));
                FileUtilities.ensureExists(Paths.get(SystemVariable.SHARED_FILES.value()));
                String tempPath = cfgManager.getValue("spring.servlet.multipart.location", "/mnt/tao/tmp");
                FileUtilities.ensureExists(Paths.get(tempPath));
                Path cachePath = TaoServicesStartup.homeDirectory().resolve("static").resolve("previews");
                FileUtilities.ensureExists(cachePath);
            } catch (IOException e) {
                logger.severe("Cannot create required folders: " + e.getMessage());
                System.exit(1);
            }
            lifeCycleProcessor.activate(() -> {
                for (Map.Entry<String, Class> entry : plugins.entrySet()) {
                    ServiceRegistry<?> registry = ServiceRegistryManager.getInstance().getServiceRegistry(entry.getValue());
                    Set<?> instances = registry.getServices();
                    logger.info(String.format("Installed %s: %s",
                                              entry.getKey(), instances.stream().map(i -> i.getClass().getSimpleName())
                                                      .sorted().collect(Collectors.joining(","))));
                }
                logger.info("Web interface is accessible at https://localhost:" + cfgManager.getValue("server.port"));
                Messaging.send(SystemPrincipal.instance(), Topic.SYSTEM.value(), new StartupCompletedMessage());
            });
        }
    }
}
