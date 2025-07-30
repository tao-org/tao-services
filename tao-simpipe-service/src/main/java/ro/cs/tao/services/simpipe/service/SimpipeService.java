package ro.cs.tao.services.simpipe.service;

import ro.cs.tao.component.DryRunMetrics;
import ro.cs.tao.services.interfaces.TAOService;

import java.util.List;

/**
 * Interface for simpipe service
 *
 * @author Efrem-Hagiu Ion-Stefan
 */
public interface SimpipeService extends TAOService {


    /**
     * Retrieves the Dry Run Metrics for a list of Components that have been executed on SIM-PIPE
     *
     * @param componentIds  The list of Ids of the processing components for whom the Dry Run Metrics will be retrieved
     *
     */
    List<DryRunMetrics> getDryRunMetricsForComponentIds(List<String> componentIds);
}
