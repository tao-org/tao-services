package ro.cs.tao.datasource.param;

import ro.cs.tao.component.ParameterDependency;

import java.util.List;

/**
 * Wrapper class for {@link DataSourceParameter}, providing an interface for accessing
 * the details of a data source query parameter.
 */
public class DataSourceParameterBean {
    final DataSourceParameter parameter;

    public DataSourceParameterBean(DataSourceParameter parameter) {
        this.parameter = parameter;
    }

    public String getName() {
        return parameter.getName();
    }

    public String getRemoteName() {
        return parameter.getRemoteName();
    }

    public String getLabel() {
        return parameter.getLabel();
    }

    public String getType() {
        return parameter.type != null ? parameter.type.friendlyName() : JavaType.STRING.friendlyName();
    }

    public boolean isRequired() {
        return parameter.isRequired();
    }

    public Object getDefaultValue() {
        return parameter.getDefaultValue();
    }

    public int getOrder() {
        return parameter.getOrder();
    }

    public Object[] getValueSet() {
        return parameter.getValueSet();
    }

    public List<ParameterDependency> getDependencies() {
        return parameter.getDependencies();
    }
}
