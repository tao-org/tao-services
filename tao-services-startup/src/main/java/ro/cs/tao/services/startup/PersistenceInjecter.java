package ro.cs.tao.services.startup;

import ro.cs.tao.component.ConditionComponent;
import ro.cs.tao.component.constraints.ConstraintFactory;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.configuration.ConfigurationProvider;
import ro.cs.tao.execution.Executor;
import ro.cs.tao.execution.TaskStatusListener;
import ro.cs.tao.execution.drmaa.ConditionExecutor;
import ro.cs.tao.execution.drmaa.DRMAAExecutor;
import ro.cs.tao.execution.local.NullExecutor;
import ro.cs.tao.execution.local.QueryExecutor;
import ro.cs.tao.execution.model.ExecutionStatus;
import ro.cs.tao.execution.model.ExecutionTask;
import ro.cs.tao.execution.model.ResourceUsage;
import ro.cs.tao.execution.ogc.OGCProcessExecutor;
import ro.cs.tao.execution.wms.WMSExecutor;
import ro.cs.tao.execution.wps.WPSExecutor;
import ro.cs.tao.messaging.Messaging;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.managers.ResourceUsageManager;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.security.SystemPrincipal;
import ro.cs.tao.security.TaoLoginModule;
import ro.cs.tao.services.commons.ContainerTunnelFilter;
import ro.cs.tao.services.factory.StorageServiceFactory;
import ro.cs.tao.services.model.ItemAction;
import ro.cs.tao.services.security.CustomAuthenticationProvider;
import ro.cs.tao.services.security.SpringSessionProvider;
import ro.cs.tao.spi.ServiceRegistry;
import ro.cs.tao.spi.ServiceRegistryManager;
import ro.cs.tao.topology.NodeFlavor;
import ro.cs.tao.utils.ExceptionUtils;

import javax.security.auth.spi.LoginModule;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * The {@code PersistenceInjecter} class is responsible for managing the initialization
 * and configuration of persistence-related components and services within the application lifecycle.
 * It extends {@code BaseLifeCycle} to utilize lifecycle hooks for startup and shutdown processes.
 *
 * This class performs the following key tasks during the startup process:
 * 1. Refreshes the system principal using user data from the persistence manager.
 * 2. Registers a {@link TaskStatusListener} to handle task status changes and update
 *    the corresponding persistence states.
 * 3. Configures various executors and services with the appropriate persistence data providers.
 * 4. Initializes and saves predefined condition components if they do not already exist.
 * 5. Sets up session providers and authentication-related providers for the security framework.
 *
 * The shutdown process is defined but does not currently implement any specific functionality.
 *
 * This class interacts heavily with multiple subsystems such as task handling,
 * job management, resource usage tracking, and data storage to ensure proper initialization
 * and persistence integration across the application.
 */
