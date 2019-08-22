package ro.cs.tao.services.scheduling.beans;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Data structure for schedule creation
 * 
 * @author Lucian Barbulescu
 *
 */
public class SchedulingRequest {
	private String id;
	private String name;
    private int repeatInterval;
    private LocalDateTime startTime;
	private long workflowId;
    private Map<String, Map<String, String>> parameters;
    private Map<String, Map<String, String>> additional;

    public String getId() {	return id; } 
	public void setId(String id) { this.id = id; }

	public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getRepeatInterval() { return repeatInterval;	}
	public void setRepeatInterval(int repeatInterval) { this.repeatInterval = repeatInterval; }
	
	public LocalDateTime getStartTime() { return startTime;	}
	public void setStartTime(LocalDateTime startTime) { this.startTime = startTime;	}
	
	public long getWorkflowId() { return workflowId; }
    public void setWorkflowId(long workflowId) { this.workflowId = workflowId; }

    public Map<String, Map<String, String>> getParameters() { return parameters; }
    public void setParameters(Map<String, Map<String, String>> parameters) { this.parameters = parameters; }

    public Map<String, Map<String, String>> getAdditional() { return additional; }
	public void setAdditional(Map<String, Map<String, String>> additional) { this.additional = additional; }
}
