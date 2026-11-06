package ro.cs.tao.ogc.model.processes.dru;

import ro.cs.tao.ogc.model.processes.core.execution.Input;
import ro.cs.tao.ogc.model.processes.core.execution.Output;
import ro.cs.tao.ogc.model.processes.core.execution.Subscriber;
import ro.cs.tao.ogc.model.processes.dru.deserializers.OGCInputDeserializer;
import ro.cs.tao.ogc.model.processes.dru.deserializers.OGCOutputDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

public class ExecutionRequest {
    @JsonDeserialize(using = OGCInputDeserializer.class)
    private Input inputs;
    @JsonDeserialize(using = OGCOutputDeserializer.class)
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
