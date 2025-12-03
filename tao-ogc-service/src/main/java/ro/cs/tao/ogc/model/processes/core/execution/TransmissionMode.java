package ro.cs.tao.ogc.model.processes.core.execution;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import ro.cs.tao.TaoEnum;

import java.util.Arrays;

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
    @JsonValue
    public String value() {
        return this.value;
    }

    @JsonCreator
    public static TransmissionMode fromValue(String value) {
        return Arrays.stream(values())
                .filter(v -> v.value.equalsIgnoreCase(value))
                .findFirst()
                .orElse(null);
    }
}
