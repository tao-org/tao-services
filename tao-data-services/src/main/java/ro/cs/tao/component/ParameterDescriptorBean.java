package ro.cs.tao.component;

import ro.cs.tao.component.enums.ParameterType;
import ro.cs.tao.component.validation.Validator;

import java.util.List;

public class ParameterDescriptorBean {
    final ParameterDescriptor descriptor;

    public ParameterDescriptorBean(ParameterDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public String getName() {
        return descriptor.getName();
    }

    public ParameterType getType() {
        return descriptor.getType();
    }

    public String getDataType() {
        return descriptor.dataType.friendlyName();
    }

    public String getDefaultValue() {
        return descriptor.getDefaultValue();
    }

    public String getDescription() {
        return descriptor.getDescription();
    }

    public String getLabel() {
        return descriptor.getLabel();
    }

    public String getUnit() {
        return descriptor.getUnit();
    }

    public String[] getValueSet() {
        return descriptor.getValueSet();
    }

    public String getFormat() {
        return descriptor.getFormat();
    }

    public boolean isNotNull() {
        return descriptor.isNotNull();
    }

    public Validator getValidator() {
        return descriptor.getValidator();
    }

    public List<ParameterDependency> getDependencies() {
        return descriptor.getDependencies();
    }

    public ParameterExpansionRule getExpansionRule() {
        return descriptor.getExpansionRule();
    }

    public String getId() {
        return descriptor.getId();
    }
}
