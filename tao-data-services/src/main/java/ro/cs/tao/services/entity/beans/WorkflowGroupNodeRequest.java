package ro.cs.tao.services.entity.beans;

import ro.cs.tao.workflow.WorkflowNodeGroupDescriptor;

public class WorkflowGroupNodeRequest {
    private long workflowId;
    private long parentNodeId;
    private Long[] groupNodeIds;
    private WorkflowNodeGroupDescriptor groupNode;

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

    public WorkflowNodeGroupDescriptor getGroupNode() {
        return groupNode;
    }

    public void setGroupNode(WorkflowNodeGroupDescriptor groupNode) {
        this.groupNode = groupNode;
    }
}
