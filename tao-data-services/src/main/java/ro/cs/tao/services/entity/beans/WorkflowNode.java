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
}
