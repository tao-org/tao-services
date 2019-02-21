package ro.cs.tao.services.entity.beans;

import ro.cs.tao.datasource.beans.Query;

import java.util.List;

public class GroupQuery {
    private Query query;
    private List<String> productNames;
    private String componentId;

    public Query getQuery() { return query; }
    public void setQuery(Query query) { this.query = query; }

    public List<String> getProductNames() { return productNames; }
    public void setProductNames(List<String> productNames) { this.productNames = productNames; }

    public String getComponentId() { return componentId; }
    public void setComponentId(String componentId) { this.componentId = componentId; }
}
