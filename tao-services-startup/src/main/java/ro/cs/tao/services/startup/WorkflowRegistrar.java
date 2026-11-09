package ro.cs.tao.services.startup;

import ro.cs.tao.services.interfaces.WorkflowBuilder;
import ro.cs.tao.spi.ServiceRegistry;
import ro.cs.tao.spi.ServiceRegistryManager;
import ro.cs.tao.workflow.WorkflowDescriptor;

import java.util.Set;

/**
 * WorkflowRegistrar is responsible for initializing and registering system-level
 * workflow descriptors for all {@link WorkflowBuilder} services available in the
 * {@link ServiceRegistry}. It executes this process during the start-up phase of
 * its lifecycle.
 *
 * This class is a part of the application lifecycle and extends {@link BaseLifeCycle},
 * inheriting its lifecycle management methods. It directly interacts with
 * {@link ServiceRegistryManager} to retrieve and process {@link WorkflowBuilder} services.
 *
 * During startup, the class retrieves all {@link WorkflowBuilder} services from the
 * {@link ServiceRegistry} and attempts to generate and register corresponding
 * {@link WorkflowDescriptor}s for system workflows. It logs the outcome of
 * each registration process separately for troubleshooting or debugging purposes.
 *
 * The class is structured with the following lifecycle methods inherited and implemented:
 *
 * - {@link #onStartUp()}:
 *   Executes the logic for processing and registering system-level workflow descriptors.
 *   If no services are found, it logs an informational message.
 *
 * - {@link #onShutdown()}:
 *   No specific shutdown behavior is implemented.
 *
 * - {@link #priority()}:
 *   Returns the priority level for the component's lifecycle execution.
 *
 * Exceptions occurring during workflow descriptor creation are caught and logged
 * at the warning level.
 */
public class WorkflowRegistrar extends BaseLifeCycle {
    @Override
    public int priority() {
        return 6;
    }

    @Override
    public void onStartUp() {
        ServiceRegistry<WorkflowBuilder> registry = ServiceRegistryManager.getInstance().getServiceRegistry(WorkflowBuilder.class);
        Set<WorkflowBuilder> services = registry.getServices();
        if (services == null || services.size() == 0) {
            logger.fine("System workflow library is empty");
        } else {
            for (WorkflowBuilder workflow : services) {
                try {
                    WorkflowDescriptor descriptor = workflow.createSystemWorkflowDescriptor();
                    if (descriptor != null) {
                        logger.finest(String.format("Registration completed for workflow %s", workflow.getName()));
                    } else {
                        logger.fine(String.format("Registration failed for workflow %s", workflow.getName()));
                    }
                } catch (Exception e) {
                    logger.warning(e.getMessage());
                }
            }
        }
    }

    @Override
    public void onShutdown() {

    }
}
