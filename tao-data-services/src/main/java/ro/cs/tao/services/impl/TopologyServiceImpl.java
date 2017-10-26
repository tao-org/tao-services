package ro.cs.tao.services.impl;

import org.springframework.stereotype.Service;
import ro.cs.tao.component.validation.ValidationException;
import ro.cs.tao.services.interfaces.TopologyService;
import ro.cs.tao.topology.NodeDescription;
import ro.cs.tao.topology.TopologyManager;

import java.util.List;

/**
 * @author Cosmin Cara
 */
@Service("topologyService")
public class TopologyServiceImpl
    extends ServiceBase<NodeDescription>
        implements TopologyService {

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

    @Override
    protected void validateFields(NodeDescription object, List<String> errors) throws ValidationException {
        String value = object.getHostName();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[hostName] cannot be empty");
        }
        value = object.getUserName();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[userName] cannot be empty");
        }
        value = object.getUserPass();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[password] cannot be empty");
        }
        if (object.getProcessorCount() <= 0) {
            errors.add("[processorCount] cannot be empty");
        }
        if (object.getMemorySizeGB() <= 0) {
            errors.add("[memorySize] cannot be empty");
        }
        if (object.getDiskSpaceSizeGB() <= 0) {
            errors.add("[diskSpace] cannot be empty");
        }
    }
}
