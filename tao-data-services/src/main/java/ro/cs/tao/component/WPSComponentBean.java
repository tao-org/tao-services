package ro.cs.tao.component;

import ro.cs.tao.component.enums.ProcessingComponentVisibility;
import ro.cs.tao.component.ogc.WPSComponent;
import ro.cs.tao.docker.ContainerType;

import java.util.List;
import java.util.stream.Collectors;

public class WPSComponentBean {

    private String id;
    private String label;
    private String version;
    private String description;
    private String authors;
    private String copyright;
    private NodeAffinity nodeAffinity;
    private List<SourceDescriptor> sources;
    private List<TargetDescriptor> targets;
    private List<String> tags;
    private String remoteAddress;
    private String capabilityName;
    private List<ParameterDescriptorBean> parameters;
    private ProcessingComponentVisibility visibility;
    private boolean active;
    private String owner;
    private String serviceId;
    private final ContainerType type;

    public WPSComponentBean() {
        this.type = ContainerType.WPS;
    }

    public WPSComponentBean(WPSComponent src) {
        this.id = src.getId();
        this.label = src.getLabel();
        this.version = src.getVersion();
        this.description = src.getDescription();
        this.authors = src.getAuthors();
        this.copyright = src.getCopyright();
        this.nodeAffinity = src.getNodeAffinity();
        this.sources = src.getSources();
        this.targets = src.getTargets();
        this.tags = src.getTags();
        this.remoteAddress = src.getRemoteAddress();
        this.capabilityName = src.getCapabilityName();
        final List<ParameterDescriptor> parameters = src.getParameters();
        if (parameters != null) {
            this.parameters = parameters.stream().map(ParameterDescriptorBean::new).collect(Collectors.toList());
        }
        this.visibility = src.getVisibility();
        this.active = src.isActive();
        this.owner = src.getOwner();
        if (src.getService() != null) {
            this.serviceId = src.getService().getId();
        }
        this.type = ContainerType.WPS;
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

    public NodeAffinity getNodeAffinity() {
        return nodeAffinity;
    }

    public void setNodeAffinity(NodeAffinity nodeAffinity) {
        this.nodeAffinity = nodeAffinity;
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

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public String getCapabilityName() {
        return capabilityName;
    }

    public void setCapabilityName(String capabilityName) {
        this.capabilityName = capabilityName;
    }

    public List<ParameterDescriptorBean> getParameters() {
        return parameters;
    }

    public void setParameters(List<ParameterDescriptorBean> parameters) {
        this.parameters = parameters;
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

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public ContainerType getType() {
        return type;
    }

    public WPSComponent toComponent() {
        final WPSComponent entity = new WPSComponent();
        entity.setId(this.id);
        entity.setLabel(this.label);
        entity.setVersion(this.version);
        entity.setDescription(this.description);
        entity.setAuthors(this.authors);
        entity.setCopyright(this.copyright);
        entity.setNodeAffinity(this.nodeAffinity);
        entity.setSources(this.sources);
        entity.setTargets(this.targets);
        entity.setTags(this.tags);
        entity.setRemoteAddress(this.remoteAddress);
        entity.setCapabilityName(this.capabilityName);
        if (this.parameters != null) {
            entity.setParameters(this.parameters.stream().map(ParameterDescriptorBean::toParameterDescriptor).collect(Collectors.toList()));
        }
        entity.setVisibility(this.visibility);
        entity.setActive(this.active);
        entity.setOwner(this.owner);
        return entity;
    }
}
