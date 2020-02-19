package ro.cs.tao.services.startup;

import ro.cs.tao.scheduling.ScheduleManager;

public class SchedulerInitializer extends BaseLifeCycle {
    @Override
    public int priority() { return 10; }

    @Override
    public void onStartUp() {
        ScheduleManager.start();
        logger.fine("Scheduling engine started");
    }

    @Override
    public void onShutdown() {
        ScheduleManager.stop();
        logger.fine("Scheduling engine stopped");
    }
}
