package ro.cs.tao.services.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import ro.cs.tao.topology.NodeDescription;

/**
 * @author Cosmin Cara
 */
@Controller
@RequestMapping("/topology")
public class TopologyController extends DataEntityController<NodeDescription> {

}
