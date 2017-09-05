package ro.cs.tao.services.topology.service;

import ro.cs.tao.topology.NodeDescription;

import java.util.List;

/**
 * @author Cosmin Cara
 */
public interface TopologyService {

    NodeDescription findByName(String hostName);

    List<NodeDescription> getAll();

    void saveNode(NodeDescription node);

    void deleteNode(String name);

}
