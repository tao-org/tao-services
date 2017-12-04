package ro.cs.tao.services.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import ro.cs.tao.docker.Container;

/**
 * @author Cosmin Cara
 */
@Controller
@RequestMapping("/docker")
public class ContainerController extends DataEntityController<Container> {
}
