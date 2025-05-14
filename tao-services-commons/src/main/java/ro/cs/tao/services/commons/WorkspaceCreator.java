package ro.cs.tao.services.commons;

import ro.cs.tao.services.interfaces.StorageService;
import ro.cs.tao.spi.ServiceRegistryManager;
import ro.cs.tao.workspaces.Repository;
import ro.cs.tao.workspaces.RepositoryType;

import java.io.IOException;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * The WorkspaceCreator class is responsible for creating a root directory for a given workspace
 * based on its associated repository type and configuration. This class implements the
 * Consumer interface to allow it to process Repository instances.
 *
 * It checks the type of the provided repository and ensures that the root of the workspace
 * is created if the repository type is not local. This involves interacting with the
 * appropriate storage service based on the repository type's prefix to create the root directory.
 *
 * The WorkspaceCreator utilizes a service registry to dynamically retrieve the appropriate
 * StorageService implementation for the repository type.
 *
 * Logs exceptions or errors during the workspace creation process using Java's logging framework.
 */
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
