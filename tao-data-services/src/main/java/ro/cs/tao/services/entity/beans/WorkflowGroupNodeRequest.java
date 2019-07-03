package ro.cs.tao.services.entity.beans;

public class WorkflowGroupNodeRequest {
    private long workflowId;
    private Long[] groupNodeIds;
    private String groupNodeName;

    public long getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(long workflowId) {
        this.workflowId = workflowId;
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
