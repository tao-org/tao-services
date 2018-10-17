package ro.cs.tao.services.entity.beans;

import ro.cs.tao.eodata.EOProduct;

import java.util.List;

public class CustomDataSourceRequest {
    private List<EOProduct> products;
    private String label;

    public List<EOProduct> getProducts() { return products; }
    public void setProducts(List<EOProduct> products) { this.products = products; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}
