package ro.cs.tao.ogc.model.processes.dru;

import ro.cs.tao.TaoEnum;

public enum UnitType implements TaoEnum<String> {
    DOCKER("docker"),
    OCI("oci");

    private final String value;

    UnitType(String value) {
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
