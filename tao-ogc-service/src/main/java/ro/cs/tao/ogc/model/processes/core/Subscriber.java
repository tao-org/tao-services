package ro.cs.tao.ogc.model.processes.core;

public class Subscriber {
    private String successUri;
    private String inProgressUri;
    private String failedUri;

    public String getFailedUri() {
        return failedUri;
    }

    public void setFailedUri(String failedUri) {
        this.failedUri = failedUri;
    }

    public String getInProgressUri() {
        return inProgressUri;
    }

    public void setInProgressUri(String inProgressUri) {
        this.inProgressUri = inProgressUri;
    }

    public String getSuccessUri() {
        return successUri;
    }

    public void setSuccessUri(String successUri) {
        this.successUri = successUri;
    }
}
