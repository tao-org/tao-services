package ro.cs.tao.component;

import ro.cs.tao.component.enums.ComponentCategory;
import ro.cs.tao.component.enums.ProcessingComponentVisibility;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a bean class used to encapsulate the data for a condition component.
 * This serves as a transfer object for transferring condition component details
 * between layers or systems.
 */
public class ConditionComponentBean {
    private String id;
    private String label;
    private String version;
    private String description;
    private String authors;
    private String copyright;
    private List<SourceDescriptor> sources;
    private List<TargetDescriptor> targets;
    private List<String> tags;
    private List<ParameterDescriptorBean> parameterDescriptors;
    private ProcessingComponentVisibility visibility;
    private ComponentCategory category;
    private boolean active;
    private String owner;

    public ConditionComponentBean() { }

    public ConditionComponentBean(ConditionComponent src) {
        this.id = src.getId();
        this.label = src.getLabel();
        this.version = src.getVersion();
        this.description = src.getDescription();
        this.authors = src.getAuthors();
        this.copyright = src.getCopyright();
        this.sources = src.getSources();
        this.targets = src.getTargets();
        this.tags = src.getTags();
        this.parameterDescriptors = src.getParameterDescriptors().stream().map(ParameterDescriptorBean::new).collect(Collectors.toList());
        this.visibility = src.getVisibility();
        this.active = src.isActive();
        this.owner = src.getOwner();
        this.category = src.getCategory();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public String getCopyright() {
        return copyright;
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    public List<SourceDescriptor> getSources() {
        return sources;
    }

    public void setSources(List<SourceDescriptor> sources) {
        this.sources = sources;
    }

    public List<TargetDescriptor> getTargets() {
        return targets;
    }

    public void setTargets(List<TargetDescriptor> targets) {
        this.targets = targets;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<ParameterDescriptorBean> getParameterDescriptors() {
        return parameterDescriptors;
    }

    public void setParameterDescriptors(List<ParameterDescriptorBean> parameterDescriptors) {
        this.parameterDescriptors = parameterDescriptors;
    }

    public ProcessingComponentVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(ProcessingComponentVisibility visibility) {
        this.visibility = visibility;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public ComponentCategory getCategory() {
        return category;
    }

    public void setCategory(ComponentCategory category) {
        this.category = category;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public ConditionComponent toComponent() {
        final ConditionComponent entity = new ConditionComponent();
        entity.setId(this.id);
        entity.setLabel(this.label);
        entity.setVersion(this.version);
        entity.setDescription(this.description);
        entity.setAuthors(this.authors);
        entity.setCopyright(this.copyright);
        entity.setSources(this.sources);
        entity.setTargets(this.targets);
        entity.setTags(this.tags);
        if (this.parameterDescriptors != null) {
            entity.setParameterDescriptors(this.parameterDescriptors.stream().map(ParameterDescriptorBean::toParameterDescriptor).collect(Collectors.toSet()));
        }
        entity.setVisibility(this.visibility);
        entity.setActive(this.active);
        entity.setOwner(this.owner);
        return entity;
    }
}
