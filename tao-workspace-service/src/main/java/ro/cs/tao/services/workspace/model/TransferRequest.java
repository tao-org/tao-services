package ro.cs.tao.services.workspace.model;

import ro.cs.tao.services.model.FileObject;
import ro.cs.tao.topology.Workspace;

public class TransferRequest {
    private final String batch;
    private final Workspace sourceWorkspace;
    private final FileObject source;
    private final Workspace destinationWorkspace;
    private final String destinationPath;

    public TransferRequest(String batch, Workspace sourceWorkspace, FileObject source, Workspace destinationWorkspace, String destinationPath) {
        this.batch = batch;
        this.sourceWorkspace = sourceWorkspace;
        this.source = source;
        this.destinationWorkspace = destinationWorkspace;
        this.destinationPath = destinationPath;
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
}
