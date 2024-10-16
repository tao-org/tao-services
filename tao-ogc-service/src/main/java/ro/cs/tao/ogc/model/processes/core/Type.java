package ro.cs.tao.ogc.model.processes.core;

import ro.cs.tao.TaoEnum;

public enum Type implements TaoEnum<String> {
    ARRAY("array"),
    BOOLEAN("boolean"),
    INTEGER("integer"),
    NUMBER("number"),
    OBJECT("object"),
    STRING("string");

    private final String value;

    Type(String value) {
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
