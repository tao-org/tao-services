package ro.cs.tao.services.orchestration.beans;

import java.util.Map;

public class WebServiceExecutionRequest {
    private String webServiceId;
    private Map<String, String> parameters;

    public String getWebServiceId() {
        return webServiceId;
    }

    public void setWebServiceId(String webServiceId) {
        this.webServiceId = webServiceId;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }
}
