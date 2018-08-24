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
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.annotation.EnableScheduling;
import ro.cs.tao.component.Identifiable;
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.datasource.DataSourceManager;
import ro.cs.tao.datasource.remote.FetchMode;
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

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Cosmin Cara
 */

@SpringBootApplication
@EnableScheduling
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
                logger.severe("Cannot create required folders: " + e.getMessage());
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
        logger.fine("Executing docker image plugins");
        List<DockerImageInstaller> installers = TopologyManager.getInstance().getInstallers();
        if (installers != null && installers.size() > 0) {
            logger.fine(String.format("Found %s images: %s", installers.size(),
                        String.join(",", installers.stream().map(i -> i.getClass().getSimpleName()).collect(Collectors.toList()))));
            for (DockerImageInstaller imageInstaller : installers) {
                try {
                    imageInstaller.installImage();
                } catch (Throwable e) {
                    logger.severe(e.getMessage());
                }
            }
        } else {
            logger.fine("No docker pluging found");
        }
    }

    private void registerDataSourceComponents() {
        SortedSet<String> sensors = DataSourceManager.getInstance().getSupportedSensors();
        if (sensors != null) {
            Set<String> existing = persistenceManager.getDataSourceComponents()
                    .stream()
                    .map(Identifiable::getId)
                    .collect(Collectors.toSet());
            String componentId;
            for (String sensor : sensors) {
                List<String> dsNames = DataSourceManager.getInstance().getNames(sensor);
                for (String dsName : dsNames) {
                    componentId = sensor + "-" + dsName;
                    if (!existing.contains(componentId)) {
                        DataSourceComponent dataSourceComponent = new DataSourceComponent(sensor, dsName);
                        dataSourceComponent.setFetchMode(FetchMode.OVERWRITE);
                        dataSourceComponent.setLabel(sensor + " from " + dsName);
                        dataSourceComponent.setVersion("1.0");
                        dataSourceComponent.setDescription(dataSourceComponent.getId());
                        dataSourceComponent.setAuthors("TAO Team");
                        dataSourceComponent.setCopyright("(C) TAO Team");
                        dataSourceComponent.setNodeAffinity("Any");
                        try {
                            dataSourceComponent = persistenceManager.saveDataSourceComponent(dataSourceComponent);
                            logger.fine("Registered new data source component: " + dataSourceComponent.getId());
                        } catch (PersistenceException e) {
                            logger.severe(e.getMessage());
                        }
                    }
                }
            }
        }
    }
}
