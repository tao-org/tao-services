package ro.cs.tao.services.commons.update;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.cs.tao.services.commons.ControllerBase;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;

@RestController
public class RestartController extends ControllerBase {

    @Autowired
    private RestartService restartService;

    @PostMapping("/restart")
    public ResponseEntity<ServiceResponse<?>> restart() {
        asyncExecute(() -> {
            restartService.doRestart();
        });
        return prepareResult("Services restart initiated", ResponseStatus.SUCCEEDED);
    }
}
