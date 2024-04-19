package ro.cs.tao.services.orchestration.beans;

import ro.cs.tao.execution.model.ExecutionTaskSummary;

public class TaskOutput {
    private long taskId;
    private String output;
    private String command;

    public static TaskOutput toTaskOutput(ExecutionTaskSummary summary) {
        return new TaskOutput(summary.getTaskId(), summary.getOutput(), summary.getCommand());
    }

    public TaskOutput() {
    }

    TaskOutput(long taskId, String output, String command) {
        this.taskId = taskId;
        this.output = output;
        this.command = command;
    }

    public long getTaskId() {
        return taskId;
    }
    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public String getOutput() {
        return output;
    }
    public void setOutput(String output) {
        this.output = output;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }
}
