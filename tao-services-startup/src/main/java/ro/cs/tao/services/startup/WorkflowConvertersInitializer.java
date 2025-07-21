package ro.cs.tao.services.startup;

import ro.cs.tao.spi.ServiceRegistry;
import ro.cs.tao.spi.ServiceRegistryManager;
import ro.cs.tao.utils.executors.ExecutionDescriptorConverter;
import ro.cs.tao.utils.executors.Executor;

/**
 * Initializes and manages the workflow converters during the application lifecycle.
 * This class is responsible for integrating all available implementations of
 * {@link ExecutionDescriptorConverter} into the application's executor framework.
 * It retrieves the converters from a service registry and applies them to the
 * conversion process.
 *
 * Inherits behavior from {@link BaseLifeCycle}, enabling lifecycle management such
 * as start-up and shutdown for the component.
 *
 * The initialization occurs during the onStartUp lifecycle phase. During this stage:
 * - A registry of {@link ExecutionDescriptorConverter} services is retrieved, if it exists.
 * - The converters from the registry are set in the {@code Executor} component for execution descriptor conversions.
 * - A log entry is added to confirm successful initialization of converters.
 *
 * The shutdown process does not have any additional logic implemented by this class.
 */
public class WorkflowConvertersInitializer extends BaseLifeCycle {
    @Override
    public int priority() {
        return 13;
    }

    @Override
    public void onStartUp() {
        ServiceRegistry<ExecutionDescriptorConverter> serviceRegistry = ServiceRegistryManager.getInstance().getServiceRegistry(ExecutionDescriptorConverter.class);
        if (serviceRegistry != null) {
            Executor.setConverters(serviceRegistry.getServices());
            logger.fine("Workflow converters initialized");
        }
    }

    @Override
    public void onShutdown() {

    }
}
