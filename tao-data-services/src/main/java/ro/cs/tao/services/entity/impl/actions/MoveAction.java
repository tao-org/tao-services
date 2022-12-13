package ro.cs.tao.services.entity.impl.actions;

import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.persistence.EOProductProvider;
import ro.cs.tao.services.model.ItemAction;
import ro.cs.tao.utils.FileUtilities;
import ro.cs.tao.workspaces.RepositoryType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class MoveAction implements ItemAction {
    private EOProductProvider productProvider;

    @Override
    public void setProductProvider(EOProductProvider productProvider) {
        this.productProvider = productProvider;
    }

    @Override
    public String name() {
        return "Move";
    }

    @Override
    public boolean isIntendedFor(RepositoryType repositoryType) {
        return repositoryType == RepositoryType.LOCAL;
    }

    @Override
    public Path doAction(Path item, Path destination) throws Exception {
        FileUtilities.move(item, destination);
        Path target = Files.isDirectory(destination) ? destination.resolve(item.getFileName()) : destination;
        List<EOProduct> products = productProvider.getByLocation(item.toUri().toString());
        if (products != null) {
            for (EOProduct product : products) {
                product.setLocation(target.toUri().toString());
                this.productProvider.update(product);
            }
        }
        return target;
    }
}
