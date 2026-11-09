package ro.cs.tao.services.orchestration.beans;

import java.util.Map;

/**
 * Represents a request for the execution of a web service.
 * This class holds the details necessary to identify the web service
 * and the parameters required for its execution.
 *
 * The request is composed of:
 * - A unique identifier for the web service.
 * - A set of key-value pairs representing the parameters required by the web service.
 *
 * This class provides methods to retrieve and modify the web service ID
 * and its parameters.
 */
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
