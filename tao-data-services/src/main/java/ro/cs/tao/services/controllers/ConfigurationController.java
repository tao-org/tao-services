package ro.cs.tao.services.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import ro.cs.tao.services.commons.KeyValuePair;

/**
 * @author Cosmin Cara
 */
@Controller
@RequestMapping("/config")
public class ConfigurationController extends DataEntityController<KeyValuePair> {
}
