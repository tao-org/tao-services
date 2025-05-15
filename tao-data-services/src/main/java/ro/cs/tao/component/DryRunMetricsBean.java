package ro.cs.tao.component;

import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.List;

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
