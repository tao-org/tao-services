package ro.cs.tao.services.entity.beans;

import ro.cs.tao.datasource.beans.Query;
import ro.cs.tao.eodata.EOProduct;

import java.util.List;

public class GroupQuery {
    private Query query;
    private List<EOProduct> products;
    private String componentId;

    public Query getQuery() { return query; }
    public void setQuery(Query query) { this.query = query; }

    public List<EOProduct> getProducts() { return products; }
    public void setProducts(List<EOProduct> products) { this.products = products; }

    public String getComponentId() { return componentId; }
    public void setComponentId(String componentId) { this.componentId = componentId; }
}
