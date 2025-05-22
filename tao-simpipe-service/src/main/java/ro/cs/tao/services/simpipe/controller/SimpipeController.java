package ro.cs.tao.services.simpipe.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.component.DryRunMetricsBean;
import ro.cs.tao.execution.ExecutionException;
import ro.cs.tao.execution.model.ExecutionJob;
import ro.cs.tao.execution.model.ExecutionRequest;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.simpipe.service.SimpipeService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The SimpipeController class provides RESTful endpoints for managing and orchestrating
 * workflow executions, job management, and interactions with external services such as
 * Web Processing Service (WPS) and Web Map Service (WMS). It supports functionalities
 * for starting, stopping, pausing, resuming, and monitoring jobs, as well as managing
 * job history and task statuses.
 *
 * This class extends the BaseController and serves as a centralized controller
 * for workflow orchestration and job execution-related operations.
 *
 * Fields:
 * - simpipeService: Service for simpipe execution logic and dry run metrics handling.
 */
@RestController
@RequestMapping("/simpipe")
@Tag(name = "Simpipe", description = "Operations related to simpipe execution")
public class SimpipeController extends BaseController {

    @Autowired
    private SimpipeService simpipeService;

    /**
     * Exports a workflow execution in ARGO format and then executes a dry run on the SIM-PIPE app.
     * The workflow is not actually executed, but its execution is simulated in a dry run in order to calculate the metrics
     * required for choosing an appropriate node flavor for future executions of the components
     * @param request   The execution request
     */
    @RequestMapping(value = "/execute", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> executeSimPipe(@RequestBody ExecutionRequest request) {
        ResponseEntity<ServiceResponse<?>> response;
        try {
            final ExecutionJob job = simpipeService.executeSimpipe(request);
            if (job == null) {
                throw new ExecutionException("Failed to create ARGO Workflow and Dry run on SIMPIPE");
            }
            response = prepareResult(job.getJobOutputPath(),
                    "The script model for the job should be available in the path and the metrics of execution created!",
                    ResponseStatus.SUCCEEDED, HttpStatus.OK);
        } catch (Exception ex) {
            response = handleException(ex);
        }
        return response;
    }

    /**
     * Returns the dry run metrics of one or more components, given their ids.
     * @param idList    The list of identifiers
     */
    @RequestMapping(value = "/metrics", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> metrics(@RequestParam(name = "id") String idList) {
        if (idList == null || idList.isEmpty()) {
            return prepareResult("Invalid id list", ResponseStatus.FAILED);
        }
        String[] ids = idList.split(",");
        return prepareResult(simpipeService.getDryRunMetricsForComponentIds(List.of(ids)).stream().map(DryRunMetricsBean::new).collect(Collectors.toList()));
    }
}
