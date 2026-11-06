package ro.cs.tao.ogc.model.processes.core;

import java.util.List;
import java.util.Map;

public class ObjectDescriptor extends Descriptor<String> {
    private List<String> required;
    private Map<String, ObjectDescriptorProperties> properties;

    public List<String> getRequired() {
        return required;
    }

    public void setRequired(List<String> required) {
        this.required = required;
    }

    public Map<String, ObjectDescriptorProperties> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, ObjectDescriptorProperties> properties) {
        this.properties = properties;
    }
}