public class PersistenceInjecter extends BaseLifeCycle {

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public void onStartUp() {
        SystemPrincipal.refresh(persistenceManager.users());
        Executor.addStatusListener(new TaskStatusListener() {
            final EnumSet<ExecutionStatus> stoppedStates = EnumSet.of(ExecutionStatus.DONE,
                                                                      ExecutionStatus.FAILED,
                                                                      ExecutionStatus.CANCELLED,
                                                                      ExecutionStatus.PENDING_FINALISATION);
            final Logger logger = Logger.getLogger(TaskStatusListener.class.getName());
            @Override
            public void taskStatusChanged(ExecutionTask task, ExecutionStatus status, String reason) {
                try {
                    persistenceManager.tasks().updateStatus(task, status, reason);
                    final ResourceUsageManager resourceUsageManager = persistenceManager.resourceUsage();
                    ResourceUsage usage = resourceUsageManager.getByTask(task.getId());
                    if (ExecutionStatus.RUNNING == status) {
                        if (task.getJob().getTasks().stream().filter(t -> t.getExecutionStatus().equals(status)).count() == 1) {
                            // this is the first task running
                            persistenceManager.jobs().update(task.getJob());
                        }
                        if (usage == null) {
                            usage = new ResourceUsage();
                            usage.setUserId(task.getJob().getUserId());
                            usage.setTaskId(task.getId());
                            usage.setHost(task.getExecutionNodeHostName());
                            final NodeFlavor flavor = persistenceManager.nodes().get(usage.getHost()).getFlavor();
                            if (flavor != null) {
                                usage.setFlavor(flavor.getId());
                            }
                            usage.setStartTime(task.getStartTime());
                            resourceUsageManager.save(usage);
                        }
                    } else if (stoppedStates.contains(status)) {
                        if (usage != null) {
                            usage.setEndTime(task.getEndTime());
                            resourceUsageManager.update(usage);
                        }
                    }
                } catch (Exception e) {
                    logger.severe(ExceptionUtils.getStackTrace(logger, e));
                }
            }

            @Override
            public void taskChanged(ExecutionTask task) {
                try {
                    task = persistenceManager.tasks().update(task);
                } catch (Exception e) {
                    logger.warning(ExceptionUtils.getStackTrace(logger, e));
                }
            }
        });
        final ConfigurationProvider configurationProvider = ConfigurationManager.getInstance();
        final ServiceRegistryManager registryManager = ServiceRegistryManager.getInstance();
        configurationProvider.setPersistentConfigurationProvider(persistenceManager.configuration());
        ro.cs.tao.utils.executors.Executor.setLocalSudoUser(configurationProvider.getValue("topology.master.user"));
        ro.cs.tao.utils.executors.Executor.setLocalSudoPwd(configurationProvider.getValue("topology.master.password"));
        QueryExecutor.setProductProvider(persistenceManager.rasterData());
        QueryExecutor.setWorkflowProvider(persistenceManager.workflows());
        QueryExecutor.setDataSourceConfigurationProvider(persistenceManager.dataSourceConfigurationProvider());
        QueryExecutor.setTaskProvider(persistenceManager.tasks());
        QueryExecutor.setJobProvider(persistenceManager.jobs());
        QueryExecutor.setComponentProvider(persistenceManager.dataSourceComponents());
        DRMAAExecutor.setTaskProvider(persistenceManager.tasks());
        DRMAAExecutor.setJobProvider(persistenceManager.jobs());
        DRMAAExecutor.setContainerProvider(persistenceManager.containers());
        WPSExecutor.setTaskProvider(persistenceManager.tasks());
        WPSExecutor.setJobProvider(persistenceManager.jobs());
        WPSExecutor.setAuthenticationProvider(persistenceManager.webServiceAuthentication());
        WMSExecutor.setTaskProvider(persistenceManager.tasks());
        WMSExecutor.setJobProvider(persistenceManager.jobs());
        WMSExecutor.setAuthenticationProvider(persistenceManager.webServiceAuthentication());
        OGCProcessExecutor.setTaskProvider(persistenceManager.tasks());
        OGCProcessExecutor.setJobProvider(persistenceManager.jobs());
        OGCProcessExecutor.setAuthenticationProvider(persistenceManager.webServiceAuthentication());
        NullExecutor.setWorkflowNodeProvider(persistenceManager.workflowNodes());
        NullExecutor.setComponentProvider(persistenceManager.processingComponents());
        ConditionExecutor.setTaskProvider(persistenceManager.tasks());
        Messaging.setPersister(this.persistenceManager.notifications());
        logger.fine("Persistence injection completed");
        SpringSessionProvider.setPersistenceManager(this.persistenceManager);
        SessionStore.setSessionContextProvider(new SpringSessionProvider());
        final Set<LoginModule> loginModules = registryManager.getServiceRegistry(LoginModule.class).getServices();
        for (LoginModule module : loginModules) {
            if (TaoLoginModule.class.isAssignableFrom(module.getClass())) {
                TaoLoginModule.setUserProvider(this.persistenceManager.users());
            }
        }
        CustomAuthenticationProvider.setPersistenceManager(this.persistenceManager);
        StorageServiceFactory.setProductProvider(this.persistenceManager.rasterData());
        StorageServiceFactory.setVectorDataProvider(this.persistenceManager.vectorData());
        StorageServiceFactory.setAuxiliaryDataProvider(this.persistenceManager.auxiliaryData());
        ServiceRegistry<ItemAction> registry = registryManager.getServiceRegistry(ItemAction.class);
        Set<ItemAction> services = registry.getServices();
        for (ItemAction action : services) {
            action.setProductProvider(this.persistenceManager.rasterData());
        }
        ContainerTunnelFilter.setContainerInstanceProvider(this.persistenceManager.containerInstance());
        ConstraintFactory.setNamingRuleProvider(this.persistenceManager.namingRules());
        // Condition components
        ConditionComponent component = this.persistenceManager.conditionComponents()
                                                              .get(ConditionComponent.ONE_CONDITION_COMPONENT_ID);
        if (component == null) {
            component = ConditionComponent.oneConditionComponent();
            try {
                this.persistenceManager.conditionComponents().save(component);
            } catch (PersistenceException e) {
                logger.warning(e.getMessage());
            }
        }
        component = this.persistenceManager.conditionComponents()
                                           .get(ConditionComponent.TWO_CONDITIONS_COMPONENT_ID);
        if (component == null) {
            component = ConditionComponent.twoConditionsComponent();
            try {
                this.persistenceManager.conditionComponents().save(component);
            } catch (PersistenceException e) {
                logger.warning(e.getMessage());
            }
        }
    }

    @Override
    public void onShutdown() {

    }
}
