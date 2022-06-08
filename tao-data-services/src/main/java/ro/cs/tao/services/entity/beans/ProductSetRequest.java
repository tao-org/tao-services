package ro.cs.tao.services.entity.beans;

import java.util.List;

public class ProductSetRequest {
    private String label;
    private List<String> productPaths;

    public String getLabel() {
        return label;
    }
    public void setLabel(String label) {
        this.label = label;
    }

    public List<String> getProductPaths() {
        return productPaths;
    }

    public void setProductPaths(List<String> productPaths) {
        this.productPaths = productPaths;
    }
}
