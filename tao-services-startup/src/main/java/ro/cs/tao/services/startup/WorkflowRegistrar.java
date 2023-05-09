package ro.cs.tao.services.startup;

import ro.cs.tao.services.interfaces.WorkflowBuilder;
import ro.cs.tao.spi.ServiceRegistry;
import ro.cs.tao.spi.ServiceRegistryManager;
import ro.cs.tao.workflow.WorkflowDescriptor;

import java.util.Set;

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
