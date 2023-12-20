package ro.cs.tao.services.startup;

import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.execution.drmaa.DRMAAExecutor;
import ro.cs.tao.execution.monitor.NodeManager;
import ro.cs.tao.orchestration.Orchestrator;
import ro.cs.tao.services.workspace.impl.JobFilesUpload;

public class OrchestratorInitializer extends BaseLifeCycle {
	
	/**	Upload results to bucket. */
	private static final String UPLOAD_RESULTS_TO_BUCKET = "tao.upload.to.bucket";
	
    @Override
    public int priority() { return 9; }

    @Override
    public void onStartUp() {
        final Orchestrator orchestrator = Orchestrator.getInstance();
        DRMAAExecutor.setApplicationId(orchestrator.getId());
        NodeManager.getInstance().setApplicationId(orchestrator.getId());
        if (ConfigurationManager.getInstance().getBooleanValue(UPLOAD_RESULTS_TO_BUCKET)) {
            // register the listener implementation that moves the files to the user's remote repository on job completion
            orchestrator.addJobListener(new JobFilesUpload());
        }
        orchestrator.start();
    }

    @Override
    public void onShutdown() {

    }
}
