package ro.cs.tao.ogc.model.processes.dru;

import ro.cs.tao.TaoEnum;

public enum DeploymentType implements TaoEnum<String> {
    LOCAL("local"),
    REMOTE("remote"),
    HPC("hpc"),
    CLOUD("cloud");

    private final String value;

    DeploymentType(String value) {
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
