package ro.cs.tao.ogc.model.processes.dru;

public class ExecutionUnit {
    private UnitType type;
    private String image;
    private DeploymentType deployment;
    private UnitConfig config;

    public UnitConfig getConfig() {
        return config;
    }

    public void setConfig(UnitConfig config) {
        this.config = config;
    }

    public DeploymentType getDeployment() {
        return deployment;
    }

    public void setDeployment(DeploymentType deployment) {
        this.deployment = deployment;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public UnitType getType() {
        return type;
    }

    public void setType(UnitType type) {
        this.type = type;
    }
}
