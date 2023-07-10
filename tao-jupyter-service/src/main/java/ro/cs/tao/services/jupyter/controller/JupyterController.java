package ro.cs.tao.services.jupyter.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.services.commons.ControllerBase;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.jupyter.impl.JupyterService;
import ro.cs.tao.services.jupyter.model.Notebook;

@RestController
public class JupyterController extends ControllerBase {
    @Autowired
    private JupyterService jupyterService;
    @RequestMapping(value = "/jupyter", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> foo(@RequestBody Notebook ws){
        try {
            return prepareResult(jupyterService.generateNotebook(ws));
        } catch (Exception e) {
            return handleException(e);
        }
    };
}
