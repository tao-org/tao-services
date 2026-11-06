package ro.cs.tao.services.entity.beans;

import java.util.List;

/**
 * Represents a request for managing a product set, including its label and associated product paths.
 */
public class ProductSetRequest {
    private String label;
    private String repositoryId;
    private List<String> productPaths;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public List<String> getProductPaths() {
        return productPaths;
    }

    public void setProductPaths(List<String> productPaths) {
        this.productPaths = productPaths;
    }
}
