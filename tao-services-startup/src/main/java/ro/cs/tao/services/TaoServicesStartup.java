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
import ro.cs.tao.Tag;
import ro.cs.tao.component.Identifiable;
import ro.cs.tao.component.RuntimeOptimizer;
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.component.enums.TagType;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.configuration.ConfigurationProvider;
import ro.cs.tao.datasource.DataSource;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.datasource.DataSourceManager;
import ro.cs.tao.datasource.remote.FetchMode;
import ro.cs.tao.docker.Container;
import ro.cs.tao.eodata.OutputDataHandler;
import ro.cs.tao.eodata.Projection;
import ro.cs.tao.eodata.metadata.MetadataInspector;
import ro.cs.tao.execution.ExecutionsManager;
import ro.cs.tao.execution.Executor;
import ro.cs.tao.execution.TaskStatusListener;
import ro.cs.tao.execution.drmaa.DrmaaTaoExecutor;
import ro.cs.tao.execution.local.NullExecutor;
import ro.cs.tao.execution.local.QueryExecutor;
import ro.cs.tao.execution.model.ExecutionStatus;
import ro.cs.tao.execution.model.ExecutionTask;
import ro.cs.tao.execution.model.TaskSelector;
import ro.cs.tao.execution.monitor.DefaultNodeInfo;
import ro.cs.tao.execution.monitor.NodeManager;
import ro.cs.tao.execution.monitor.OSRuntimeInfo;
import ro.cs.tao.execution.wps.WPSExecutor;
import ro.cs.tao.messaging.Messaging;
import ro.cs.tao.messaging.Topic;
import ro.cs.tao.messaging.system.StartupCompletedMessage;
import ro.cs.tao.orchestration.Orchestrator;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.quota.QuotaManager;
import ro.cs.tao.scheduling.ScheduleManager;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.security.SystemPrincipal;
import ro.cs.tao.services.commons.StartupBase;
import ro.cs.tao.services.commons.update.UpdateChecker;
import ro.cs.tao.services.interfaces.ContainerService;
import ro.cs.tao.services.interfaces.WorkflowBuilder;
import ro.cs.tao.services.security.CustomAuthenticationProvider;
import ro.cs.tao.services.security.SpringSessionProvider;
import ro.cs.tao.services.security.TaoLocalLoginModule;
import ro.cs.tao.spi.ServiceRegistry;
import ro.cs.tao.spi.ServiceRegistryManager;
import ro.cs.tao.topology.*;
import ro.cs.tao.topology.docker.DockerImageInstaller;
import ro.cs.tao.topology.docker.StandaloneContainer;
import ro.cs.tao.topology.provider.DefaultNodeProvider;
import ro.cs.tao.user.User;
import ro.cs.tao.user.UserStatus;
import ro.cs.tao.utils.DockerHelper;
import ro.cs.tao.utils.FileUtilities;
import ro.cs.tao.utils.StringUtilities;
import ro.cs.tao.workflow.WorkflowDescriptor;

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
    private static final String MASTER_USER = "topology.master.user";
    private static final String MASTER_PASSWORD = "topology.master.password";
    private static final String NODE_NAME_PREFIX = "topology.node.name.prefix";
    private static final String NODE_DEFAULT_DESCRIPTION = "topology.node.default.description";
    private static final String NODE_LIMIT = "topology.node.limit";

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
            final ConfigurationProvider cfgManager = ConfigurationManager.getInstance();
            if (cfgManager == null) {
                throw new RuntimeException("Framework configuration unavailable");
            }
            cfgManager.setValue(ConfigurationProvider.APP_HOME, homeDirectory().toString());
            injectPersistence();
            logger.fine("Initialized persistence manager");
            for (Map.Entry<String, Class> entry : plugins.entrySet()) {
                ServiceRegistry<?> registry = ServiceRegistryManager.getInstance().getServiceRegistry(entry.getValue());
                Set<?> instances = registry.getServices();
                logger.info(String.format("Installed %s: %s",
                                          entry.getKey(), instances.stream().map(i -> i.getClass().getSimpleName())
                                                                    .sorted().collect(Collectors.joining(","))));
            }
            logger.info("Web interface is accessible at https://localhost:" + cfgManager.getValue("server.port"));
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
            Messaging.setPersister(this.persistenceManager.notifications());
            logger.fine("Initialized messaging subsystem");
            SpringSessionProvider.setPersistenceManager(this.persistenceManager);
            SessionStore.setSessionContextProvider(new SpringSessionProvider());
            TaoLocalLoginModule.setPersistenceManager(this.persistenceManager);
            CustomAuthenticationProvider.setPersistenceManager(this.persistenceManager);
            logger.fine("Initialized authentication provider");
            updateLocalhost();
            backgroundWorker.submit(this::registerFlavors);
            backgroundWorker.submit(this::createUserWorkspaces);
            Orchestrator.getInstance().start();
            backgroundWorker.submit(this::registerDataSourceComponents);
            registerEmbeddedContainers();
            backgroundWorker.submit(this::registerWorkflowLibrary);
            backgroundWorker.submit(() -> {
                final Map<String, String> map = Projection.getSupported();
                logger.fine(String.format("Projection database initialized [%d projections]", map.size()));
            });
            backgroundWorker.submit(() -> {
                final NodeManager nodeManager = NodeManager.getInstance();
                if (nodeManager != null) {
                    final String prefix = cfgManager.getValue(NODE_NAME_PREFIX);
                    final String description = cfgManager.getValue(NODE_DEFAULT_DESCRIPTION);
                    final String user = cfgManager.getValue(MASTER_USER);
                    final String pwd = cfgManager.getValue(MASTER_PASSWORD);
                    if (StringUtilities.isNullOrEmpty(prefix) || StringUtilities.isNullOrEmpty(description) ||
                            StringUtilities.isNullOrEmpty(user) || StringUtilities.isNullOrEmpty(pwd)) {
                        nodeManager.setDefaultNodeInfo(null);
                    } else {
                        nodeManager.setDefaultNodeInfo(new DefaultNodeInfo(prefix, description, user, pwd));
                    }
                    nodeManager.setNodeProvider(new DefaultNodeProvider());
                    nodeManager.setTaskProvider(persistenceManager.tasks());
                    nodeManager.setNodeLimit(Integer.parseInt(cfgManager.getValue(NODE_LIMIT, "0")));
                    nodeManager.initialize();
                    TopologyManager.getInstance().registerListener(nodeManager);
                    nodeManager.start();
                    logger.fine("Topology monitoring initialized. " + (nodeManager.canCreateNewNodes() ?
                            "Node creation is enabled up to " + nodeManager.getNodeLimit() + " nodes." :
                            "Node creation is not supported."));
                } else {
                    logger.fine(String.format("Topology monitoring not available (DRMAA session factory set to %s",
                                              cfgManager.getValue("tao.drmaa.sessionfactory")));
                }
            });
            backgroundWorker.submit(() -> {
            	ScheduleManager.start();
            	logger.fine("Scheduling engine started");
            });
            UpdateChecker.initialize();
            Messaging.send(SystemPrincipal.instance(), Topic.SYSTEM.value(), new StartupCompletedMessage());
        }
    }

    private void injectPersistence() {
        Executor.setStatusListener(new TaskStatusListener() {
            @Override
            public void taskStatusChanged(ExecutionTask task, ExecutionStatus status) {
                try {
                    persistenceManager.tasks().updateStatus(task, status);
                } catch (PersistenceException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void taskChanged(ExecutionTask task) {
                try {
                    persistenceManager.tasks().update(task);
                } catch (PersistenceException e) {
                    e.printStackTrace();
                }
            }
        });
        QueryExecutor.setProductProvider(persistenceManager.rasterData());
        QueryExecutor.setWorkflowProvider(persistenceManager.workflows());
        DrmaaTaoExecutor.setTaskProvider(persistenceManager.tasks());
        DrmaaTaoExecutor.setContainerProvider(persistenceManager.containers());
        NullExecutor.setWorkflowNodeProvider(persistenceManager.workflowNodes());
        NullExecutor.setComponentProvider(persistenceManager.processingComponents());
        WPSExecutor.setTaskProvider(persistenceManager.tasks());
    }

    private void updateLocalhost() {
        Logger logger = Logger.getLogger(TaoServicesStartup.class.getName());
        NodeDescription node = persistenceManager.nodes().get("localhost");
        if (node != null) {
            try {
                List<Tag> nodeTags = persistenceManager.tags().list(TagType.TOPOLOGY_NODE);
                if (nodeTags == null || nodeTags.size() == 0) {
                    for (TagType tagType : TagType.values()) {
                        persistenceManager.tags().save(new Tag(TagType.TOPOLOGY_NODE, tagType.friendlyName()));
                    }
                }
                logger.finest("Overriding the default 'localhost' database entry");
                String masterHost = InetAddress.getLocalHost().getHostName();
                NodeDescription master = persistenceManager.nodes().get(masterHost);
                if (master == null) {
                    Tag masterTag = new Tag(TagType.TOPOLOGY_NODE, "master");
                    int processors = Runtime.getRuntime().availableProcessors();
                    persistenceManager.tags().save(masterTag);
                    master = new NodeDescription();
                    master.setId(masterHost);
                    String user = ConfigurationManager.getInstance().getValue("topology.master.user", node.getUserName());
                    master.setUserName(user);
                    String pwd = ConfigurationManager.getInstance().getValue("topology.master.password", node.getUserPass());
                    master.setUserPass(pwd);
                    OSRuntimeInfo inspector = OSRuntimeInfo.createInspector(masterHost, user, pwd);
                    master.setDescription(node.getDescription());
                    master.setServicesStatus(node.getServicesStatus());
                    final NodeFlavor masterFlavor = persistenceManager.nodeFlavors().getMasterFlavor();
                    masterFlavor.setCpu(processors);
                    masterFlavor.setMemory((int) inspector.getTotalMemoryMB() / 1024);
                    masterFlavor.setDisk((int) inspector.getTotalDiskGB());
                    master.setFlavor(masterFlavor);
                    master.setActive(true);
                    master.addTag(masterTag.getText());
                    if (master.getServicesStatus() == null || master.getServicesStatus().size() == 0) {
                        // check docker service on master
                        String name = "Docker";
                        String version = DockerHelper.getDockerVersion();
                        ServiceDescription description = persistenceManager.nodes().getServiceDescription(name, version);
                        if (description == null) {
                            description = new ServiceDescription();
                            description.setName(name);
                            description.setDescription("Application container manager");
                            description.setVersion(version);
                            description = persistenceManager.nodes().saveServiceDescription(description);
                        }
                        NodeServiceStatus nodeService = new NodeServiceStatus();
                        nodeService.setServiceDescription(description);
                        nodeService.setStatus(DockerHelper.isDockerFound() ? ServiceStatus.INSTALLED : ServiceStatus.NOT_FOUND);
                        master.addServiceStatus(nodeService);
                        // check CRM on master
                        Set<Executor> executors = ExecutionsManager.getInstance().getRegisteredExecutors();
                        Executor<?> executor = executors.stream().filter(e -> e instanceof DrmaaTaoExecutor).findFirst().orElse(null);
                        if (executor != null) {
                            DrmaaTaoExecutor taoExecutor = (DrmaaTaoExecutor) executor;
                            nodeService = new NodeServiceStatus();
                            name = taoExecutor.getDRMName();
                            version = taoExecutor.getDRMVersion();
                            description = persistenceManager.nodes().getServiceDescription(name, version);
                            if (description == null) {
                                description = new ServiceDescription();
                                description.setName(name);
                                description.setDescription("NoCRM".equals(name) ? "Local execution" : name);
                                description.setVersion(version);
                                description = persistenceManager.nodes().saveServiceDescription(description);
                            }
                            nodeService.setServiceDescription(description);
                            nodeService.setStatus("n/a".equals(description.getVersion()) ? ServiceStatus.NOT_FOUND : ServiceStatus.INSTALLED);
                            master.addServiceStatus(nodeService);
                        }

                    }
                    persistenceManager.nodes().save(master);
                    persistenceManager.nodes().delete(node.getId());
                    logger.fine(String.format("Node [localhost] has been renamed to [%s]", masterHost));
                }
                TopologyManager.setMasterNode(master);
                createMasterShare();
            } catch (Exception ex) {
                logger.severe("Cannot update localhost name: " + ex.getMessage());
            }
        }
    }

    private void createMasterShare() {
        TopologyManager manager = TopologyManager.getInstance();
        manager.notifyListeners(manager.getMasterNodeInfo(), manager.checkMasterShare());
    }

    private void registerFlavors() {
        TopologyManager manager = TopologyManager.getInstance();
        final List<NodeFlavor> flavors = manager.listNodeFlavors();
        final Set<String> existingFlavors = persistenceManager.nodeFlavors().list().stream().map(NodeFlavor::getId).collect(Collectors.toSet());
        if (flavors != null) {
            for (NodeFlavor flavor : flavors) {
                try {
                    if (existingFlavors.contains(flavor.getId())) {
                        persistenceManager.nodeFlavors().update(flavor);
                    } else {
                        persistenceManager.nodeFlavors().save(flavor);
                    }
                } catch (PersistenceException e) {
                    logger.severe(e.getMessage());
                }
            }
            logger.fine("Updated node flavors from the topology node provider");
        }
    }

    private void createUserWorkspaces() {
        List<User> activeUsers = persistenceManager.users().list(UserStatus.ACTIVE);
        if (activeUsers != null) {
            final Path rootPath = Paths.get(SystemVariable.ROOT.value());
            for (User user : activeUsers) {
                try {
                    Path userPath = rootPath.resolve(user.getUsername());
                    Files.createDirectories(userPath);
                    Files.createDirectories(userPath.resolve("files"));
                } catch (IOException e) {
                    logger.severe(String.format("Failed to create user workspace [user=%s, reason=%s]",
                                                user.getUsername(), e.getMessage()));
                }
            }
        }
    }

    private void registerEmbeddedContainers() {
        List<DockerImageInstaller> installers = TopologyManager.getInstance().getInstallers();
        if (installers != null && installers.size() > 0) {
            logger.finest(String.format("Found %d docker image plugins: %s", installers.size(),
                                        installers.stream().map(i -> i.getClass().getSimpleName()).collect(Collectors.joining(","))));
            for (DockerImageInstaller imageInstaller : installers) {
                backgroundWorker.submit(() -> {
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
                });
            }
        } else {
            logger.fine("No docker image plugin found");
        }
        final List<StandaloneContainer> containers = TopologyManager.getInstance().getStandaloneContainers();
        if (containers != null && containers.size() > 0) {
            logger.finest(String.format("Found %d docker standalone containers: %s", containers.size(),
                                        containers.stream().map(i -> i.getClass().getSimpleName()).collect(Collectors.joining(","))));
            for (StandaloneContainer container : containers) {
                backgroundWorker.submit(() -> {
                    try {
                        container.install();
                        logger.fine(String.format("Installed docker image %s", container.getContainerName()));
                        container.start();
                        logger.fine(String.format("Started docker container %s", container.getContainerName()));
                    } catch (Throwable e) {
                        logger.severe(e.getMessage());
                    }
                });
            }
        } else {
            logger.fine("No docker standalone container found");
        }
    }

    private void registerDataSourceComponents() {
        SortedSet<String> sensors = DataSourceManager.getInstance().getSupportedSensors();
        if (sensors != null) {
            Set<String> existing = persistenceManager.dataSourceComponents().list()
                    .stream()
                    .filter(DataSourceComponent::getSystem)
                    .map(Identifiable::getId)
                    .collect(Collectors.toSet());
            List<Tag> tags = persistenceManager.tags().list(TagType.DATASOURCE);
            if (tags == null) {
                tags = new ArrayList<>();
            }
            String componentId;
            List<String> newDs = null;
            for (String sensor : sensors) {
                Tag sensorTag = tags.stream().filter(t -> t.getText().equalsIgnoreCase(sensor)).findFirst().orElse(null);
                if (sensorTag == null) {
                    try {
                        sensorTag = persistenceManager.tags().save(new Tag(TagType.DATASOURCE, sensor));
                        tags.add(sensorTag);
                    } catch (PersistenceException e) {
                        logger.severe(e.getMessage());
                    }
                }
                List<String> dsNames = DataSourceManager.getInstance().getNames(sensor);
                for (String dsName : dsNames) {
                    componentId = sensor + "-" + dsName;
                    if (!existing.contains(componentId)) {
                        Tag dsNameTag = tags.stream().filter(t -> t.getText().equalsIgnoreCase(dsName)).findFirst().orElse(null);
                        if (dsNameTag == null) {
                            try {
                                dsNameTag = persistenceManager.tags().save(new Tag(TagType.DATASOURCE, dsName));
                                tags.add(dsNameTag);
                            } catch (PersistenceException e) {
                                logger.severe(e.getMessage());
                            }
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
                        if (sensorTag != null) {
                            dataSourceComponent.addTags(sensorTag.getText());
                        }
                        if (dsNameTag != null) {
                            dataSourceComponent.addTags(dsNameTag.getText());
                        }
                        try {
                            dataSourceComponent = persistenceManager.dataSourceComponents().save(dataSourceComponent);
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

    private void registerWorkflowLibrary() {
        ServiceRegistry<WorkflowBuilder> registry = ServiceRegistryManager.getInstance().getServiceRegistry(WorkflowBuilder.class);
        Set<WorkflowBuilder> services = registry.getServices();
        if (services == null || services.size() == 0) {
            logger.fine("System workflow library is empty");
        } else {
            for (WorkflowBuilder workflow : services) {
                try {
                    WorkflowDescriptor descriptor = workflow.createWorkflowDescriptor();
                    if (descriptor != null) {
                        logger.finest(String.format("Registration completed for workflow %s", workflow.getName()));
                    } else {
                        logger.fine(String.format("Registration failed for workflow %s", workflow.getName()));
                    }
                } catch (Exception e) {
                    logger.warning(e.getMessage());
                }
            }
        }
    }
}
