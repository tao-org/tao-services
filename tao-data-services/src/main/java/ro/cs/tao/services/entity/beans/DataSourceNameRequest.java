package ro.cs.tao.services.entity.beans;

import ro.cs.tao.component.Variable;

import java.util.List;

public class DataSourceNameRequest {
    private String label;
    private List<Variable> products;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<Variable> getProducts() {
        return products;
    }

    public void setProducts(List<Variable> products) {
        this.products = products;
    }
}
