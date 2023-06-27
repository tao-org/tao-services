package ro.cs.tao.services.jupyter.model;


import java.util.List;

public class Cell {
    private String cell_type;
    private CellMetadata metadata;
    private List<String> source;
    private List<Output> outputs;

    public String getCell_type() {
        return cell_type;
    }

    public void setCell_type(String cell_type) {
        this.cell_type = cell_type;
    }

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
