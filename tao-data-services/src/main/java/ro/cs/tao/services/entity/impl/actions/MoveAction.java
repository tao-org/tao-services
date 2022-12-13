package ro.cs.tao.services.entity.impl.actions;

import ro.cs.tao.services.model.ItemAction;
import ro.cs.tao.utils.FileUtilities;
import ro.cs.tao.workspaces.RepositoryType;

import java.nio.file.Path;

public class MoveAction implements ItemAction {
    @Override
    public String name() {
        return "Move";
    }

    @Override
    public boolean isIntendedFor(RepositoryType repositoryType) {
        return repositoryType == RepositoryType.LOCAL;
    }

    @Override
    public void doAction(Path item, Path destination) throws Exception {
        FileUtilities.move(item, destination);
    }
}
