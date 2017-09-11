package ro.cs.tao.services.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import ro.cs.tao.component.TaoComponent;
import ro.cs.tao.services.crud.BasicController;

/**
 * @author Cosmin Cara
 */
@Controller
@RequestMapping("/component")
public class ComponentController extends BasicController<TaoComponent> {

}
