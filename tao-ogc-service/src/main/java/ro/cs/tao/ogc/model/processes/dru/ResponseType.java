package ro.cs.tao.ogc.model.processes.dru;

import com.fasterxml.jackson.annotation.JsonValue;
import ro.cs.tao.TaoEnum;

public enum ResponseType implements TaoEnum<String> {
    RAW("raw"),
    DOCUMENT("document");

    private final String value;

    ResponseType(String value) {
        this.value = value;
    }


    @JsonValue
    @Override
    public String friendlyName() {
        return this.value;
    }

    @Override
    public String value() {
        return this.value;
    }
}
