package ro.cs.tao.ogc.model.processes.core;

public class ResultOutput {
    private String type;
    private Object value; // or String href if external
    private String href;

    public ResultOutput() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }
}
