package ro.cs.tao.services.simpipe.service;

import ro.cs.tao.component.DryRunMetrics;
import ro.cs.tao.execution.ExecutionException;
import ro.cs.tao.execution.model.ExecutionJob;
import ro.cs.tao.execution.model.ExecutionRequest;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.services.interfaces.TAOService;

import java.util.List;

/**
 * Interface for simpipe service
 *
 * @author Efrem-Hagiu Ion-Stefan
 */
public interface SimpipeService extends TAOService {

    /**
     * Creates an ARGO Script job for a workflow definition. After the creation of the ARGO Workflow it executes a dry
     * run on the SIM-PIPE app and persists the Dry Run Metrics retrieved.
     *
     * @param request       The execution request, containing:
     *  - The workflow identifier
     *  - A friendly name for this execution
     *  - Argo format in which the Workflow will be exported
     *  - The overridden parameter values for components
     *
     * @throws ExecutionException   In case anything goes wrong with executing the Job or the Dry Run
     * @throws PersistenceException   In case anything goes wrong with persisting the Dry Run Metrics
     */
    ExecutionJob executeSimpipe(ExecutionRequest request) throws ExecutionException, PersistenceException;

    /**
     * Creates an ARGO Script job for a workflow definition. After the creation of the ARGO Workflow it executes a dry
     * run on the SIM-PIPE app and persists the Dry Run Metrics retrieved.
     *
     * @param componentIds  The list of Ids of the processing components for whom the Dry Run Metrics will be retrieved
     *
     */
    List<DryRunMetrics> getDryRunMetricsForComponentIds(List<String> componentIds);
}
