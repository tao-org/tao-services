package ro.cs.tao.services.entity.conversion;

import ro.cs.tao.docker.Container;
import ro.cs.tao.services.entity.beans.ContainerRequest;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.deser.std.StdDeserializer;

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
    public ContainerRequest deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws JacksonException {
        final ContainerRequest request = new ContainerRequest();
        super.deserialize(jsonParser, deserializationContext, request);
        JsonNode node = jsonParser.readValueAsTree();
        final ObjectMapper mapper = new ObjectMapper();
        request.setContainerDescriptor(mapper.readerFor(Container.class).readValue(node.get("containerDescriptor").toString()));
        return request;
    }
}
