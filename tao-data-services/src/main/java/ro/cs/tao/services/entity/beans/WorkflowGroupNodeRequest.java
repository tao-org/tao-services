package ro.cs.tao.services.entity.beans;

/**
 * Represents a request for managing workflow group nodes, including the workflow identifier,
 * group node identifiers, and group node name.
 */
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
