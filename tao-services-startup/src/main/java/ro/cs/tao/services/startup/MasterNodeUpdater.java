package ro.cs.tao.services.startup;

import ro.cs.tao.Tag;
import ro.cs.tao.component.enums.TagType;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.execution.ExecutionsManager;
import ro.cs.tao.execution.Executor;
import ro.cs.tao.execution.drmaa.DRMAAExecutor;
import ro.cs.tao.execution.monitor.OSRuntimeInfo;
import ro.cs.tao.execution.monitor.RuntimeInfo;
import ro.cs.tao.topology.*;
import ro.cs.tao.utils.DockerHelper;
import ro.cs.tao.utils.executors.AuthenticationType;
import ro.cs.tao.utils.executors.MemoryUnit;

import java.net.InetAddress;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * At the very first run, creates the master node from the pre-defined record 'localhost'.
 */
public class MasterNodeUpdater extends BaseLifeCycle {

    @Override
    public int priority() {
        return 1;
    }

    @Override
    public void onStartUp() {
        try {
            String masterHost = InetAddress.getLocalHost().getHostName();
            NodeDescription master = persistenceManager.nodes().get(masterHost);
            if (master == null) {
                NodeDescription node = persistenceManager.nodes().get("localhost");
                List<Tag> nodeTags = persistenceManager.tags().list(TagType.TOPOLOGY_NODE);
                if (nodeTags == null || nodeTags.isEmpty()) {
                    for (TagType tagType : TagType.values()) {
                        persistenceManager.tags().save(new Tag(TagType.TOPOLOGY_NODE, tagType.friendlyName()));
                    }
                }
                logger.finest("Overriding the default 'localhost' database entry");
                Tag masterTag = new Tag(TagType.TOPOLOGY_NODE, "master");
                int processors = Runtime.getRuntime().availableProcessors();
                persistenceManager.tags().save(masterTag);
                master = new NodeDescription();
                master.setId(masterHost);
                String user = ConfigurationManager.getInstance().getValue("topology.master.user", node.getUserName());
                master.setUserName(user);
                String pwd = ConfigurationManager.getInstance().getValue("topology.master.password", node.getUserPass());
                master.setUserPass(pwd);
                OSRuntimeInfo<?> inspector = OSRuntimeInfo.createInspector(masterHost, user, pwd, AuthenticationType.PASSWORD, RuntimeInfo.class);
                master.setDescription(node.getDescription());
                master.setServicesStatus(node.getServicesStatus());
                final NodeFlavor masterFlavor = persistenceManager.nodeFlavors().getMasterFlavor();
                masterFlavor.setCpu(processors);
                masterFlavor.setMemory((int) (inspector.getTotalMemoryMB() / MemoryUnit.KB.value()));
                masterFlavor.setDisk((int) inspector.getTotalDiskGB());
                master.setFlavor(masterFlavor);
                master.setActive(true);
                master.setRole(NodeRole.MASTER);
                master.setVolatile(false);
                master.addTag(masterTag.getText());
                if (master.getServicesStatus() == null || master.getServicesStatus().isEmpty()) {
                    // check docker service on master
                    String name = "Docker";
                    String version = DockerHelper.getDockerVersion();
                    ServiceDescription description = persistenceManager.nodes().getServiceDescription(name, version);
                    if (description == null) {
                        description = new ServiceDescription();
                        description.setName(name);
                        description.setDescription("Application container manager");
                        description.setVersion(version != null ? version : "n/a");
                        description = persistenceManager.nodes().saveServiceDescription(description);
                        NodeServiceStatus nodeService = new NodeServiceStatus();
                        nodeService.setServiceDescription(description);
                        master.addServiceStatus(nodeService);
                    }
                    NodeServiceStatus nodeService = master.getServicesStatus().stream().filter(s -> s.getServiceDescription().getName().equals("Docker")).findFirst().get();
                    nodeService.setStatus(DockerHelper.isDockerFound() ? ServiceStatus.INSTALLED : ServiceStatus.NOT_FOUND);
                    // check CRM on master
                    Set<Executor> executors = ExecutionsManager.getInstance().getRegisteredExecutors();
                    Executor<?> executor = executors.stream().filter(e -> e instanceof DRMAAExecutor).findFirst().orElse(null);
                    if (executor != null) {
                        DRMAAExecutor taoExecutor = (DRMAAExecutor) executor;
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
            if (master.getAppId() == null) {
                master.setAppId(UUID.randomUUID().toString());
                persistenceManager.nodes().update(master);
            }
            TopologyManager.setMasterNode(master);
            TopologyManager manager = TopologyManager.getInstance();
            manager.notifyListeners(manager.getMasterNodeInfo(), manager.checkMasterShare());
        } catch (Exception ex) {
            logger.severe("Cannot update localhost name: " + ex.getMessage());
        }

    }

    @Override
    public void onShutdown() {

    }
}
