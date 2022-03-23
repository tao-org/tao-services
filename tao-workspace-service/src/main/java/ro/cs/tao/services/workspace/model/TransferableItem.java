package ro.cs.tao.services.workspace.model;

import ro.cs.tao.services.model.FileObject;
import ro.cs.tao.topology.Workspace;

import java.util.Objects;

public class TransferableItem {
    private String user;
    private String batch;
    private Workspace sourceWorkspace;
    private FileObject source;
    private Workspace destinationWorkspace;
    private String destinationPath;
    private boolean move;
    private boolean force;

    public TransferableItem() {
    }

    public TransferableItem(String user, String batch, Workspace sourceWorkspace, FileObject source, Workspace destinationWorkspace, String destinationPath, boolean move, boolean force) {
        this.user = user;
        this.batch = batch;
        this.sourceWorkspace = sourceWorkspace;
        this.source = source;
        this.destinationWorkspace = destinationWorkspace;
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

    public Workspace getSourceWorkspace() {
        return sourceWorkspace;
    }

    public FileObject getSource() {
        return source;
    }

    public Workspace getDestinationWorkspace() {
        return destinationWorkspace;
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
        return user.equals(that.user) && batch.equals(that.batch) && sourceWorkspace.equals(that.sourceWorkspace) && source.equals(that.source) && destinationWorkspace.equals(that.destinationWorkspace) && destinationPath.equals(that.destinationPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, batch, sourceWorkspace, source, destinationWorkspace, destinationPath);
    }

    @Override
    public String toString() {
        return "TransferableItem{" +
                "source=" + source +
                ", destinationPath='" + destinationPath + '\'' +
                '}';
    }
}
