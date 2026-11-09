package ro.cs.tao.services.entity.beans;

import ro.cs.tao.datasource.beans.Query;
import ro.cs.tao.eodata.EOProduct;

import java.util.List;

/**
 * Represents a query associated with a data source group, including the query itself and a list of products.
 */
public class GroupQuery {
    private Query query;
    private List<EOProduct> products;

    public Query getQuery() { return query; }
    public void setQuery(Query query) { this.query = query; }

    public List<EOProduct> getProducts() { return products; }
    public void setProducts(List<EOProduct> products) { this.products = products; }

}
