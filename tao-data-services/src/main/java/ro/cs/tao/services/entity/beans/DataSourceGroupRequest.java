package ro.cs.tao.services.entity.beans;

import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.datasource.beans.Query;

public class DataSourceGroupRequest {
    private String groupId;
    private DataSourceComponent component;
    private Query query;

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public DataSourceComponent getComponent() { return component; }
    public void setComponent(DataSourceComponent component) { this.component = component; }

    public Query getQuery() { return query; }
    public void setQuery(Query query) { this.query = query; }
}
