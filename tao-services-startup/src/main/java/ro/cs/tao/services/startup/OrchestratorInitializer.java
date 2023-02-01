package ro.cs.tao.services.startup;

import ro.cs.tao.execution.drmaa.DRMAAExecutor;
import ro.cs.tao.orchestration.Orchestrator;

public class OrchestratorInitializer extends BaseLifeCycle {
    @Override
    public int priority() { return 9; }

    @Override
    public void onStartUp() {
        final Orchestrator orchestrator = Orchestrator.getInstance();
        DRMAAExecutor.setApplicationId(orchestrator.getId());
        orchestrator.start();
    }

    @Override
    public void onShutdown() {

    }
}
