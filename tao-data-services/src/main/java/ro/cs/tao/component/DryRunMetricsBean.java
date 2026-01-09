package ro.cs.tao.component;

import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.List;

/**
 * A bean class that provides access to the dry run metrics of a processing component.
 * This class wraps an instance of {@code DryRunMetrics} and exposes its metrics
 * in a structured format for external usage.
 */
public class DryRunMetricsBean {
    private final DryRunMetrics metrics;

    public DryRunMetricsBean(DryRunMetrics metrics) {
        this.metrics = metrics;
    }

    @XmlElementWrapper(name="cpuMetrics")
    public List<String> getCpuMetrics(){
        return this.metrics.getCpuUsageMetrics();
    }

    @XmlElementWrapper(name="memoryMetrics")
    public List<String> getMemoryMetrics(){
        return this.metrics.getMemoryUsageMetrics();
    }

    @XmlElementWrapper(name="duration")
    public int getDuration(){
        return this.metrics.getDuration();
    }

}
