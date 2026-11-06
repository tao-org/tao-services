package ro.cs.tao.services.monitoring.beans;

import org.springframework.boot.micrometer.metrics.actuate.endpoint.MetricsEndpoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a metric that contains information such as its name, description,
 * unit of measurement, and a set of statistical measurements.
 *
 * This class is primarily used to encapsulate metric-related data and provides
 * methods for accessing and mutating the metric's properties. Additionally,
 * it includes a utility method for converting external metric response objects
 * into instances of the Metric class.
 */
public class Metric {
    private String name;
    private String description;
    private String unit;
    private Map<String, Double> measurements;

    public static Metric toBean(MetricsEndpoint.MetricDescriptor metricResponse) {
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
