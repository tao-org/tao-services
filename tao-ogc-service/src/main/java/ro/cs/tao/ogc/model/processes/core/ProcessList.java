package ro.cs.tao.ogc.model.processes.core;

import ro.cs.tao.ogc.model.common.Link;

import java.util.List;

public class ProcessList {
    private List<ProcessSummary> processes;
    private List<Link> links;

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    public List<ProcessSummary> getProcesses() {
        return processes;
    }

    public void setProcesses(List<ProcessSummary> processes) {
        this.processes = processes;
    }
}
