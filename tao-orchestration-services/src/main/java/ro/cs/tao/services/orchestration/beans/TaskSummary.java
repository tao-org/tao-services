package ro.cs.tao.services.orchestration.beans;

import ro.cs.tao.execution.model.ExecutionStatus;
import ro.cs.tao.execution.model.ExecutionTaskSummary;

import java.time.LocalDateTime;

public class TaskSummary {
    private long taskId;
    private String workflowName;
    private String componentName;
    private LocalDateTime taskStart;
    private LocalDateTime taskEnd;
    private LocalDateTime lastUpdated;
    private double percentComplete;
    private String host;
    private ExecutionStatus taskStatus;
    private String userId;
    private Integer usedCPU;
    private Integer usedRAM;
    private String jobName;
    private String componentType;

    public static TaskSummary toTaskSummary(ExecutionTaskSummary summary) {
        return new TaskSummary(summary.getTaskId(), summary.getWorkflowName(), summary.getComponentName(), summary.getTaskStart(),
                               summary.getTaskEnd(), summary.getLastUpdated(), summary.getPercentComplete(), summary.getHost(),
                               summary.getTaskStatus(), summary.getUserId(), summary.getUsedCPU(), summary.getUsedRAM(),
                               summary.getJobName(), summary.getComponentType());
    }

    public TaskSummary() {
    }

    TaskSummary(long taskId, String workflowName, String componentName, LocalDateTime taskStart, LocalDateTime taskEnd, LocalDateTime lastUpdated, double percentComplete, String host, ExecutionStatus taskStatus, String userId, Integer usedCPU, Integer usedRAM, String jobName, String componentType) {
        this.taskId = taskId;
        this.workflowName = workflowName;
        this.componentName = componentName;
        this.taskStart = taskStart;
        this.taskEnd = taskEnd;
        this.lastUpdated = lastUpdated;
        this.percentComplete = percentComplete;
        this.host = host;
        this.taskStatus = taskStatus;
        this.userId = userId;
        this.usedCPU = usedCPU;
        this.usedRAM = usedRAM;
        this.jobName = jobName;
        this.componentType = componentType;
    }

    public long getTaskId() {
        return taskId;
    }
    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public String getWorkflowName() { return workflowName; }
    public void setWorkflowName(String workflowName) { this.workflowName = workflowName; }

    public String getComponentName() {
        return componentName;
    }
    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public LocalDateTime getTaskStart() {
        return taskStart;
    }
    public void setTaskStart(LocalDateTime taskStart) {
        this.taskStart = taskStart;
    }

    public LocalDateTime getTaskEnd() {
        return taskEnd;
    }
    public void setTaskEnd(LocalDateTime taskEnd) {
        this.taskEnd = taskEnd;
    }

    public String getHost() {
        return host;
    }
    public void setHost(String host) {
        this.host = host;
    }

    public ExecutionStatus getTaskStatus() {
        return taskStatus;
    }
    public void setTaskStatus(ExecutionStatus taskStatus) {
        this.taskStatus = taskStatus;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public double getPercentComplete() {
        return percentComplete;
    }
    public void setPercentComplete(double percentComplete) {
        this.percentComplete = percentComplete;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Integer getUsedCPU() {
        return usedCPU;
    }

    public void setUsedCPU(Integer usedCPU) {
        this.usedCPU = usedCPU;
    }

    public Integer getUsedRAM() {
        return usedRAM;
    }

    public void setUsedRAM(Integer usedRAM) {
        this.usedRAM = usedRAM;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getComponentType() {
        return componentType;
    }

    public void setComponentType(String componentType) {
        this.componentType = componentType;
    }
}
