package ro.cs.tao.services.startup;

import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.configuration.ConfigurationProvider;
import ro.cs.tao.execution.monitor.DefaultNodeInfo;
import ro.cs.tao.execution.monitor.NodeManager;
import ro.cs.tao.topology.TopologyManager;
import ro.cs.tao.topology.provider.DefaultNodeProvider;
import ro.cs.tao.utils.StringUtilities;

public class TopologyInitializer extends BaseLifeCycle {
    private static final String MASTER_USER = "topology.master.user";
    private static final String MASTER_PASSWORD = "topology.master.password";
    private static final String MASTER_SSH_KEY = "topology.node.ssh.key";
    private static final String NODE_NAME_PREFIX = "topology.node.name.prefix";
    private static final String NODE_DEFAULT_DESCRIPTION = "topology.node.default.description";
    private static final String NODE_LIMIT = "topology.node.limit";

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public void onStartUp() {
        final DefaultNodeProvider nodeProvider = new DefaultNodeProvider();
        TopologyManager.getInstance().setLocalNodeProvider(nodeProvider);
        TopologyManager.getInstance().setVolatileInstanceProvider(persistenceManager.volatileInstances());
        final NodeManager nodeManager = NodeManager.getInstance();
        final ConfigurationProvider cfgManager = ConfigurationManager.getInstance();
        if (nodeManager != null) {
            final String prefix = cfgManager.getValue(NODE_NAME_PREFIX);
            final String description = cfgManager.getValue(NODE_DEFAULT_DESCRIPTION);
            final String user = cfgManager.getValue(MASTER_USER);
            final String pwd = cfgManager.getValue(MASTER_PASSWORD);
            final String key = cfgManager.getValue(MASTER_SSH_KEY);
            if (StringUtilities.isNullOrEmpty(prefix) || StringUtilities.isNullOrEmpty(description) ||
                    StringUtilities.isNullOrEmpty(user) || StringUtilities.isNullOrEmpty(pwd) || StringUtilities.isNullOrEmpty(key)) {
                nodeManager.setDefaultNodeInfo(null);
            } else {
                nodeManager.setDefaultNodeInfo(new DefaultNodeInfo(prefix, description, user, pwd, key));
            }
            nodeManager.setNodeProvider(nodeProvider);
            nodeManager.setTaskProvider(persistenceManager.tasks());
            nodeManager.setResourceSubscriptionProvider(persistenceManager.resourceSubscription());
            nodeManager.setFlavorProvider(persistenceManager.nodeFlavors());
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
    }

    @Override
    public void onShutdown() {

    }
}
