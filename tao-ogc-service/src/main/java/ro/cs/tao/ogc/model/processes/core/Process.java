package ro.cs.tao.ogc.model.processes.core;

import java.util.List;
import java.util.Map;

public class Process extends ProcessSummary {
    private Map<String, InputDescription<?>> inputs;
    private Map<String, OutputDescription> outputs;

    public Map<String, InputDescription<?>> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String, InputDescription<?>> inputs) {
        this.inputs = inputs;
    }

    public Map<String, OutputDescription> getOutputs() {
        return outputs;
    }

    public void setOutputs(Map<String, OutputDescription> outputs) {
        this.outputs = outputs;
    }
}
