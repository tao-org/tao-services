package ro.cs.tao.services.entity.impl.actions;

import ro.cs.tao.eodata.OutputDataHandler;
import ro.cs.tao.services.model.ItemAction;
import ro.cs.tao.spi.ServiceRegistryManager;
import ro.cs.tao.workspaces.RepositoryType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class QuicklookAction implements ItemAction {
    @Override
    public String name() {
        return "Quicklook";
    }

    @Override
    public boolean isIntendedFor(RepositoryType repositoryType) {
        return repositoryType == RepositoryType.LOCAL;
    }

    @Override
    public Path doAction(Path item) throws Exception {
        if (item == null || !Files.exists(item)) {
            throw new IllegalArgumentException("[item] Invalid");
        }
        final Set<OutputDataHandler> services = ServiceRegistryManager.getInstance()
                                                                      .getServiceRegistry(OutputDataHandler.class)
                                                                      .getServices();
        List<OutputDataHandler<Path>> handlers = new ArrayList<>();
        for (OutputDataHandler odh : services) {
            if (Path.class.equals(odh.isIntendedFor()) && !odh.allowNext()) {
                handlers.add(odh);
            }
        }
        Path out = null;
        for (OutputDataHandler<Path> handler : handlers) {
            try {
                out = handler.handle(item);
            } catch (Exception e) {
                throw new Exception("Could not create quicklook for " + item, e);
            }
        }
        return out;
    }
}
