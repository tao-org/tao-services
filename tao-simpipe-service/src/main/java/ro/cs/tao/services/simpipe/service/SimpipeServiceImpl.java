package ro.cs.tao.services.simpipe.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.component.DryRunMetrics;
import ro.cs.tao.drmaa.Environment;
import ro.cs.tao.execution.JobCompletedListener;
import ro.cs.tao.execution.model.ExecutionJob;
import ro.cs.tao.execution.model.ExecutionStatus;
import ro.cs.tao.execution.persistence.ExecutionJobProvider;
import ro.cs.tao.persistence.DryRunMetricsProvider;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.services.interfaces.OrchestratorService;
import ro.cs.tao.services.interfaces.WorkflowService;
import ro.cs.tao.services.simpipe.SimpipeClient;

import javax.annotation.PostConstruct;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

@Service("simpipeService")
public class SimpipeServiceImpl implements SimpipeService, JobCompletedListener {

    private static final Logger logger = Logger.getLogger(SimpipeServiceImpl.class.getName());
    private static SimpipeServiceImpl instance;

    @Autowired
    private ExecutionJobProvider jobProvider;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private DryRunMetricsProvider dryRunMetricsProvider;

    @Autowired
    private OrchestratorService orchestratorService;

    private SimpipeServiceImpl(){}

    @PostConstruct
    private void init(){
        instance = this;
    }

    public static SimpipeServiceImpl getInstance(){
        return instance;
    }

    @Override
    public List<DryRunMetrics> getDryRunMetricsForComponentIds(List<String> componentIds){
        if(componentIds == null || componentIds.isEmpty())
            throw new IllegalArgumentException("List of component IDs is null or empty");
        List<DryRunMetrics> metrics = new ArrayList<>();
        for (String componentId : componentIds) {
            DryRunMetrics componentMetrics = dryRunMetricsProvider.getByComponentId(componentId);
            if(componentMetrics != null) {
                metrics.add(componentMetrics);
            }
        }
        return metrics;
    }

    @Override
    public void onCompleted(ExecutionJob job) {
        if (job.getExecutionStatus() != ExecutionStatus.DONE || job.getEnvironment() != Environment.SIMPIPE) {
            return;
        }
        Executors.newSingleThreadExecutor().execute(() -> {
            logger.fine("SIM-PIPE dry run execution starting for job " + job.getName());
            Path yamlPath = Path.of(job.getJobOutputPath());
            SimpipeClient client = new SimpipeClient(job.getName().toLowerCase(), yamlPath, workflowService.getFullDescriptor(job.getWorkflowId()));
            List<DryRunMetrics> metrics = client.runClient();
            for (DryRunMetrics metric : metrics) {
                try {
                    dryRunMetricsProvider.save(metric);
                } catch (PersistenceException e) {
                    logger.severe("Error in persisting Dry Run metrics: " + e.getMessage());
                }
            }
        });
    }
}
