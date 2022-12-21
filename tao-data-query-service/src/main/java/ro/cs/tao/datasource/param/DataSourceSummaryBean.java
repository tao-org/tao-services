package ro.cs.tao.datasource.param;

import ro.cs.tao.services.model.datasource.DataSourceDescriptor;

public class DataSourceSummaryBean {
    private final DataSourceDescriptor descriptor;

    public DataSourceSummaryBean(DataSourceDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public String getSensor() {
        return descriptor.getSensor();
    }

    public String getDataSourceName() {
        return descriptor.getDataSourceName();
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

    public String getMission() { return descriptor.getMission(); }

    public boolean isRequiresAuthentication() { return descriptor.isRequiresAuthentication(); }

    public String getDataSourceLabel() {
        final String sensor = descriptor.getSensor();
        return sensor.indexOf('(') > 0 ?
                descriptor.getDataSourceName() + " " + sensor.substring(sensor.indexOf('(')) :
                sensor.substring(sensor.lastIndexOf('-') + 1).length() > 2 ?
                        descriptor.getDataSourceName() + " " + sensor.substring(sensor.lastIndexOf('-') + 1) :
                        descriptor.getDataSourceName();
    }
}
