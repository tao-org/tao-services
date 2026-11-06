package ro.cs.tao.services.entity.beans;

public class RepositoryQueryRequest {
    private String label;
    private String sourceRepositoryId;
    private String sourcePath;
    private String destinationRepositoryId;
    private String destinationPath;
    private Integer itemsCount;
    private String fileNamePattern;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getSourceRepositoryId() {
        return sourceRepositoryId;
    }

    public void setSourceRepositoryId(String sourceRepositoryId) {
        this.sourceRepositoryId = sourceRepositoryId;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public String getDestinationRepositoryId() {
        return destinationRepositoryId;
    }

    public void setDestinationRepositoryId(String destinationRepositoryId) {
        this.destinationRepositoryId = destinationRepositoryId;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public void setDestinationPath(String destinationPath) {
        this.destinationPath = destinationPath;
    }

    public Integer getItemsCount() {
        return itemsCount;
    }

    public void setItemsCount(Integer itemsCount) {
        this.itemsCount = itemsCount;
    }

    public String getFileNamePattern() {
        return fileNamePattern;
    }

    public void setFileNamePattern(String fileNamePattern) {
        this.fileNamePattern = fileNamePattern;
    }
}
