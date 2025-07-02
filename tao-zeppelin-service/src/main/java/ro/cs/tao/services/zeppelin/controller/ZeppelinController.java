package ro.cs.tao.services.zeppelin.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
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
import ro.cs.tao.services.zeppelin.model.notebook.NotebookParams;
import ro.cs.tao.services.zeppelin.service.ZeppelinService;
import ro.cs.tao.workspaces.Repository;
import ro.cs.tao.workspaces.RepositoryType;

import java.nio.file.Path;
import java.util.List;

/**
 * @author Efrem-Hagiu Stefan
 */
@RestController
@RequestMapping("/zeppelin")
@Tag(name = "Zeppelin", description = "Operations related to Apache Zeppelin instance creation ")
public class ZeppelinController extends BaseController {

    @Autowired
    private ZeppelinService zeppelinService;
    @Autowired
    private RepositoryProvider repositoryProvider;

    @RequestMapping(value = "/start", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> generateNotebook(@RequestBody List<NotebookParams> ps) {
        try {
            final Repository repository = getLocalRepository(currentUser());
            return prepareResult(zeppelinService.generateNotebook(Path.of(repository.root()), ps));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/readNotebook", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> ReadNotebook() {
        try {
            final Repository repository = getLocalRepository(currentUser());
            return prepareResult(zeppelinService.readNotebook(Path.of(repository.root())));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    private Repository getLocalRepository(String user) {
        return repositoryProvider.getUserSystemRepositories(user).stream().filter(w -> w.getType() == RepositoryType.LOCAL).findFirst().orElse(null);
    }

}
