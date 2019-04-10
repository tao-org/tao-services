package ro.cs.tao.services.entity.beans;

import java.util.Map;

public class DataSourceNameRequest {
    private String label;
    private Map<String, String> products;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Map<String, String> getProducts() {
        return products;
    }

    public void setProducts(Map<String, String> products) {
        this.products = products;
    }
}
