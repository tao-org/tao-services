package ro.cs.tao.ogc.model.processes.core;

import ro.cs.tao.TaoEnum;

public enum JobControlOptions implements TaoEnum<String> {
    SYNC_EXECUTE("sync-execute"),
    ASYNC_EXECUTE("async-execute"),
    DISMISS("dismiss");

    private final String value;

    JobControlOptions(String value) {
        this.value = value;
    }

    @Override
    public String friendlyName() {
        return this.value;
    }

    @Override
    public String value() {
        return this.value;
    }


    @Override
    public String toString() {
        return this.value;
    }
}
