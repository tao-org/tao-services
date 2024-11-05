package ro.cs.tao.ogc.model.processes.core;

import java.util.List;

public class Process extends ProcessSummary {
    private List<InputDescription<?>> inputDescription;
    private List<OutputDescription> outputDescription;

    public List<InputDescription<?>> getInputDescription() {
        return inputDescription;
    }

    public void setInputDescription(List<InputDescription<?>> inputDescription) {
        this.inputDescription = inputDescription;
    }

    public List<OutputDescription> getOutputDescription() {
        return outputDescription;
    }

    public void setOutputDescription(List<OutputDescription> outputDescription) {
        this.outputDescription = outputDescription;
    }
}
