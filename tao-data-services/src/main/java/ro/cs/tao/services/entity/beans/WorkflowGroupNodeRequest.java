package ro.cs.tao.services.entity.beans;

public class WorkflowGroupNodeRequest {
    private long workflowId;
    private long parentNodeId;
    private Long[] groupNodeIds;
    private String groupNodeName;

    public long getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(long workflowId) {
        this.workflowId = workflowId;
    }

    public long getParentNodeId() {
        return parentNodeId;
    }

    public void setParentNodeId(long parentNodeId) {
        this.parentNodeId = parentNodeId;
    }

    public Long[] getGroupNodeIds() {
        return groupNodeIds;
    }

    public void setGroupNodeIds(Long[] groupNodeIds) {
        this.groupNodeIds = groupNodeIds;
    }

    public String getGroupNodeName() { return groupNodeName; }

    public void setGroupNodeName(String name) {
        this.groupNodeName = name;
    }
}
