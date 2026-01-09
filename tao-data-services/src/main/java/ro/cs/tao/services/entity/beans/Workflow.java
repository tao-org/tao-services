package ro.cs.tao.services.entity.beans;

import ro.cs.tao.eodata.enums.Visibility;
import ro.cs.tao.workflow.ParameterValue;
import ro.cs.tao.workflow.WorkflowDescriptor;
import ro.cs.tao.workflow.WorkflowNodeDescriptor;
import ro.cs.tao.workflow.enums.Status;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a workflow bean, encapsulating information about a workflow
 * including its identifier, name, description, creation timestamp, custom parameter values,
 * user identifier, visibility, status, path, creation source workflow identifier,
 * active flag, nodes, coordinates, zoom level, temporary flag, tags, and image.
 */
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
    private String image;

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

    public Workflow() {
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

    public String getImage() { return image; }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public void setCustomValues(List<ParameterValue> customValues) {
        this.customValues = customValues;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setCreatedFromWorkflowId(Long createdFromWorkflowId) {
        this.createdFromWorkflowId = createdFromWorkflowId;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setNodes(List<WorkflowNode> nodes) {
        this.nodes = nodes;
    }

    public void setxCoord(float xCoord) {
        this.xCoord = xCoord;
    }

    public void setyCoord(float yCoord) {
        this.yCoord = yCoord;
    }

    public void setZoom(float zoom) {
        this.zoom = zoom;
    }

    public void setTemporary(boolean temporary) {
        this.temporary = temporary;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public void setImage(String image) { this.image = image; }

    public WorkflowDescriptor toEntity() {
        final WorkflowDescriptor descriptor = new WorkflowDescriptor();
        descriptor.setId(id);
        descriptor.setName(name);
        descriptor.setDescription(description);
        descriptor.setCreated(created);
        descriptor.setCustomValues(customValues);
        descriptor.setUserId(userId);
        descriptor.setStatus(status);
        descriptor.setVisibility(visibility);
        descriptor.setActive(active);
        descriptor.setxCoord(xCoord);
        descriptor.setyCoord(yCoord);
        descriptor.setZoom(zoom);
        descriptor.setTemporary(temporary);
        descriptor.setTags(tags);
        descriptor.setPath(path);
        descriptor.setCreatedFromWorkflowId(createdFromWorkflowId);
        if (nodes != null) {
            descriptor.setNodes(nodes.stream().map(WorkflowNode::toEntity).toList());
        }
        return descriptor;
    }
}
