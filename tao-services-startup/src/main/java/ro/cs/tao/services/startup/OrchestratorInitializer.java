package ro.cs.tao.services.startup;

import ro.cs.tao.eodata.naming.NameExpressionParser;
import ro.cs.tao.eodata.naming.TokenResolver;
import ro.cs.tao.execution.JobCompletedListener;
import ro.cs.tao.execution.drmaa.DRMAAExecutor;
import ro.cs.tao.execution.monitor.NodeManager;
import ro.cs.tao.orchestration.Orchestrator;
import ro.cs.tao.services.bridge.spring.SpringContextBridge;
import ro.cs.tao.services.workspace.impl.JobFilesUpload;
import ro.cs.tao.spi.ServiceRegistry;
import ro.cs.tao.spi.ServiceRegistryManager;

import java.util.Map;

/**
 * The OrchestratorInitializer class is responsible for setting up and initializing the orchestrator
 * during the application startup process. It extends the BaseLifeCycle class and provides specific
 * implementations for managing the orchestrator's lifecycle events.
 *
 * During startup, the orchestrator is initialized with application identifiers and relevant configurations.
 * Necessary listeners and resolvers are also registered to facilitate orchestrator operations. Once all
 * initializations are complete, the orchestrator is started.
 *
 * This class ensures that the orchestrator is properly integrated with the system components, such as
 * the node manager and job execution environment, to provide a seamless workflow execution process.
 *
 * Priority for this lifecycle component is set to 9, indicating its relative startup order in the system.
 */
public class OrchestratorInitializer extends BaseLifeCycle {
	
    @Override
    public int priority() { return 9; }

    @Override
    public void onStartUp() {
        final Orchestrator orchestrator = Orchestrator.getInstance();
        DRMAAExecutor.setApplicationId(orchestrator.getId());
        NodeManager.getInstance().setApplicationId(orchestrator.getId());
        // add the job listener to upload the files to the remote repository
        orchestrator.addJobListener(new JobFilesUpload());
        //add additional JobListeners from the Spring Context
        Map<String,JobCompletedListener> jobListeners = SpringContextBridge.services().getBeansOfTypes(JobCompletedListener.class);
        for(JobCompletedListener listener : jobListeners.values()){
            orchestrator.addJobListener(listener);
        }
        final ServiceRegistry<TokenResolver> registry = ServiceRegistryManager.getInstance().getServiceRegistry(TokenResolver.class);
        NameExpressionParser.setResolvers(registry.getServices());
        orchestrator.start();
    }

    @Override
    public void onShutdown() {

    }
}
