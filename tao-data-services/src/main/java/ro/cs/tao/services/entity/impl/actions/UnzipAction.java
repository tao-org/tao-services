package ro.cs.tao.services.entity.impl.actions;

import ro.cs.tao.services.model.ItemAction;
import ro.cs.tao.utils.FileUtilities;
import ro.cs.tao.utils.Zipper;
import ro.cs.tao.utils.executors.monitoring.ProgressListener;
import ro.cs.tao.workspaces.RepositoryType;

import java.nio.file.Files;
import java.nio.file.Path;

public class UnzipAction implements ItemAction {
    private ProgressListener listener;

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
    public void setProgressListener(ProgressListener listener) {
        this.listener = listener;
    }

    @Override
    public Path doAction(Path item) throws Exception {
        //FileUtilities.unzip(item, item.getParent(), true);
        if (item == null || !Files.exists(item)) {
            throw new IllegalArgumentException("[item] Invalid");
        }
        Zipper.decompressZip(item, item.getParent().resolve(FileUtilities.getFilenameWithoutExtension(item)), this.listener);
        return item;
    }
}
