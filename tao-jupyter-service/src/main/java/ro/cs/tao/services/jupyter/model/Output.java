package ro.cs.tao.services.jupyter.model;

import java.util.List;
import java.util.Map;

public class Output {
    private String output_type;
    private String name;
    private List<String> text;
    private Map<String, Object> data;
    private Map<String, Object> metadata;

    public String getOutput_type() {
        return output_type;
    }

    public void setOutput_type(String output_type) {
        this.output_type = output_type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getText() {
        return text;
    }

    public void setText(List<String> text) {
        this.text = text;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
