package ro.cs.tao.services.startup;

import ro.cs.tao.scheduling.ScheduleManager;

/**
 * Initializes and manages the lifecycle of the scheduling engine.
 * Extends the {@code BaseLifeCycle} to provide startup and shutdown hooks
 * for controlling the {@code ScheduleManager}'s behavior.
 *
 * <ul>
 * <li>On startup, this class starts the scheduling engine by invoking {@code ScheduleManager.start()}.</li>
 * <li>On shutdown, this class stops the scheduling engine by invoking {@code ScheduleManager.stop()}.</li>
 * <li>Specifies its lifecycle priority to be 10.</li>
 * </ul>
 *
 * This class ensures that the scheduling engine is correctly started and stopped as part of the application's lifecycle.
 */
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
