package ro.cs.tao.services.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ro.cs.tao.docker.Container;
import ro.cs.tao.services.interfaces.ContainerService;
import ro.cs.tao.topology.TopologyManager;

import java.util.List;

/**
 * @author Cosmin Cara
 */
@Controller
@RequestMapping("/docker")
public class ContainerController extends DataEntityController<Container, ContainerService> {
    @Override
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ResponseEntity<List<Container>> list() {
        List<Container> objects = TopologyManager.getInstance().getAvailableDockerImages();
        if (objects == null || objects.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(objects, HttpStatus.OK);
    }
}
