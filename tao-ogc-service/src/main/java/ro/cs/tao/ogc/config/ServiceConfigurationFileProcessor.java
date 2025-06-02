package ro.cs.tao.ogc.config;

import ro.cs.tao.services.commons.config.ConfigurationFileProcessor;
import ro.cs.tao.utils.Tuple;

import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

public class ServiceConfigurationFileProcessor implements ConfigurationFileProcessor {

    @Override
    public Map<String, Tuple<String, FileTime>> getFileMap() {
        return new HashMap<String, Tuple<String, FileTime>>() {{
            put("tao-ogc.properties", new Tuple<>("/tao-ogc.properties",
                                                  FileTime.from(LocalDateTime.now().toInstant(ZoneOffset.UTC))));
        }};
    }

}
