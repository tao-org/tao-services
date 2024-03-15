package ro.cs.tao.services.startup;

import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.eodata.naming.NameExpressionParser;
import ro.cs.tao.eodata.naming.TokenResolver;
import ro.cs.tao.execution.drmaa.DRMAAExecutor;
import ro.cs.tao.execution.monitor.NodeManager;
import ro.cs.tao.orchestration.Orchestrator;
import ro.cs.tao.services.workspace.impl.JobFilesUpload;
import ro.cs.tao.spi.ServiceRegistry;
import ro.cs.tao.spi.ServiceRegistryManager;

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
        final ServiceRegistry<TokenResolver> registry = ServiceRegistryManager.getInstance().getServiceRegistry(TokenResolver.class);
        NameExpressionParser.setResolvers(registry.getServices());
        orchestrator.start();
    }

    @Override
    public void onShutdown() {

    }
}
