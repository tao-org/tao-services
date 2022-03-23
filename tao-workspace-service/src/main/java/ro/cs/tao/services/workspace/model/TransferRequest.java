package ro.cs.tao.services.workspace.model;

import ro.cs.tao.services.model.FileObject;

import java.util.List;

public class TransferRequest {
    private int sourceWorkspace;
    private int destinationWorkspace;
    private String destinationPath;
    private Boolean move;
    private Boolean overwrite;
    private List<FileObject> fileObjects;

    public int getSourceWorkspace() {
        return sourceWorkspace;
    }

    public void setSourceWorkspace(int sourceWorkspace) {
        this.sourceWorkspace = sourceWorkspace;
    }

    public int getDestinationWorkspace() {
        return destinationWorkspace;
    }

    public void setDestinationWorkspace(int destinationWorkspace) {
        this.destinationWorkspace = destinationWorkspace;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public void setDestinationPath(String destinationPath) {
        this.destinationPath = destinationPath;
    }

    public Boolean getMove() {
        return move;
    }

    public void setMove(Boolean move) {
        this.move = move;
    }

    public Boolean getOverwrite() {
        return overwrite;
    }

    public void setOverwrite(Boolean overwrite) {
        this.overwrite = overwrite;
    }

    public List<FileObject> getFileObjects() {
        return fileObjects;
    }

    public void setFileObjects(List<FileObject> objects) {
        this.fileObjects = objects;
    }
}
