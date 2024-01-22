package ro.cs.tao.services.jupyter.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import ro.cs.tao.persistence.RepositoryProvider;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.jupyter.impl.JupyterService;
import ro.cs.tao.services.jupyter.model.NotebookParams;
import ro.cs.tao.workspaces.Repository;
import ro.cs.tao.workspaces.RepositoryType;

import java.nio.file.Path;
import java.util.List;

@RestController
public class JupyterController extends BaseController {
    @Autowired
    private JupyterService jupyterService;
    @Autowired
    private RepositoryProvider repositoryProvider;

    @RequestMapping(value = "/jupyter", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> generateTemplate(@RequestBody List<NotebookParams> ps) {
        try {
            final Repository repository = getLocalRepository(currentUser());
            return prepareResult(jupyterService.generateNotebook(Path.of(repository.root()), ps));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/ReadNotebook", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> ReadNotebook() {
        try {
            final Repository repository = getLocalRepository(currentUser());
            return prepareResult(jupyterService.readNotebook(Path.of(repository.root())));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    private Repository getLocalRepository(String user) {
        return repositoryProvider.getUserSystemRepositories(user).stream().filter(w -> w.getType() == RepositoryType.LOCAL).findFirst().orElse(null);
    }
}
