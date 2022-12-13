package ro.cs.tao.services.entity.impl.actions;

import ro.cs.tao.services.model.ItemAction;
import ro.cs.tao.utils.FileUtilities;
import ro.cs.tao.workspaces.RepositoryType;

import java.nio.file.Files;
import java.nio.file.Path;

public class CopyAction implements ItemAction {
    @Override
    public String name() {
        return "Copy";
    }

    @Override
    public boolean isIntendedFor(RepositoryType repositoryType) {
        return repositoryType == RepositoryType.LOCAL;
    }

    @Override
    public Path doAction(Path item, Path destination) throws Exception {
        FileUtilities.copy(item, destination);
        return Files.isDirectory(destination) ? destination.resolve(item.getFileName()) : destination;
    }
}
