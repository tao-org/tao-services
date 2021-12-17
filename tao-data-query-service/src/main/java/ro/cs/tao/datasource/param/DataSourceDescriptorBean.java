package ro.cs.tao.datasource.param;

import ro.cs.tao.services.model.datasource.DataSourceDescriptor;

import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DataSourceDescriptorBean {
    private final DataSourceDescriptor descriptor;

    public DataSourceDescriptorBean(DataSourceDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public String getMission() { return descriptor.getMission(); }

    public String getSensor() {
        return descriptor.getSensor();
    }

    public String getDataSourceName() {
        return descriptor.getDataSourceName();
    }

    public String getDataSourceLabel() {
        final String sensor = descriptor.getSensor();
        return sensor.indexOf('(') > 0 ?
                descriptor.getDataSourceName() + " " + sensor.substring(sensor.indexOf('(')) :
                sensor.substring(sensor.lastIndexOf('-') + 1).length() > 2 ?
                        descriptor.getDataSourceName() + " " + sensor.substring(sensor.lastIndexOf('-') + 1) :
                        descriptor.getDataSourceName();
    }

    public String getCategory() {
        return descriptor.getCategory();
    }

    public String getDescription() {
        return descriptor.getDescription();
    }

    public String getTemporalCoverage() {
        return descriptor.getTemporalCoverage();
    }

    public String getSpatialCoverage() {
        return descriptor.getSpatialCoverage();
    }

    public String getUser() {
        return descriptor.getUser();
    }

    public String getPwd() {
        return descriptor.getPwd();
    }

    public Map<String, DataSourceParameterBean> getParameters() {
        return descriptor.getParameters().entrySet()
                .stream()
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), new DataSourceParameterBean(e.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
