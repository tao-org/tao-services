package ro.cs.tao.services.topology.service;

import org.springframework.stereotype.Service;
import ro.cs.tao.topology.NodeDescription;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Cosmin Cara
 */
@Service("topologyService")
public class TopologyServiceImpl implements TopologyService {
    @Override
    public NodeDescription findByName(String hostName) {
        NodeDescription node = new NodeDescription();
        node.setHostName("host_sample");
        node.setIpAddr("10.0.0.1");
        node.setUserName("user");
        node.setUserPass("drowssap");
        node.setProcessorCount(4);
        node.setMemorySizeGB(16);
        node.setDiskSpaceSizeGB(500);
        return node;
    }

    @Override
    public List<NodeDescription> getAll() {
        List<NodeDescription> list = new ArrayList<NodeDescription>();
        NodeDescription node = new NodeDescription();
        node.setHostName("host_sample_1");
        node.setIpAddr("10.0.0.1");
        node.setUserName("user");
        node.setUserPass("drowssap");
        node.setProcessorCount(4);
        node.setMemorySizeGB(16);
        node.setDiskSpaceSizeGB(500);
        list.add(node);

        node = new NodeDescription();
        node.setHostName("host_sample_2");
        node.setIpAddr("10.0.0.2");
        node.setUserName("user");
        node.setUserPass("drowssap");
        node.setProcessorCount(4);
        node.setMemorySizeGB(16);
        node.setDiskSpaceSizeGB(500);
        list.add(node);
        return list;
    }

    @Override
    public void saveNode(NodeDescription node) {
        //no-op
    }

    @Override
    public void deleteNode(String name) {
        //no-op
    }
}
