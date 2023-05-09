package ro.cs.tao.services.workspace.model;

import ro.cs.tao.services.model.FileObject;

import java.util.List;

public class TransferRequest {
    private String sourceWorkspace;
    private String destinationWorkspace;
    private String destinationPath;
    private Boolean move;
    private Boolean overwrite;
    private List<FileObject> fileObjects;
    private String filter;

    public String getSourceWorkspace() {
        return sourceWorkspace;
    }

    public void setSourceWorkspace(String sourceWorkspace) {
        this.sourceWorkspace = sourceWorkspace;
    }

    public String getDestinationWorkspace() {
        return destinationWorkspace;
    }

    public void setDestinationWorkspace(String destinationWorkspace) {
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

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }
}
