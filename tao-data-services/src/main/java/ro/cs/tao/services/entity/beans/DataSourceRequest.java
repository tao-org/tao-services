package ro.cs.tao.services.entity.beans;

import ro.cs.tao.eodata.EOProduct;

import java.util.List;

public class DataSourceRequest {
    private List<EOProduct> products;
    private String productType;
    private String dataSource;
    private String label;
    private Long queryId;

    public List<EOProduct> getProducts() { return products; }
    public void setProducts(List<EOProduct> products) { this.products = products; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public Long getQueryId() { return queryId; }
    public void setQueryId(Long queryId) { this.queryId = queryId; }
}
