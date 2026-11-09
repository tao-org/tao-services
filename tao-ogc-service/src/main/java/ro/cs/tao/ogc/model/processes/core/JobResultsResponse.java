package ro.cs.tao.ogc.model.processes.core;

import ro.cs.tao.ogc.model.common.Link;

import java.util.List;
import java.util.Map;

public class JobResultsResponse {

    private String jobId;
    private StatusInfo status;
    private Map<String, ResultOutput> outputs;
    private List<Link> links;

    public JobResultsResponse() {
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public StatusInfo getStatus() {
        return status;
    }

    public void setStatus(StatusInfo status) {
        this.status = status;
    }

    public Map<String, ResultOutput> getOutputs() {
        return outputs;
    }

    public void setOutputs(Map<String, ResultOutput> outputs) {
        this.outputs = outputs;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }
}
