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

/**
 * Custom deserializer for the {@link ContainerRequest} class.
 * This deserializer is responsible for converting JSON input into a {@link ContainerRequest} instance,
 * with special handling for the "containerDescriptor" property to map it to a {@link Container} object.
 * Extends {@link StdDeserializer} for Jackson-based deserialization.
 *
 * This class provides two constructors for flexibility and overrides the {@code deserialize} method
 * to implement the custom deserialization logic.
 */
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
