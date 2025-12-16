package ro.cs.tao.services.entity.beans;

import ro.cs.tao.eodata.enums.Visibility;
import ro.cs.tao.workflow.ParameterValue;
import ro.cs.tao.workflow.WorkflowDescriptor;
import ro.cs.tao.workflow.WorkflowNodeDescriptor;
import ro.cs.tao.workflow.enums.Status;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Workflow {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime created;
    private List<ParameterValue> customValues;
    private String userId;
    private Visibility visibility;
    private Status status;
    private String path;
    private Long createdFromWorkflowId;
    private boolean active;
    private List<WorkflowNode> nodes;
    private float xCoord;
    private float yCoord;
    private float zoom;
    private boolean temporary;
    private List<String> tags;

    public Workflow(WorkflowDescriptor descriptor) {
        this.id = descriptor.getId();
        this.name = descriptor.getName();
        this.description = descriptor.getDescription();
        this.created = descriptor.getCreated();
        this.customValues = descriptor.getCustomValues();
        this.userId = descriptor.getUserId();
        this.visibility = descriptor.getVisibility();
        this.status = descriptor.getStatus();
        this.visibility = descriptor.getVisibility();
        this.path = descriptor.getPath();
        this.createdFromWorkflowId = descriptor.getCreatedFromWorkflowId();
        this.active = descriptor.isActive();
        this.xCoord = descriptor.getxCoord();
        this.yCoord = descriptor.getyCoord();
        this.zoom = descriptor.getZoom();
        this.temporary = descriptor.isTemporary();
        this.tags = descriptor.getTags();
        List<WorkflowNodeDescriptor> initialNodes = descriptor.getNodes();
        if (initialNodes != null) {
            this.nodes = new ArrayList<>();
            for (WorkflowNodeDescriptor initialNode : initialNodes) {
                this.nodes.add(new WorkflowNode(initialNode));
            }
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public List<ParameterValue> getCustomValues() {
        return customValues;
    }

    public String getUserId() {
        return userId;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public Status getStatus() {
        return status;
    }

    public String getPath() {
        return path;
    }

    public Long getCreatedFromWorkflowId() {
        return createdFromWorkflowId;
    }

    public boolean isActive() {
        return active;
    }

    public List<WorkflowNode> getNodes() {
        return nodes;
    }

    public float getxCoord() {
        return xCoord;
    }

    public float getyCoord() {
        return yCoord;
    }

    public float getZoom() {
        return zoom;
    }

    public boolean isTemporary() {
        return temporary;
    }

    public List<String> getTags() {
        return tags;
    }
}
