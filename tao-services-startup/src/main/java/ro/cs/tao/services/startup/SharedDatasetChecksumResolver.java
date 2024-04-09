package ro.cs.tao.services.startup;

import ro.cs.tao.persistence.managers.DataSubscriptionManager;
import ro.cs.tao.persistence.managers.RepositoryManager;
import ro.cs.tao.services.factory.StorageServiceFactory;
import ro.cs.tao.services.interfaces.StorageService;
import ro.cs.tao.subscription.DataSubscription;
import ro.cs.tao.workspaces.Repository;

import java.util.List;

public class SharedDatasetChecksumResolver extends BaseLifeCycle{
    @Override
    public int priority() {
        return 99;
    }

    @Override
    public void onStartUp() {
        final DataSubscriptionManager dataSubscriptionManager = persistenceManager.dataSubscription();
        final List<DataSubscription> list = dataSubscriptionManager.getUnckecked();
        if (list != null && !list.isEmpty()) {
            final RepositoryManager repositoryManager = persistenceManager.repositories();
            for (DataSubscription subscription : list) {
                final Repository repository = repositoryManager.get(subscription.getRepositoryId());
                if (repository != null) {
                    StorageService instance = StorageServiceFactory.getInstance(repository);
                    instance.associate(repository);
                    try {
                        subscription.setCheckSum(instance.computeHash(subscription.getDataRootPath()));
                        dataSubscriptionManager.update(subscription);
                    } catch (Exception e) {
                        logger.severe(e.getMessage());
                    }
                }
            }
        }
    }

    @Override
    public void onShutdown() {
        // NO OP
    }
}
