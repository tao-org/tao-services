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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import ro.cs.tao.Tag;
import ro.cs.tao.component.Identifiable;
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.component.enums.TagType;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.datasource.DataSourceManager;
import ro.cs.tao.datasource.remote.FetchMode;
import ro.cs.tao.docker.Container;
import ro.cs.tao.messaging.Messaging;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.StartupBase;
import ro.cs.tao.services.interfaces.ContainerService;
import ro.cs.tao.services.model.monitoring.OSRuntimeInfo;
import ro.cs.tao.services.security.CustomAuthenticationProvider;
import ro.cs.tao.services.security.SpringSessionProvider;
import ro.cs.tao.services.security.TaoLocalLoginModule;
import ro.cs.tao.topology.NodeDescription;
import ro.cs.tao.topology.TopologyManager;
import ro.cs.tao.topology.docker.DockerImageInstaller;
import ro.cs.tao.utils.FileUtilities;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Cosmin Cara
 */

@SpringBootApplication
@EnableScheduling
@EnableWebMvc
public class TaoServicesStartup extends StartupBase {
    private final static Logger logger = Logger.getLogger(TaoServicesStartup.class.getName());

    @Autowired
    private PersistenceManager persistenceManager;

    @Autowired
    private ContainerService containerService;

    public static void main(String[] args) throws IOException {
        run(TaoServicesStartup.class, args);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            logger.fine("Spring initialization completed");
            try {
                FileUtilities.ensureExists(Paths.get(SystemVariable.SHARED_WORKSPACE.value()));
                FileUtilities.ensureExists(Paths.get(SystemVariable.SHARED_FILES.value()));
            } catch (IOException e) {
                logger.severe("Cannot create required folders: " + e.getMessage());
                System.exit(1);
            }
            BaseController.setPersistenceManager(this.persistenceManager);
            logger.fine("Initialized persistence manager");
            Messaging.setPersister(this.persistenceManager);
            logger.fine("Initialized messaging subsystem");
            SpringSessionProvider.setPersistenceManager(this.persistenceManager);
            SessionStore.setSessionContextProvider(new SpringSessionProvider());
            TaoLocalLoginModule.setPersistenceManager(this.persistenceManager);
            CustomAuthenticationProvider.setPersistenceManager(this.persistenceManager);
            logger.fine("Initialized authentication provider");
            updateLocalhost();
            backgroundWorker.submit(this::registerEmbeddedContainers);
            backgroundWorker.submit(this::registerDataSourceComponents);
        }
    }

    private void updateLocalhost() {
        Logger logger = Logger.getLogger(TaoServicesStartup.class.getName());
        NodeDescription node = TopologyManager.getInstance().get("localhost");
        if (node != null) {
            try {
                logger.finest("Overriding the default 'localhost' database entry");
                String masterHost = InetAddress.getLocalHost().getHostName();
                NodeDescription master = persistenceManager.getNodeByHostName(masterHost);
                if (master == null) {
                    Tag masterTag = new Tag(TagType.TOPOLOGY_NODE, "master");
                    Tag procTag = new Tag(TagType.TOPOLOGY_NODE,
                                          String.valueOf(Runtime.getRuntime().availableProcessors()) + " processors");
                    persistenceManager.saveTag(masterTag);
                    persistenceManager.saveTag(procTag);
                    master = new NodeDescription();
                    master.setId(masterHost);
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
                    master.addTag(masterTag.getText());
                    master.addTag(procTag.getText());
                    persistenceManager.saveExecutionNode(master);
                    persistenceManager.removeExecutionNode(node.getId());
                    logger.fine(String.format("Node [localhost] has been renamed to [%s]", masterHost));
                }
            } catch (Exception ex) {
                logger.severe("Cannot update localhost name: " + ex.getMessage());
            }
        }
    }

    private void registerEmbeddedContainers() {
        List<DockerImageInstaller> installers = TopologyManager.getInstance().getInstallers();
        if (installers != null && installers.size() > 0) {
            logger.finest(String.format("Found %s docker image plugins: %s", installers.size(),
                        String.join(",", installers.stream().map(i -> i.getClass().getSimpleName()).collect(Collectors.toList()))));
            for (DockerImageInstaller imageInstaller : installers) {
                try {
                    Container container = imageInstaller.installImage();
                    if (container != null && container.getLogo() != null) {
                        Path imgPath = homeDirectory().resolve("static").resolve("workflow").resolve("media")
                                                    .resolve(container.getId() + ".png");
                        if (!Files.exists(imgPath)) {
                            Files.createDirectories(imgPath.getParent());
                            Files.write(imgPath, Base64.getDecoder().decode(container.getLogo()));
                        }
                    }
                } catch (Throwable e) {
                    logger.severe(e.getMessage());
                }
            }
        } else {
            logger.fine("No docker image plugin found");
        }
    }

    private void registerDataSourceComponents() {
        SortedSet<String> sensors = DataSourceManager.getInstance().getSupportedSensors();
        if (sensors != null) {
            Set<String> existing = persistenceManager.getDataSourceComponents()
                    .stream()
                    .map(Identifiable::getId)
                    .collect(Collectors.toSet());
            List<Tag> tags = persistenceManager.getDatasourceTags();
            if (tags == null) {
                tags = new ArrayList<>();
            }
            String componentId;
            List<String> newDs = null;
            for (String sensor : sensors) {
                Tag sensorTag = tags.stream().filter(t -> t.getText().equalsIgnoreCase(sensor)).findFirst().orElse(null);
                if (sensorTag == null) {
                    sensorTag = persistenceManager.saveTag(new Tag(TagType.DATASOURCE, sensor));
                    tags.add(sensorTag);
                }
                List<String> dsNames = DataSourceManager.getInstance().getNames(sensor);
                for (String dsName : dsNames) {
                    componentId = sensor + "-" + dsName;
                    if (!existing.contains(componentId)) {
                        Tag dsNameTag = tags.stream().filter(t -> t.getText().equalsIgnoreCase(dsName)).findFirst().orElse(null);
                        if (dsNameTag == null) {
                            dsNameTag = persistenceManager.saveTag(new Tag(TagType.DATASOURCE, dsName));
                            tags.add(dsNameTag);
                        }
                        DataSourceComponent dataSourceComponent = new DataSourceComponent(sensor, dsName);
                        dataSourceComponent.setFetchMode(FetchMode.OVERWRITE);
                        dataSourceComponent.setLabel(sensor + " from " + dsName);
                        dataSourceComponent.setVersion("1.0");
                        dataSourceComponent.setDescription(dataSourceComponent.getId());
                        dataSourceComponent.setAuthors("TAO Team");
                        dataSourceComponent.setCopyright("(C) TAO Team");
                        dataSourceComponent.setNodeAffinity("Any");
                        dataSourceComponent.setSystem(true);
                        dataSourceComponent.addTags(sensorTag.getText());
                        dataSourceComponent.addTags(dsNameTag.getText());
                        try {
                            dataSourceComponent = persistenceManager.saveDataSourceComponent(dataSourceComponent);
                            if (newDs == null) {
                                newDs = new ArrayList<>();
                            }
                            newDs.add(dataSourceComponent.getId());
                        } catch (PersistenceException e) {
                            logger.severe(e.getMessage());
                        }
                    }
                    existing.remove(componentId);
                }
            }
            if (newDs != null) {
                logger.fine(String.format("Registered %s new data source components: %s",
                                          newDs.size(), String.join(",", newDs)));
            }
            if (existing.size() > 0) {
                logger.warning(String.format("There are %s data source components in the database that have not been found: %s",
                                             existing.size(), String.join(",", existing)));
            }
        }
    }
}
