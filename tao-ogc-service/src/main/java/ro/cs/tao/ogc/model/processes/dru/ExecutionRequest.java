package ro.cs.tao.ogc.model.processes.dru;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import ro.cs.tao.ogc.model.processes.core.Input;
import ro.cs.tao.ogc.model.processes.core.Output;
import ro.cs.tao.ogc.model.processes.core.Subscriber;

import java.util.List;

public class ExecutionRequest {
    @JsonDeserialize(using = InputDescriptionDeserializer.class)
    private Input inputs;
    @JsonDeserialize(using = OutputDeserializer.class)
    private List<Output> outputs;
    private ResponseType response;
    private Subscriber subscriber;

    public Input getInputs() {
        return inputs;
    }

    public void setInputs(Input inputs) {
        this.inputs = inputs;
    }

    public List<Output> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<Output> outputs) {
        this.outputs = outputs;
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
