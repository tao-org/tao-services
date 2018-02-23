package ro.cs.tao.services.entity.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import ro.cs.tao.services.interfaces.ConfigurationService;
import ro.cs.tao.services.model.KeyValuePair;

/**
 * @author Cosmin Cara
 */
@Controller
@RequestMapping("/config")
public class ConfigurationController extends DataEntityController<KeyValuePair, ConfigurationService> {
}