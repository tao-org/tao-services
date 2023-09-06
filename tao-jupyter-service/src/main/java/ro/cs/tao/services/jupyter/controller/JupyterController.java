package ro.cs.tao.services.jupyter.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.services.commons.ControllerBase;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.jupyter.impl.JupyterService;
import ro.cs.tao.services.jupyter.model.NotebookParams;

@RestController
public class JupyterController extends ControllerBase {
    @Autowired
    private JupyterService jupyterService;

    @RequestMapping(value = "/jupyter", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> generateTemplate(@RequestBody NotebookParams ps) {
        try {
            return prepareResult(jupyterService.generateNotebook(ps));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    ;
}
