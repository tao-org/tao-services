package ro.cs.tao.ogc.model.processes.core;

import ro.cs.tao.TaoEnum;

public enum ValuePassing implements TaoEnum<String> {
    BYVALUE("byValue"),
    BYREFERENCE("byReference");

    private final String value;

    ValuePassing(String value) {
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
        return this.toString();
    }
}
