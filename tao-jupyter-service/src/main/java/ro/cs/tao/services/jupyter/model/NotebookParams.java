package ro.cs.tao.services.jupyter.model;

public class NotebookParams {
    private String name;

    private String dataType;

    private String defaultValue;

    public String getName() {
        return this.name;
    }

    public String getDataType() {
        return this.dataType;
    }

    public String getDefaultValue() {
        return this.defaultValue;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isNull()
    {
        return (this.name == null) && (this.defaultValue == null);
    }
}
