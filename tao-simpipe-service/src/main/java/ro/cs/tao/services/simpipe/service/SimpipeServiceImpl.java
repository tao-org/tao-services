package ro.cs.tao.services.simpipe.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.services.simpipe.SimpipeClient;
import ro.cs.tao.component.DryRunMetrics;
import ro.cs.tao.execution.ExecutionException;
import ro.cs.tao.execution.model.ExecutionJob;
import ro.cs.tao.execution.model.ExecutionRequest;
import ro.cs.tao.execution.model.ExecutionStatus;
import ro.cs.tao.execution.persistence.ExecutionJobProvider;
import ro.cs.tao.persistence.DryRunMetricsProvider;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.services.interfaces.OrchestratorService;
import ro.cs.tao.services.interfaces.WorkflowService;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Service("simpipeService")
public class SimpipeServiceImpl implements SimpipeService {

    private static final Logger logger = Logger.getLogger(SimpipeServiceImpl.class.getName());

    @Autowired
    private ExecutionJobProvider jobProvider;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private DryRunMetricsProvider dryRunMetricsProvider;

    @Autowired
    private OrchestratorService orchestratorService;

    @Override
    public ExecutionJob executeSimpipe(ExecutionRequest request) throws ExecutionException, PersistenceException {
        ExecutionJob job = orchestratorService.scriptJob(request);
        while (job.getExecutionStatus() != ExecutionStatus.DONE && job.getExecutionStatus() != ExecutionStatus.FAILED
                && job.getExecutionStatus() != ExecutionStatus.CANCELLED){
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e){
                logger.warning(e.getMessage());
            }
            job = jobProvider.get(job.getId());
        }
        if (job.getExecutionStatus() == ExecutionStatus.DONE) {
            logger.fine("SIM-PIPE dry run execution starting for job " + job.getName());
            Path yamlPath = Path.of(job.getJobOutputPath(), Path.of(job.getJobOutputPath()).getFileName().toString() + ".yaml");
            SimpipeClient client = new SimpipeClient(job.getName().toLowerCase(), yamlPath, workflowService.getFullDescriptor(job.getWorkflowId()));
            List<DryRunMetrics> metrics = client.runClient();
            for (DryRunMetrics metric : metrics) {
                dryRunMetricsProvider.save(metric);
            }
        }
        return job;
    }

    @Override
    public List<DryRunMetrics> getDryRunMetricsForComponentIds(List<String> componentIds){
        if(componentIds == null || componentIds.isEmpty())
            throw new IllegalArgumentException("List of component IDs is null or empty");
        List<DryRunMetrics> metrics = new ArrayList<>();
        for (String componentId : componentIds) {
            metrics.add(dryRunMetricsProvider.getByComponentId(componentId));
        }
        return metrics;
    }
}
