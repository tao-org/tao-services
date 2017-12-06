package ro.cs.tao.services.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ro.cs.tao.component.ProcessingComponent;
import ro.cs.tao.services.interfaces.ComponentService;

import java.util.List;

/**
 * @author Cosmin Cara
 */
@Controller
@RequestMapping("/component")
public class ComponentController extends DataEntityController<ProcessingComponent, ComponentService> {

    @RequestMapping(value = "/constraints", method = RequestMethod.GET)
    public ResponseEntity<?> listConstraints() {
        final List<String> objects = service.getAvailableConstraints();
        if (objects == null || objects.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(objects, HttpStatus.OK);
    }

}
