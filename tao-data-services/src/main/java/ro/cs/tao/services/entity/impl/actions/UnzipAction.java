package ro.cs.tao.services.entity.impl.actions;

import ro.cs.tao.services.model.ItemAction;
import ro.cs.tao.utils.FileUtilities;
import ro.cs.tao.workspaces.RepositoryType;

import java.nio.file.Path;

public class UnzipAction implements ItemAction {
    @Override
    public String name() {
        return "Unzip";
    }

    @Override
    public String[] supportedFiles() {
        return new String[] { ".zip" };
    }

    @Override
    public boolean isIntendedFor(RepositoryType repositoryType) {
        return repositoryType == RepositoryType.LOCAL;
    }

    @Override
    public Path doAction(Path item) throws Exception {
        FileUtilities.unzip(item, item.getParent(), true);
        return item;
    }
}
