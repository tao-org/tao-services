package ro.cs.tao.services.startup;

import ro.cs.tao.execution.Executor;
import ro.cs.tao.execution.TaskStatusListener;
import ro.cs.tao.execution.drmaa.DrmaaTaoExecutor;
import ro.cs.tao.execution.local.NullExecutor;
import ro.cs.tao.execution.local.QueryExecutor;
import ro.cs.tao.execution.model.ExecutionStatus;
import ro.cs.tao.execution.model.ExecutionTask;
import ro.cs.tao.messaging.Messaging;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.security.TaoLoginModule;
import ro.cs.tao.services.security.CustomAuthenticationProvider;
import ro.cs.tao.services.security.SpringSessionProvider;
import ro.cs.tao.spi.ServiceRegistryManager;

import javax.security.auth.spi.LoginModule;
import java.util.Set;

//import ro.cs.tao.execution.wps.WPSExecutor;

public class PersistenceInjecter extends BaseLifeCycle {

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public void onStartUp() {
        Executor.addStatusListener(new TaskStatusListener() {
            @Override
            public void taskStatusChanged(ExecutionTask task, ExecutionStatus status) {
                try {
                    persistenceManager.tasks().updateStatus(task, status);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void taskChanged(ExecutionTask task) {
                try {
                    persistenceManager.tasks().update(task);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        QueryExecutor.setProductProvider(persistenceManager.rasterData());
        QueryExecutor.setWorkflowProvider(persistenceManager.workflows());
        QueryExecutor.setDataSourceConfigurationProvider(persistenceManager.dataSourceConfigurationProvider());
        DrmaaTaoExecutor.setTaskProvider(persistenceManager.tasks());
        DrmaaTaoExecutor.setContainerProvider(persistenceManager.containers());
        NullExecutor.setWorkflowNodeProvider(persistenceManager.workflowNodes());
        NullExecutor.setComponentProvider(persistenceManager.processingComponents());
        //WPSExecutor.setTaskProvider(persistenceManager.tasks());
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
    }

    @Override
    public void onShutdown() {

    }
}
