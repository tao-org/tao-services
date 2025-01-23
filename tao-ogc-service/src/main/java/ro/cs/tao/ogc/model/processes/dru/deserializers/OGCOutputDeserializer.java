package ro.cs.tao.ogc.model.processes.dru.deserializers;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import ro.cs.tao.ogc.model.processes.core.execution.Format;
import ro.cs.tao.ogc.model.processes.core.execution.Output;
import ro.cs.tao.ogc.model.processes.core.execution.TransmissionMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class OGCOutputDeserializer extends StdDeserializer<List<Output>> {
    public OGCOutputDeserializer(){
        this(null);
    }

    public OGCOutputDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public List<Output> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        List<Output> outputsList = new ArrayList<>();
        try {
            JsonNode node = jsonParser.getCodec().readTree(jsonParser);
            Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
            while(iterator.hasNext()){
                Map.Entry<String, JsonNode> entry = iterator.next();
                Output output = parseOutput(entry.getValue());
                output.setName(entry.getKey());
                outputsList.add(output);
            }
        }
        catch (IOException e){
            throw new IOException(e.getMessage());
        }
        return outputsList;
    }

    private Output parseOutput(JsonNode node){
        Output output = new Output();
        Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
        while(iterator.hasNext()){
            Map.Entry<String, JsonNode> entry = iterator.next();
            if(entry.getKey().equals("transmissionMode"))
                output.setTransmissionMode(entry.getValue().textValue().equals(TransmissionMode.VALUE.friendlyName())
                        ? TransmissionMode.VALUE : TransmissionMode.REFERENCE);
            if(entry.getKey().equals("format"))
                output.setFormat(parseFormat(entry.getValue()));
        }
        return output;
    }

    private Format parseFormat(JsonNode jsonNode){
        Format format = new Format();
        if(jsonNode.get("mediaType") != null)
            format.setMediaType(jsonNode.get("mediaType").asText());
        if(jsonNode.get("encoding") != null)
            format.setEncoding(jsonNode.get("encoding").asText());
        if(jsonNode.get("schema") != null)
            format.setSchema(jsonNode.get("schema").asText());
        return format;
    }


}
