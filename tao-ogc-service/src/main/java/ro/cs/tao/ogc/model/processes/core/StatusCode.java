package ro.cs.tao.ogc.model.processes.core;

import ro.cs.tao.TaoEnum;

public enum StatusCode implements TaoEnum<String> {
    ACCEPTED("accepted"),
    RUNNING("running"),
    SUCCESSFUL("successful"),
    FAILED("failed"),
    DISMISSED("dismissed");

    private final String value;

    StatusCode(String value) {
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
