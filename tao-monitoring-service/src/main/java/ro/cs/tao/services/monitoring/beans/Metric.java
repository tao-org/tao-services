package ro.cs.tao.services.monitoring.beans;

import org.springframework.boot.actuate.metrics.MetricsEndpoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Metric {
    private String name;
    private String description;
    private String unit;
    private Map<String, Double> measurements;

    public static Metric toBean(MetricsEndpoint.MetricResponse metricResponse) {
        final Metric metric = new Metric();
        metric.setName(metricResponse.getName());
        metric.setDescription(metricResponse.getDescription());
        metric.setUnit(metricResponse.getBaseUnit());
        final List<MetricsEndpoint.Sample> samples = metricResponse.getMeasurements();
        if (samples != null) {
            metric.setMeasurements(new LinkedHashMap<>());
            for (MetricsEndpoint.Sample sample : samples) {
                metric.getMeasurements().put(sample.getStatistic().name(), sample.getValue());
            }
        }
        return metric;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Map<String, Double> getMeasurements() {
        return measurements;
    }

    public void setMeasurements(Map<String, Double> measurements) {
        this.measurements = measurements;
    }
}
