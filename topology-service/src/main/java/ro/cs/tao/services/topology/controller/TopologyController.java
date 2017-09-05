package ro.cs.tao.services.topology.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ro.cs.tao.services.commons.ServiceError;
import ro.cs.tao.services.topology.service.TopologyService;
import ro.cs.tao.topology.NodeDescription;

import java.util.List;

/**
 * @author Cosmin Cara
 */
@Controller
@RequestMapping("/topology")
public class TopologyController {

    @Autowired
    private TopologyService topologyService;

    @RequestMapping(value = "/node/getbyname/{name}", method = RequestMethod.GET)
    public ResponseEntity<?> getByName(@PathVariable("name") String name) {
        NodeDescription node = topologyService.findByName(name);
        if (node == null) {
            return new ResponseEntity<>(new ServiceError(String.format("Node [%s] not found", name)),
                                        HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(node, HttpStatus.OK);
    }

    @RequestMapping(value = "/node/list/", method = RequestMethod.GET)
    public ResponseEntity<List<NodeDescription>> getAll() {
        List<NodeDescription> nodes = topologyService.getAll();
        if (nodes == null || nodes.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(nodes, HttpStatus.OK);
    }

    @RequestMapping(value = "/node/update/{name}", method = RequestMethod.PUT)
    public ResponseEntity<?> save(@PathVariable("name") String name, @RequestBody NodeDescription node) {
        topologyService.saveNode(node);
        return new ResponseEntity<>(node, HttpStatus.OK);
    }

    @RequestMapping(value = "/node/{name}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteNode(@PathVariable("name") String name) {
        topologyService.deleteNode(name);
        return new ResponseEntity<NodeDescription>(HttpStatus.NO_CONTENT);
    }
}
