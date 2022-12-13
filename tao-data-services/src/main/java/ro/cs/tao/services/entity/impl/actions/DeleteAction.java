package ro.cs.tao.services.entity.impl.actions;

import ro.cs.tao.services.model.ItemAction;
import ro.cs.tao.utils.FileUtilities;
import ro.cs.tao.workspaces.RepositoryType;

import java.nio.file.Files;
import java.nio.file.Path;

public class DeleteAction implements ItemAction {
    @Override
    public String name() {
        return "Delete";
    }

    @Override
    public boolean isIntendedFor(RepositoryType repositoryType) {
        return repositoryType == RepositoryType.LOCAL;
    }

    @Override
    public Path doAction(Path item) throws Exception {
        if (Files.isDirectory(item)) {
            FileUtilities.deleteTree(item);
        } else {
            Files.delete(item);
        }
        return item;
    }
}
