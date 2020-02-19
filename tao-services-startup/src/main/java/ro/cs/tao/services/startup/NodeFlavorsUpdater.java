package ro.cs.tao.services.startup;

import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.topology.NodeFlavor;
import ro.cs.tao.topology.TopologyManager;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class NodeFlavorsUpdater extends BaseLifeCycle {

    @Override
    public int priority() {
        return 2;
    }

    @Override
    public void onStartUp() {
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

    @Override
    public void onShutdown() {

    }
}
