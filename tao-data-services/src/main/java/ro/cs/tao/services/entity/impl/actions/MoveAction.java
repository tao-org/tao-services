package ro.cs.tao.services.entity.impl.actions;

import ro.cs.tao.services.interfaces.StorageService;
import ro.cs.tao.services.model.ItemAction;
import ro.cs.tao.workspaces.RepositoryType;

import java.nio.file.Path;

public class MoveAction implements ItemAction {
    private StorageService storageService;

    @Override
    public String name() {
        return "Move";
    }

    @Override
    public boolean isIntendedFor(RepositoryType repositoryType) {
        return repositoryType == RepositoryType.LOCAL;
    }

    @Override
    public void associateWith(StorageService service) {
        this.storageService = service;
    }

    @Override
    public Path doAction(Path[] items, Path destination) throws Exception {
        if (items != null) {
            for (Path item : items) {
                this.storageService.move(item.toString(), destination.toString());
            }
        }
        return destination;
    }
}
