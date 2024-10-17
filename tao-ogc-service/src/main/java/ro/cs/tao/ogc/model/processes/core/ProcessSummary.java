package ro.cs.tao.ogc.model.processes.core;

import ro.cs.tao.ogc.model.common.Link;

import java.util.ArrayList;
import java.util.List;

public class ProcessSummary extends DescriptionType{
    private String id;
    private String version;
    private List<JobControlOptions> jobControlOptions;
    private List<Link> links;
    private List<TransmissionMode> outputTransmission;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<JobControlOptions> getJobControlOptions() {
        return jobControlOptions;
    }

    public void setJobControlOptions(List<JobControlOptions> jobControlOptions) {
        this.jobControlOptions = jobControlOptions;
    }

    public void addJobControlOption(JobControlOptions option) {
        if (this.jobControlOptions == null) {
            this.jobControlOptions = new ArrayList<>();
        }
        this.jobControlOptions.add(option);
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    public void addLink(Link link) {
        if (this.links == null) {
            this.links = new ArrayList<>();
        }
        this.links.add(link);
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<TransmissionMode> getOutputTransmission() {
        return outputTransmission;
    }

    public void setOutputTransmission(List<TransmissionMode> outputTransmission) {
        this.outputTransmission = outputTransmission;
    }

    public void addOutputTransmission(TransmissionMode mode) {
        if (this.outputTransmission == null) {
            this.outputTransmission = new ArrayList<>();
        }
        this.outputTransmission.add(mode);
    }
}
