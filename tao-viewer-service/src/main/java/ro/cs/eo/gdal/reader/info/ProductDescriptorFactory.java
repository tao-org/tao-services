package ro.cs.eo.gdal.reader.info;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ProductDescriptorFactory {
    private static final TypeReference<Set<FormatDescriptor>> JSON_FORMAT_DESCRIPTOR_TYPE_REFERENCE = new TypeReference<Set<FormatDescriptor>>(){};
    private static final ProductDescriptorFactory instance;
    private final Set<FormatDescriptor> descriptors;

    static {
        try {
            instance = new ProductDescriptorFactory();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ProductDescriptor getDescriptor(Path path) throws IOException {
        List<FormatDescriptor> descriptors = instance.descriptors.stream().filter(d -> d.matches(path.toString())).collect(Collectors.toList());
        if (descriptors.size() > 0) {
            descriptors.sort(Comparator.comparingInt(FormatDescriptor::getPriority));
            return new ProductDescriptor(descriptors.get(0), path);
        } else {
            return null;
        }
    }

    private ProductDescriptorFactory() throws IOException {
        this.descriptors = new HashSet<>();
        try (InputStream inputStream = ProductDescriptorFactory.class.getResourceAsStream("descriptors.json")) {
            this.descriptors.addAll(new ObjectMapper().readValue(inputStream, JSON_FORMAT_DESCRIPTOR_TYPE_REFERENCE));
        }
    }
}
