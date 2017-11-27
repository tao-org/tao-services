package ro.cs.tao.services.monitoring.impl;

import org.springframework.stereotype.Service;
import reactor.bus.Event;
import reactor.fn.Consumer;
import ro.cs.tao.messaging.Message;
import ro.cs.tao.messaging.MessageBus;
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
import java.util.LinkedList;
import java.util.Queue;

/**
 * @author Cosmin Cara
 */
@Service("monitoringService")
public class MonitoringServiceImpl
        implements MonitoringService, Consumer<Event<Message>> {

    private static final int MAX_QUEUE_SIZE = 100;
    private final Queue<Message> messageQueue;

    public MonitoringServiceImpl() {
        this.messageQueue = new LinkedList<>();
        MessageBus.register(this, MessageBus.INFORMATION, MessageBus.WARNING, MessageBus.ERROR, MessageBus.PROGRESS);
    }

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

    @Override
    public Message[] getNotifications() {
        Message[] messages;
        synchronized (this.messageQueue) {
            messages = this.messageQueue.toArray(new Message[this.messageQueue.size()]);
            this.messageQueue.clear();
        }
        return messages;
    }

    @Override
    public void accept(Event<Message> message) {
        synchronized (this.messageQueue) {
            if (this.messageQueue.size() == MAX_QUEUE_SIZE) {
                this.messageQueue.poll();
            }
            this.messageQueue.offer(message.getData());
        }
    }
}
