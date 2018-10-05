package ro.cs.tao.services.orchestration.beans;

import java.util.Map;

public class ExecutionRequest {
    private long workflowId;
    private String name;
    private Map<String, Map<String, String>> parameters;

    public long getWorkflowId() { return workflowId; }
    public void setWorkflowId(long workflowId) { this.workflowId = workflowId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Map<String, Map<String, String>> getParameters() { return parameters; }
    public void setParameters(Map<String, Map<String, String>> parameters) { this.parameters = parameters; }
}
