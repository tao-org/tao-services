package ro.cs.tao.ogc.model.processes.core;

import ro.cs.tao.ogc.model.common.Link;

import java.util.ArrayList;
import java.util.List;

public class JobsInfo {
    private List<StatusInfo> jobs = new ArrayList<>();
    private List<Link> links = new ArrayList<>();

    public JobsInfo() {
    }

    public List<StatusInfo> getJobs() {
        return jobs;
    }

    public void setJobs(List<StatusInfo> jobs) {
        this.jobs = jobs;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }
}
