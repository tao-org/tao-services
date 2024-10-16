package ro.cs.tao.ogc.model.processes.core;

import ro.cs.tao.TaoEnum;

public enum TransmissionMode implements TaoEnum<String> {
    VALUE("value"),
    REFERENCE("reference");

    private final String value;

    TransmissionMode(String value) {
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
}
