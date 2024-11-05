package ro.cs.tao.ogc.model.processes.dru;

import ro.cs.tao.TaoEnum;

public enum ResponseType implements TaoEnum<String> {
    RAW("raw"),
    DOCUMENT("document");

    private final String value;

    ResponseType(String value) {
        this.value = value;
    }

    @Override
    public String friendlyName() {
        return "";
    }

    @Override
    public String value() {
        return "";
    }
}
