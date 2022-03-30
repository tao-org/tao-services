package ro.cs.tao.services.orchestration.beans;

import java.util.Map;

public class WPSExecutionRequest {
    private String wpsId;
    private Map<String, String> parameters;

    public String getWpsId() {
        return wpsId;
    }

    public void setWpsId(String wpsId) {
        this.wpsId = wpsId;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }
}
