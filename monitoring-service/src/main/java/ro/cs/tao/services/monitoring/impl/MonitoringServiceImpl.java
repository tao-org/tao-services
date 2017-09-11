package ro.cs.tao.services.monitoring.impl;

import org.springframework.stereotype.Service;
import ro.cs.tao.services.monitoring.interfaces.MonitoringService;
import ro.cs.tao.services.monitoring.model.Memory;
import ro.cs.tao.services.monitoring.model.MemoryUnit;
import ro.cs.tao.services.monitoring.model.Runtime;
import ro.cs.tao.services.monitoring.model.Snapshot;
import ro.cs.tao.services.monitoring.model.TimeUnit;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;

/**
 * @author Cosmin Cara
 */
@Service("monitoringService")
public class MonitoringServiceImpl implements MonitoringService {
    @Override
    public Snapshot getMasterSnapshot() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        Runtime runtimeInfo = new Runtime(TimeUnit.SECONDS);
        runtimeInfo.setStartTime(runtimeMXBean.getStartTime());
        runtimeInfo.setUpTime(runtimeMXBean.getUptime());

        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        runtimeInfo.setLastMinuteSystemLoad(operatingSystemMXBean.getSystemLoadAverage());
        runtimeInfo.setAvailableProcessors(operatingSystemMXBean.getAvailableProcessors());

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        runtimeInfo.setThreadCount(threadMXBean.getThreadCount());
        runtimeInfo.setDaemonThreadCount(threadMXBean.getDaemonThreadCount());
        runtimeInfo.setPeakThreadCount(threadMXBean.getPeakThreadCount());
        runtimeInfo.setCurrentThreadUserTime(threadMXBean.getCurrentThreadUserTime());
        runtimeInfo.setCurrentThreadCpuTime(threadMXBean.getCurrentThreadCpuTime());

        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        Memory memoryInfo = new Memory(MemoryUnit.MEGABYTE);
        final MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        final MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();
        memoryInfo.setHeapCommitted(heapMemoryUsage.getCommitted());
        memoryInfo.setHeapInitial(heapMemoryUsage.getInit());
        memoryInfo.setHeapMax(heapMemoryUsage.getMax());
        memoryInfo.setHeapUsed(heapMemoryUsage.getUsed());
        memoryInfo.setNonHeapCommitted(nonHeapMemoryUsage.getCommitted());
        memoryInfo.setNonHeapInitial(nonHeapMemoryUsage.getInit());
        memoryInfo.setNonHeapMax(nonHeapMemoryUsage.getMax());
        memoryInfo.setNonHeapUsed(nonHeapMemoryUsage.getUsed());

        Snapshot snapshot = new Snapshot();
        snapshot.setMemory(memoryInfo);
        snapshot.setRuntime(runtimeInfo);
        return snapshot;
    }
}
