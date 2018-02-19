package ro.cs.tao.services.monitoring.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import ro.cs.tao.messaging.Message;
import ro.cs.tao.messaging.Notifiable;
import ro.cs.tao.messaging.Topics;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.services.commons.MessageConverter;
import ro.cs.tao.services.commons.ServiceMessage;
import ro.cs.tao.services.interfaces.MonitoringService;
import ro.cs.tao.services.model.monitoring.Memory;
import ro.cs.tao.services.model.monitoring.MemoryUnit;
import ro.cs.tao.services.model.monitoring.Runtime;
import ro.cs.tao.services.model.monitoring.Snapshot;
import ro.cs.tao.services.model.monitoring.TimeUnit;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

/**
 * @author Cosmin Cara
 */
@Service("monitoringService")
public class MonitoringServiceImpl extends Notifiable implements MonitoringService<ServiceMessage> {

    private static final int MAX_QUEUE_SIZE = 100;
    private final Queue<Message> messageQueue;
    @Autowired
    private PersistenceManager persistenceManager;

    public MonitoringServiceImpl() {
        super();
        this.messageQueue = new LinkedList<>();
        subscribe(Topics.INFORMATION, Topics.WARNING, Topics.ERROR, Topics.PROGRESS);
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
    public Snapshot getNodeSnapshot(String hostName) {
        Snapshot snapshot = new Snapshot();
        try {
            SnmpClient client = new SnmpClient(hostName);
            client.start();
            logger.info(client.getAsString(SnmpClient.ProcessorCount));
            logger.info(client.getAsString(SnmpClient.SystemDescription));
            logger.info(client.getAsString(SnmpClient.LastMinuteCPULoad));
            logger.info(client.getAsString(SnmpClient.PercentageUserCPUTime));
            logger.info(client.getAsString(SnmpClient.SystemUpTime));
            logger.info(client.getAsString(SnmpClient.TotalMemoryUsed));
            logger.info(client.getAsString(SnmpClient.TotalMemoryFree));
        } catch (Throwable ex) {
            logger.warning(ex.getMessage());
        }
        snapshot.setMemory(new Memory(MemoryUnit.MEGABYTE));
        snapshot.setRuntime(new Runtime(TimeUnit.SECONDS));
        return snapshot;
    }

    @Override
    public List<ServiceMessage> getLiveNotifications() {
        List<ServiceMessage> messages = new ArrayList<>();
        synchronized (this.messageQueue) {
            messages.addAll(this.messageQueue.stream()
                                    .map(m -> new MessageConverter().to(m))
                                    .collect(Collectors.toList()));
            this.messageQueue.clear();
        }
        return messages;
    }

    @Override
    public List<ServiceMessage> getNotifications(String user, int page) {
        final Page<Message> userMessages = persistenceManager.getUserMessages(user, page);
        return userMessages != null ?
                userMessages.getContent().stream()
                        .map(m -> new MessageConverter().to(m))
                        .collect(Collectors.toList()) : new ArrayList<>();
    }

    @Override
    public List<ServiceMessage> acknowledgeNotification(List<ServiceMessage> notifications) {
        if (notifications != null) {
            MessageConverter converter = new MessageConverter();
            notifications.forEach(message -> {
                message.setRead(true);
                try {
                    persistenceManager.saveMessage(converter.from(message));
                } catch (PersistenceException e) {
                    logger.severe(e.getMessage());
                }
            });
        }
        return notifications;
    }

    @Override
    protected void onMessageReceived(Message message) {
        if (this.messageQueue.size() == MAX_QUEUE_SIZE) {
            this.messageQueue.poll();
        }
        this.messageQueue.offer(message);
    }
}
