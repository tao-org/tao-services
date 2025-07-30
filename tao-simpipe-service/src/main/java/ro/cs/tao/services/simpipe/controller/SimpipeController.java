package ro.cs.tao.services.simpipe.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.cs.tao.component.DryRunMetricsBean;
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
