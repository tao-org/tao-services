package ro.cs.tao.services.jupyter.model;


import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Cell {
    private String cell_type = "code";
    private int execution_count = 0;
    private String id;
    private CellMetadata metadata = new CellMetadata();
    private List<String> source = Collections.singletonList("print(\"Welcome to Notebook\")");
    private List<Output> outputs;

    public String getCell_type() {
        return cell_type;
    }

    public void setCell_type(String cell_type) {
        this.cell_type = cell_type;
    }
    public void setExecution_count(int execution_count) {this.execution_count = execution_count;}
    public int getExecution_count() { return this.execution_count; }
    public String getId() {return this.id;}
    public void setId(String id) { this.id = id;}
    public CellMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(CellMetadata metadata) {
        this.metadata = metadata;
    }

    public List<String> getSource() {
        return source;
    }

    public void setSource(List<String> source) {
        this.source = source;
    }

    public List<Output> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<Output> outputs) {
        this.outputs = outputs;
    }
}
