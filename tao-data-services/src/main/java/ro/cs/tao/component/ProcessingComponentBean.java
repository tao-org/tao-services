package ro.cs.tao.component;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import ro.cs.tao.component.enums.ComponentCategory;
import ro.cs.tao.component.enums.ProcessingComponentType;
import ro.cs.tao.component.enums.ProcessingComponentVisibility;
import ro.cs.tao.component.template.Template;
import ro.cs.tao.component.template.TemplateType;
import ro.cs.tao.component.template.engine.TemplateEngine;

import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ProcessingComponentBean {
    private final ProcessingComponent component;

    public ProcessingComponentBean(ProcessingComponent component) {
        this.component = component;
    }

    public String getFileLocation() {
        return component.getFileLocation();
    }

    @JsonIgnore
    public String getExpandedFileLocation() {
        return component.getExpandedFileLocation();
    }

    public String getWorkingDirectory() {
        return component.getWorkingDirectory();
    }

    @JsonIgnore
    public Template getTemplate() {
        return component.getTemplate();
    }

    @XmlElementWrapper(name = "variables")
    public Set<Variable> getVariables() {
        return component.getVariables();
    }

    @XmlElementWrapper(name = "parameters")
    public List<ParameterDescriptorBean> getParameterDescriptors() {
        return component.getParameterDescriptors().stream().map(ParameterDescriptorBean::new).collect(Collectors.toList());
    }

    public boolean getMultiThread() {
        return component.getMultiThread();
    }

    public ProcessingComponentVisibility getVisibility() {
        return component.getVisibility();
    }

    public boolean getActive() {
        return component.getActive();
    }

    @XmlTransient
    public TemplateType getTemplateType() {
        return component.getTemplateType();
    }

    @JsonGetter("templatecontents")
    public String getTemplateContents() {
        return component.getTemplateContents();
    }

    @JsonIgnore
    public TemplateEngine getTemplateEngine() {
        return component.getTemplateEngine();
    }

    public Integer getParallelism() {
        return component.getParallelism();
    }

    public String getContainerId() {
        return component.getContainerId();
    }

    public ProcessingComponentType getComponentType() {
        return component.getComponentType();
    }

    public String getOwner() {
        return component.getOwner();
    }

    public boolean isTransient() {
        return component.isTransient();
    }

    public String getId() {
        return component.getId();
    }

    public String getLabel() {
        return component.getLabel();
    }

    public String getVersion() {
        return component.getVersion();
    }

    public String getDescription() {
        return component.getDescription();
    }

    public String getAuthors() {
        return component.getAuthors();
    }

    public String getCopyright() {
        return component.getCopyright();
    }

    public String getNodeAffinity() {
        return component.getNodeAffinity();
    }

    public ComponentCategory getCategory() { return component.getCategory(); }

    @XmlElementWrapper(name = "inputs")
    public List<SourceDescriptor> getSources() {
        return component.getSources();
    }

    @XmlElementWrapper(name = "outputs")
    public List<TargetDescriptor> getTargets() {
        return component.getTargets();
    }

    @XmlElementWrapper(name = "tags")
    public List<String> getTags() {
        return component.getTags();
    }

    public boolean hasDescriptor(String id) {
        return component.hasDescriptor(id);
    }

    public boolean getManagedOutput() { return component.isOutputManaged(); }
}
