package ro.cs.tao.services.orchestration.beans;

import ro.cs.tao.execution.model.ExecutionTaskSummary;

/**
 * The TaskOutput class represents the output of a specific task.
 * It encapsulates details about the task such as its unique identifier (taskId),
 * the corresponding output generated during task execution, and the command
 * associated with the task.
 *
 * This class provides constructors for initialization, getters and setters for
 * accessing and modifying task attributes, and a static factory method to create
 * a TaskOutput instance from an ExecutionTaskSummary instance.
 */
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
