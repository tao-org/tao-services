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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationHome;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.annotation.EnableScheduling;
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.datasource.DataSourceManager;
import ro.cs.tao.datasource.remote.FetchMode;
import ro.cs.tao.messaging.Messaging;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.admin.service.AdministrationServiceLauncher;
import ro.cs.tao.services.auth.service.AuthenticationServiceLauncher;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.entity.DataServicesLauncher;
import ro.cs.tao.services.interfaces.ContainerService;
import ro.cs.tao.services.model.monitoring.OSRuntimeInfo;
import ro.cs.tao.services.monitoring.MonitoringServiceLauncer;
import ro.cs.tao.services.orchestration.OrchestratorLauncher;
import ro.cs.tao.services.progress.ProgressReportLauncher;
import ro.cs.tao.services.query.DataQueryServiceLauncher;
import ro.cs.tao.services.security.CustomAuthenticationProvider;
import ro.cs.tao.services.security.SpringSessionProvider;
import ro.cs.tao.services.security.TaoLocalLoginModule;
import ro.cs.tao.services.user.service.UserServiceLauncher;
import ro.cs.tao.topology.NodeDescription;
import ro.cs.tao.topology.TopologyManager;
import ro.cs.tao.topology.docker.DockerImageInstaller;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * @author Cosmin Cara
 */

@SpringBootApplication
@EnableScheduling
public class TaoServicesStartup implements ApplicationListener {
    private static final ApplicationHome home;
    private final ExecutorService backgroundWorker = Executors.newSingleThreadExecutor();

    @Autowired
    private PersistenceManager persistenceManager;

    @Autowired
    private ContainerService containerService;

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
                         OrchestratorLauncher.class, AuthenticationServiceLauncher.class,
                         UserServiceLauncher.class, AdministrationServiceLauncher.class)
                .build()
                .run(args);
    }

    static Path homeDirectory() { return home.getDir().getParentFile().toPath(); }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            BaseController.setPersistenceManager(this.persistenceManager);
            Messaging.setPersister(this.persistenceManager);
            SpringSessionProvider.setPersistenceManager(this.persistenceManager);
            SessionStore.setSessionContextProvider(new SpringSessionProvider());
            TaoLocalLoginModule.setPersistenceManager(this.persistenceManager);
            CustomAuthenticationProvider.setPersistenceManager(this.persistenceManager);
            updateLocalhost();
            backgroundWorker.submit(this::registerEmbeddedContainers);
            backgroundWorker.submit(this::registerDataSourceComponents);
            try {
                Files.createDirectories(Paths.get(SystemVariable.SHARED_WORKSPACE.value()));
                Files.createDirectories(Paths.get(SystemVariable.SHARED_FILES.value()));
            } catch (IOException e) {
                Logger.getLogger(TaoServicesStartup.class.getName()).severe("Cannot create required folders: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    private void updateLocalhost() {
        Logger logger = Logger.getLogger(TaoServicesStartup.class.getName());
        NodeDescription node = TopologyManager.getInstance().get("localhost");
        if (node != null) {
            try {
                String masterHost = InetAddress.getLocalHost().getHostName();
                NodeDescription master = persistenceManager.getNodeByHostName(masterHost);
                if (master == null) {
                    master = new NodeDescription();
                    master.setHostName(masterHost);
                    OSRuntimeInfo inspector = OSRuntimeInfo.createInspector(master);
                    String user = ConfigurationManager.getInstance().getValue("topology.master.user", node.getUserName());
                    master.setUserName(user);
                    String pwd = ConfigurationManager.getInstance().getValue("topology.master.password", node.getUserPass());
                    master.setUserPass(pwd);
                    master.setDescription(node.getDescription());
                    master.setServicesStatus(node.getServicesStatus());
                    master.setProcessorCount(Runtime.getRuntime().availableProcessors());
                    master.setDiskSpaceSizeGB((int) inspector.getTotalDiskGB());
                    master.setMemorySizeGB((int) inspector.getTotalMemoryMB() / 1024);
                    master.setActive(true);
                    persistenceManager.saveExecutionNode(master);
                    persistenceManager.removeExecutionNode(node.getHostName());
                    logger.info(String.format("Node [localhost] has been renamed to [%s]", masterHost));
                }
            } catch (Exception ex) {
                logger.severe("Cannot update localhost name: " + ex.getMessage());
            }
        }
    }

    private void registerEmbeddedContainers() {
        Logger logger = Logger.getLogger(TaoServicesStartup.class.getName());
        logger.fine("Executing docker image plugins");
        List<DockerImageInstaller> installers = TopologyManager.getInstance().getInstallers();
        if (installers != null) {
            for (DockerImageInstaller imageInstaller : installers) {
                try {
                    imageInstaller.installImage();
                } catch (IOException e) {
                    Logger.getLogger(TaoServicesStartup.class.getName()).severe(e.getMessage());
                }
            }
        } else {
            logger.fine("No docker pluging found");
        }
    }

    private void registerDataSourceComponents() {
        SortedSet<String> sensors = DataSourceManager.getInstance().getSupportedSensors();
        if (sensors != null) {
            String componentId;
            for (String sensor : sensors) {
                List<String> dsNames = DataSourceManager.getInstance().getNames(sensor);
                for (String dsName : dsNames) {
                    componentId = sensor + "-" + dsName;
                    DataSourceComponent dataSourceComponent;
                    dataSourceComponent = persistenceManager.getDataSourceInstance(componentId);
                    if (dataSourceComponent == null) {
                        dataSourceComponent = new DataSourceComponent(sensor, dsName);
                        dataSourceComponent.setFetchMode(FetchMode.OVERWRITE);
                        dataSourceComponent.setLabel(sensor + " from " + dsName);
                        dataSourceComponent.setVersion("1.0");
                        dataSourceComponent.setDescription(dataSourceComponent.getId());
                        dataSourceComponent.setAuthors("TAO Team");
                        dataSourceComponent.setCopyright("(C) TAO Team");
                        dataSourceComponent.setNodeAffinity("Any");
                        try {
                            persistenceManager.saveDataSourceComponent(dataSourceComponent);
                        } catch (PersistenceException e) {
                            Logger.getLogger(TaoServicesStartup.class.getName()).severe(e.getMessage());
                        }
                    }
                }
            }
        }
    }
}
