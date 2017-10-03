package ro.cs.tao.services.impl;

import org.springframework.stereotype.Service;
import ro.cs.tao.services.interfaces.TopologyService;
import ro.cs.tao.topology.NodeDescription;
import ro.cs.tao.topology.TopologyManager;

import java.util.List;

/**
 * @author Cosmin Cara
 */
@Service("topologyService")
public class TopologyServiceImpl implements TopologyService {

    @Override
    public NodeDescription findById(String hostName) {
       return TopologyManager.getInstance().get(hostName);
    }

    @Override
    public List<NodeDescription> list() {
       return TopologyManager.getInstance().list();
    }

    @Override
    public void save(NodeDescription node) {
        TopologyManager.getInstance().add(node);
    }

    @Override
    public void update(NodeDescription node) {
        TopologyManager.getInstance().update(node);
    }

    @Override
    public void delete(String hostName) {
        TopologyManager.getInstance().remove(hostName);
    }
}
