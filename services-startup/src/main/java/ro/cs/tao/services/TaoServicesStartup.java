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
import ro.cs.tao.docker.Container;
import ro.cs.tao.messaging.Messaging;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.admin.service.AdministrationServiceLauncher;
import ro.cs.tao.services.auth.service.AuthenticationServiceLauncher;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.entity.DataServicesLauncher;
import ro.cs.tao.services.entity.impl.ContainerInitializer;
import ro.cs.tao.services.interfaces.ContainerService;
import ro.cs.tao.services.monitoring.MonitoringServiceLauncer;
import ro.cs.tao.services.monitoring.os.OSRuntimeInfo;
import ro.cs.tao.services.orchestration.OrchestratorLauncher;
import ro.cs.tao.services.progress.ProgressReportLauncher;
import ro.cs.tao.services.query.DataQueryServiceLauncher;
import ro.cs.tao.services.security.CustomAuthenticationProvider;
import ro.cs.tao.services.security.SpringSessionProvider;
import ro.cs.tao.services.security.TaoLocalLoginModule;
import ro.cs.tao.services.user.service.UserServiceLauncher;
import ro.cs.tao.topology.NodeDescription;
import ro.cs.tao.topology.TopologyManager;
import ro.cs.tao.utils.Platform;
import ro.cs.tao.utils.executors.DebugOutputConsumer;
import ro.cs.tao.utils.executors.Executor;
import ro.cs.tao.utils.executors.ExecutorType;
import ro.cs.tao.utils.executors.ProcessExecutor;

import java.io.*;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
                //.bannerMode(Banner.Mode.OFF)
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
                    //master.setProcessorCount(node.getProcessorCount());
                    master.setProcessorCount(Runtime.getRuntime().availableProcessors());
                    //master.setDiskSpaceSizeGB(node.getDiskSpaceSizeGB());
                    master.setDiskSpaceSizeGB((int) inspector.getTotalDiskGB());
                    //master.setMemorySizeGB(node.getMemorySizeGB());
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
        try {
            boolean localMode = Boolean.parseBoolean(ConfigurationManager.getInstance().getValue("tao.localmode", "true"));
            boolean canUseDocker = !localMode;
            if (!localMode) {
                List<String> arguments = new ArrayList<>();
                arguments.add("docker");
                try {
                    Executor executor = ProcessExecutor.create(ExecutorType.PROCESS,
                                                               InetAddress.getLocalHost().getHostName(),
                                                               arguments);
                    executor.setOutputConsumer(new DebugOutputConsumer());
                    canUseDocker = executor.execute(false) == 0;
                } catch (Exception ignored) {
                }
            }
            String snapContainerName = ConfigurationManager.getInstance().getValue("embedded.snap.container.name");
            String otbContainerName = ConfigurationManager.getInstance().getValue("embedded.otb.container.name");
            String snapPath = null, otbPath = null;
            Container snapContainer = null, otbContainer = null;
            if (canUseDocker) {
                try {
                    Path dockerImagesPath = Paths.get(ConfigurationManager.getInstance().getValue("tao.docker.images"));
                    Files.createDirectories(dockerImagesPath);
                    Path dockerfilePath = dockerImagesPath.resolve(snapContainerName).resolve("Dockerfile");
                    if (!Files.exists(dockerfilePath)) {
                        Files.createDirectories(dockerfilePath.getParent());
                        byte[] buffer = new byte[1024];
                        try (BufferedInputStream is = new BufferedInputStream(TopologyManager.class.getResourceAsStream("docker/snap/Dockerfile"));
                             OutputStream os = new BufferedOutputStream(Files.newOutputStream(dockerfilePath))) {
                            int read;
                            while ((read = is.read(buffer)) != -1) {
                                os.write(buffer, 0, read);
                            }
                            os.flush();
                        }
                    }
                    TopologyManager topologyManager = TopologyManager.getInstance();
                    if (topologyManager.getDockerImage(snapContainerName) == null) {
                        Logger.getLogger(TaoServicesStartup.class.getName()).info("Begin registering docker image for SNAP");
                        topologyManager.registerImage(dockerfilePath.toRealPath(), snapContainerName, "SNAP");
                        Logger.getLogger(TaoServicesStartup.class.getName()).info("Docker image for SNAP registration completed");
                    }
                    snapContainer = topologyManager.getDockerImage(snapContainerName);
                    snapPath = "/opt/snap/bin";
                    dockerfilePath = dockerImagesPath.resolve(otbContainerName).resolve("Dockerfile");
                    if (!Files.exists(dockerfilePath)) {
                        Files.createDirectories(dockerfilePath.getParent());
                        byte[] buffer = new byte[1024];
                        try (BufferedInputStream is = new BufferedInputStream(TopologyManager.class.getResourceAsStream("docker/otb/Dockerfile"));
                             OutputStream os = new BufferedOutputStream(Files.newOutputStream(dockerfilePath))) {
                            int read;
                            while ((read = is.read(buffer)) != -1) {
                                os.write(buffer, 0, read);
                            }
                            os.flush();
                        }
                    }
                    if (topologyManager.getDockerImage(otbContainerName) == null) {
                        Logger.getLogger(TaoServicesStartup.class.getName()).info("Begin registering docker image for OTB");
                        topologyManager.registerImage(dockerfilePath.toRealPath(), otbContainerName, "OTB");
                        Logger.getLogger(TaoServicesStartup.class.getName()).info("Docker image for OTB registration completed");
                    }
                    otbContainer = topologyManager.getDockerImage(otbContainerName);
                    otbPath = "/opt/OTB-6.4.0-Linux64/bin";
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                String systemPath = System.getenv("Path");
                String[] paths = systemPath.split(File.pathSeparator);
                final boolean isWindows = Platform.ID.win.equals(Platform.getCurrentPlatform().getId());
                String snapExec = "gpt" + (isWindows ? ".exe" : "");
                String otbExec = "otbcli_BandMath" + (isWindows ? ".bat" : "");
                Path currentPath;
                for (String path : paths) {
                    currentPath = Paths.get(path).resolve(snapExec);
                    if (Files.exists(currentPath)) {
                        snapPath = path;
                    }
                    currentPath = Paths.get(path).resolve(otbExec);
                    if (Files.exists(currentPath)) {
                        otbPath = path;
                    }
                    if (snapPath != null && otbPath != null) {
                        break;
                    }
                }
                snapContainer = new Container();
                snapContainer.setId(snapContainerName);
                snapContainer.setName(snapContainerName);
                otbContainer = new Container();
                otbContainer.setId(otbContainerName);
                otbContainer.setName(otbContainerName);
            }
            ContainerInitializer.setPersistenceManager(persistenceManager);
            ContainerInitializer.setContainerService(containerService);
            if (snapPath != null) {
                if (snapContainer != null) {
                    ContainerInitializer.initSnap(snapContainer.getId(), snapContainerName, snapPath);
                }
            } else {
                Logger.getLogger(TaoServicesStartup.class.getName()).warning("SNAP was not found in system path");
            }
            if (otbPath != null) {
                if (otbContainer != null) {
                    ContainerInitializer.initOtb(otbContainer.getId(), otbContainerName, otbPath);
                }
            } else {
                Logger.getLogger(TaoServicesStartup.class.getName()).warning("OTB was not found in system path");
            }
        } catch (Exception ex) {
            Logger.getLogger(TaoServicesStartup.class.getName()).severe("Error encountered while initializing Docker images: " + ex.getMessage());
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
