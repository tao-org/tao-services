package ro.cs.tao.ogc.model.processes.dru;

import ro.cs.tao.ogc.model.processes.core.Descriptor;
import ro.cs.tao.ogc.model.processes.core.Output;
import ro.cs.tao.ogc.model.processes.core.Subscriber;

import java.util.List;

public class ExecutionRequest {
    private List<Descriptor<?>> inputs;
    private Output output;
    private ResponseType response;
    private Subscriber subscriber;

    public List<Descriptor<?>> getInputs() {
        return inputs;
    }

    public void setInputs(List<Descriptor<?>> inputs) {
        this.inputs = inputs;
    }

    public Output getOutput() {
        return output;
    }

    public void setOutput(Output output) {
        this.output = output;
    }

    public ResponseType getResponse() {
        return response;
    }

    public void setResponse(ResponseType response) {
        this.response = response;
    }

    public Subscriber getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(Subscriber subscriber) {
        this.subscriber = subscriber;
    }
}
