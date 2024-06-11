package ro.cs.tao.services.entity.impl.actions;

import ro.cs.tao.services.model.ItemAction;
import ro.cs.tao.utils.FileUtilities;
import ro.cs.tao.utils.Zipper;
import ro.cs.tao.utils.executors.monitoring.ProgressListener;
import ro.cs.tao.workspaces.RepositoryType;

import java.nio.file.Files;
import java.nio.file.Path;

public class ZipAction implements ItemAction {
    private ProgressListener listener;

    @Override
    public String name() {
        return "Zip";
    }

    @Override
    public boolean isIntendedFor(RepositoryType repositoryType) {
        return repositoryType == RepositoryType.LOCAL;
    }

    @Override
    public void setProgressListener(ProgressListener listener) {
        this.listener = listener;
    }

    @Override
    public Path doAction(Path item) throws Exception {
        if (item == null || !Files.exists(item)) {
            throw new IllegalArgumentException("[item] Invalid");
        }
        final String targetFileName = FileUtilities.getFilenameWithoutExtension(item);
        Zipper.compress(item, targetFileName, false, this.listener);
        return item.getParent().resolve(targetFileName);
    }
}
