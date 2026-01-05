package ro.cs.tao.services.entity.beans;

import ro.cs.tao.component.ComponentLink;
import ro.cs.tao.workflow.ParameterValue;
import ro.cs.tao.workflow.WorkflowNodeDescriptor;
import ro.cs.tao.workflow.enums.ComponentType;
import ro.cs.tao.workflow.enums.TransitionBehavior;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class WorkflowNode {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime created;
    private List<ParameterValue> customValues;
    private String componentId;
    private ComponentType componentType;
    private float xCoord;
    private float yCoord;
    private int level;
    private Set<ComponentLink> incomingLinks;
    private boolean preserveOutput;
    private List<ParameterValue> additionalInfo;
    private TransitionBehavior behavior = TransitionBehavior.FAIL_ON_ERROR;
    private Long createdFromNodeId;

    public WorkflowNode(WorkflowNodeDescriptor nodeDescriptor) {
        this.id = nodeDescriptor.getId();
        this.name = nodeDescriptor.getName();
        this.description = nodeDescriptor.getDescription();
        this.created = nodeDescriptor.getCreated();
        this.customValues = nodeDescriptor.getCustomValues();
        this.componentId = nodeDescriptor.getComponentId();
        this.componentType = nodeDescriptor.getComponentType();
        this.additionalInfo = nodeDescriptor.getAdditionalInfo();
        this.level = nodeDescriptor.getLevel();
        this.incomingLinks = nodeDescriptor.getIncomingLinks();
        this.preserveOutput = nodeDescriptor.getPreserveOutput();
        this.createdFromNodeId = nodeDescriptor.getCreatedFromNodeId();
        this.behavior = nodeDescriptor.getBehavior();
        this.xCoord = nodeDescriptor.getxCoord();
        this.yCoord = nodeDescriptor.getyCoord();
    }

    public WorkflowNode() {
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

    public String getComponentId() {
        return componentId;
    }

    public ComponentType getComponentType() {
        return componentType;
    }

    public float getxCoord() {
        return xCoord;
    }

    public float getyCoord() {
        return yCoord;
    }

    public int getLevel() {
        return level;
    }

    public Set<ComponentLink> getIncomingLinks() {
        return incomingLinks;
    }

    public boolean isPreserveOutput() {
        return preserveOutput;
    }

    public List<ParameterValue> getAdditionalInfo() {
        return additionalInfo;
    }

    public TransitionBehavior getBehavior() {
        return behavior;
    }

    public Long getCreatedFromNodeId() {
        return createdFromNodeId;
    }

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

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public void setComponentType(ComponentType componentType) {
        this.componentType = componentType;
    }

    public void setxCoord(float xCoord) {
        this.xCoord = xCoord;
    }

    public void setyCoord(float yCoord) {
        this.yCoord = yCoord;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setIncomingLinks(Set<ComponentLink> incomingLinks) {
        this.incomingLinks = incomingLinks;
    }

    public void setPreserveOutput(boolean preserveOutput) {
        this.preserveOutput = preserveOutput;
    }

    public void setAdditionalInfo(List<ParameterValue> additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public void setBehavior(TransitionBehavior behavior) {
        this.behavior = behavior;
    }

    public void setCreatedFromNodeId(Long createdFromNodeId) {
        this.createdFromNodeId = createdFromNodeId;
    }

    public WorkflowNodeDescriptor toEntity() {
        final WorkflowNodeDescriptor descriptor = new WorkflowNodeDescriptor();
        descriptor.setId(id);
        descriptor.setName(name);
        descriptor.setDescription(description);
        descriptor.setCreated(created);
        descriptor.setCustomValues(customValues);
        descriptor.setComponentId(componentId);
        descriptor.setComponentType(componentType);
        descriptor.setxCoord(xCoord);
        descriptor.setyCoord(yCoord);
        descriptor.setLevel(level);
        descriptor.setIncomingLinks(incomingLinks);
        descriptor.setPreserveOutput(preserveOutput);
        descriptor.setAdditionalInfo(additionalInfo);
        descriptor.setBehavior(behavior);
        descriptor.setCreatedFromNodeId(createdFromNodeId);
        return descriptor;
    }
}
