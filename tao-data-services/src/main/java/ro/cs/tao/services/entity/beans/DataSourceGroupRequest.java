package ro.cs.tao.services.entity.beans;

public class DataSourceGroupRequest {
    private String groupId;
    private String groupLabel;
    private GroupQuery[] queries;

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getGroupLabel() { return groupLabel; }
    public void setGroupLabel(String groupLabel) { this.groupLabel = groupLabel; }

    public GroupQuery[] getQueries() { return queries; }
    public void setQueries(GroupQuery[] queries) { this.queries = queries; }
}
