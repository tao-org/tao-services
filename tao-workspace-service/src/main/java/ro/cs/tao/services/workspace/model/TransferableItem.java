package ro.cs.tao.services.workspace.model;

import ro.cs.tao.services.model.FileObject;
import ro.cs.tao.workspaces.Repository;

import java.util.Objects;

public class TransferableItem {
    private String user;
    private String batch;
    private Repository sourceRepository;
    private FileObject source;
    private Repository destinationRepository;
    private String destinationPath;
    private boolean move;
    private boolean force;

    public TransferableItem() {
    }

    public TransferableItem(String user, String batch, Repository sourceRepository, FileObject source, Repository destinationRepository, String destinationPath, boolean move, boolean force) {
        this.user = user;
        this.batch = batch;
        this.sourceRepository = sourceRepository;
        this.source = source;
        this.destinationRepository = destinationRepository;
        this.destinationPath = destinationPath;
        this.move = move;
        this.force = force;
    }

    public String getUser() {
        return user;
    }

    public String getBatch() {
        return batch;
    }

    public Repository getSourceRepository() {
        return sourceRepository;
    }

    public FileObject getSource() {
        return source;
    }

    public Repository getDestinationRepository() {
        return destinationRepository;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public boolean isMove() {
        return move;
    }

    public boolean isForce() {
        return force;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransferableItem that = (TransferableItem) o;
        return user.equals(that.user) && batch.equals(that.batch) && sourceRepository.equals(that.sourceRepository) && source.equals(that.source) && destinationRepository.equals(that.destinationRepository) && destinationPath.equals(that.destinationPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, batch, sourceRepository, source, destinationRepository, destinationPath);
    }

    @Override
    public String toString() {
        return "TransferableItem{" +
                "source=" + source +
                ", destinationPath='" + destinationPath + '\'' +
                '}';
    }
}
