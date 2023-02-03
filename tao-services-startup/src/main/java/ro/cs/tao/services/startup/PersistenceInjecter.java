package ro.cs.tao.services.startup;

import ro.cs.tao.execution.Executor;
import ro.cs.tao.execution.TaskStatusListener;
import ro.cs.tao.execution.drmaa.DRMAAExecutor;
import ro.cs.tao.execution.local.NullExecutor;
import ro.cs.tao.execution.local.QueryExecutor;
import ro.cs.tao.execution.model.ExecutionStatus;
import ro.cs.tao.execution.model.ExecutionTask;
import ro.cs.tao.execution.wps.WPSExecutor;
import ro.cs.tao.messaging.Messaging;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.security.TaoLoginModule;
import ro.cs.tao.services.factory.StorageServiceFactory;
import ro.cs.tao.services.model.ItemAction;
import ro.cs.tao.services.security.CustomAuthenticationProvider;
import ro.cs.tao.services.security.SpringSessionProvider;
import ro.cs.tao.spi.ServiceRegistry;
import ro.cs.tao.spi.ServiceRegistryManager;
import ro.cs.tao.utils.ExceptionUtils;

import javax.security.auth.spi.LoginModule;
import java.util.Set;
import java.util.logging.Logger;

public class PersistenceInjecter extends BaseLifeCycle {

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public void onStartUp() {
        Executor.addStatusListener(new TaskStatusListener() {
            final Logger logger = Logger.getLogger(TaskStatusListener.class.getName());
            @Override
            public void taskStatusChanged(ExecutionTask task, ExecutionStatus status, String reason) {
                try {
                    persistenceManager.tasks().updateStatus(task, status, reason);
                } catch (Exception e) {
                    logger.severe(ExceptionUtils.getStackTrace(logger, e));
                }
            }

            @Override
            public void taskChanged(ExecutionTask task) {
                try {
                    task = persistenceManager.tasks().update(task);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        QueryExecutor.setProductProvider(persistenceManager.rasterData());
        QueryExecutor.setWorkflowProvider(persistenceManager.workflows());
        QueryExecutor.setDataSourceConfigurationProvider(persistenceManager.dataSourceConfigurationProvider());
        QueryExecutor.setTaskProvider(persistenceManager.tasks());
        DRMAAExecutor.setTaskProvider(persistenceManager.tasks());
        DRMAAExecutor.setContainerProvider(persistenceManager.containers());
        WPSExecutor.setTaskProvider(persistenceManager.tasks());
        WPSExecutor.setJobProvider(persistenceManager.jobs());
        WPSExecutor.setAuthenticationProvider(persistenceManager.wpsAuthentication());
        NullExecutor.setWorkflowNodeProvider(persistenceManager.workflowNodes());
        NullExecutor.setComponentProvider(persistenceManager.processingComponents());
        Messaging.setPersister(this.persistenceManager.notifications());
        logger.fine("Persistence injection completed");
        SpringSessionProvider.setPersistenceManager(this.persistenceManager);
        SessionStore.setSessionContextProvider(new SpringSessionProvider());
        final Set<LoginModule> loginModules = ServiceRegistryManager.getInstance().getServiceRegistry(LoginModule.class).getServices();
        for (LoginModule module : loginModules) {
            if (TaoLoginModule.class.isAssignableFrom(module.getClass())) {
                TaoLoginModule.setUserProvider(this.persistenceManager.users());
            }
        }
        CustomAuthenticationProvider.setPersistenceManager(this.persistenceManager);
        StorageServiceFactory.setProductProvider(this.persistenceManager.rasterData());
        StorageServiceFactory.setVectorDataProvider(this.persistenceManager.vectorData());
        StorageServiceFactory.setAuxiliaryDataProvider(this.persistenceManager.auxiliaryData());
        ServiceRegistry<ItemAction> registry = ServiceRegistryManager.getInstance().getServiceRegistry(ItemAction.class);
        Set<ItemAction> services = registry.getServices();
        for (ItemAction action : services) {
            action.setProductProvider(this.persistenceManager.rasterData());
        }
    }

    @Override
    public void onShutdown() {

    }
}
