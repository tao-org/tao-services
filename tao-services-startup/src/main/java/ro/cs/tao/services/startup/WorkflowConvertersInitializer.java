package ro.cs.tao.services.startup;

import ro.cs.tao.spi.ServiceRegistry;
import ro.cs.tao.spi.ServiceRegistryManager;
import ro.cs.tao.utils.executors.ExecutionDescriptorConverter;
import ro.cs.tao.utils.executors.Executor;

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
