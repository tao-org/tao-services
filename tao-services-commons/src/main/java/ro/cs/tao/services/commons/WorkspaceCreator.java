package ro.cs.tao.services.commons;

import ro.cs.tao.services.interfaces.StorageService;
import ro.cs.tao.spi.ServiceRegistryManager;
import ro.cs.tao.workspaces.Repository;
import ro.cs.tao.workspaces.RepositoryType;

import java.io.IOException;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class WorkspaceCreator implements Consumer<Repository> {
    private final Logger logger = Logger.getLogger(WorkspaceCreator.class.getName());
    @Override
    public void accept(Repository repository) {
        final RepositoryType type = repository.getType();
        if (type != RepositoryType.LOCAL) {
            final Set<StorageService> services = ServiceRegistryManager.getInstance().getServiceRegistry(StorageService.class).getServices();
            final StorageService storageService = services.stream().filter(s -> s.isIntendedFor(type.prefix())).findFirst().orElse(null);
            if (storageService != null) {
                try {
                    storageService.createRoot(repository.root());
                } catch (IOException e) {
                    this.logger.severe(e.getMessage());
                }
            }
        }
    }
}
