package ro.cs.tao.services.startup;

import ro.cs.tao.orchestration.Orchestrator;

public class OrchestratorInitializer extends BaseLifeCycle {
    @Override
    public int priority() { return 9; }

    @Override
    public void onStartUp() {
        Orchestrator.getInstance().start();
    }

    @Override
    public void onShutdown() {

    }
}
