package ro.cs.eo.gdal.reader.info;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ro.cs.tao.utils.AutoEvictableCache;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ProductDescriptorFactory {
    private static final TypeReference<Set<FormatDescriptor>> JSON_FORMAT_DESCRIPTOR_TYPE_REFERENCE = new TypeReference<Set<FormatDescriptor>>(){};
    private static final ProductDescriptorFactory instance;
    private final Set<FormatDescriptor> descriptors;
    private final AutoEvictableCache<Path, ProductDescriptor> descriptorCache;

    static {
        try {
            instance = new ProductDescriptorFactory();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ProductDescriptor getDescriptor(Path path) {
        return instance.descriptorCache.get(path);
    }

    private ProductDescriptorFactory() throws IOException {
        this.descriptors = new HashSet<>();
        try (InputStream inputStream = ProductDescriptorFactory.class.getResourceAsStream("descriptors.json")) {
            this.descriptors.addAll(new ObjectMapper().readValue(inputStream, JSON_FORMAT_DESCRIPTOR_TYPE_REFERENCE));
        }
        this.descriptorCache = new AutoEvictableCache<>(new Function<Path, ProductDescriptor>() {
            @Override
            public ProductDescriptor apply(Path path) {
                List<FormatDescriptor> descriptors = instance.descriptors.stream().filter(d -> d.matches(path.toString())).collect(Collectors.toList());
                if (descriptors.size() > 0) {
                    descriptors.sort(Comparator.comparingInt(FormatDescriptor::getPriority));
                    for (FormatDescriptor descriptor : descriptors) {
                        try {
                            return new ProductDescriptor(descriptor, path);
                        } catch (Exception ignored) {
                            //skip
                        }
                    }
                }
                throw new IllegalArgumentException("Unknown format for product: " + path.getFileName().toString());
            }
        }, 600);
    }
}
