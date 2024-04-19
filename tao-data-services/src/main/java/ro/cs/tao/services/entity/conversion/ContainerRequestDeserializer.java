package ro.cs.tao.services.entity.conversion;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import ro.cs.tao.docker.Container;
import ro.cs.tao.services.entity.beans.ContainerRequest;

import java.io.IOException;

public class ContainerRequestDeserializer extends StdDeserializer<ContainerRequest> {

    public ContainerRequestDeserializer(Class<ContainerRequest> vc) {
        super(vc);
    }

    public ContainerRequestDeserializer() {
        this(ContainerRequest.class);
    }

    @Override
    public ContainerRequest deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        final ContainerRequest request = new ContainerRequest();
        super.deserialize(jsonParser, deserializationContext, request);
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        final ObjectMapper mapper = new ObjectMapper();
        request.setContainerDescriptor(mapper.readerFor(Container.class).readValue(node.get("containerDescriptor").toString()));
        return request;
    }
}
