package ro.cs.tao.component;

import ro.cs.tao.component.enums.ParameterType;
import ro.cs.tao.component.validation.Validator;
import ro.cs.tao.datasource.param.JavaType;

import java.util.List;

public class ParameterDescriptorBean {
    private ParameterDescriptor descriptor;

    public ParameterDescriptorBean() {
    }

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

    public void setName(String value) {
        descriptor().setName(value);
    }

    public void setType(ParameterType type) {
        descriptor().setType(type);
    }

    public void setDataType(String dataType) {
        descriptor().setDataType(JavaType.fromFriendlyName(dataType).value());
    }

    public void setDefaultValue(String defaultValue) {
        descriptor().setDefaultValue(defaultValue);
    }

    public void setDescription(String description) {
        descriptor().setDescription(description);
    }

    public void setLabel(String label) {
        descriptor().setLabel(label);
    }

    public void setUnit(String unit) {
        descriptor().setUnit(unit);
    }

    public void setValueSet(String[] valueSet) {
        descriptor().setValueSet(valueSet);
    }

    public void setFormat(String format) {
        descriptor().setFormat(format);
    }

    public void setNotNull(boolean notNull) {
        descriptor().setNotNull(notNull);
    }

    public void setValidator(Validator customValidator) {
        descriptor().setValidator(customValidator);
    }

    public void setDependencies(List<ParameterDependency> dependencies) {
        descriptor().setDependencies(dependencies);
    }

    public void setExpansionRule(ParameterExpansionRule expansionRule) {
        descriptor().setExpansionRule(expansionRule);
    }

    public void setId(String id) {
        descriptor().setId(id);
    }

    public ParameterDescriptor toParameterDescriptor() {
        return this.descriptor;
    }

    private ParameterDescriptor descriptor() {
        if (descriptor == null) {
            descriptor = new ParameterDescriptor();
        }
        return descriptor;
    }
}
