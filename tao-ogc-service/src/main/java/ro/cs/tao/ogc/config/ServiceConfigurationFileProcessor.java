package ro.cs.tao.ogc.config;

import ro.cs.tao.services.commons.config.ConfigurationFileProcessor;

import java.util.HashMap;
import java.util.Map;

public class ServiceConfigurationFileProcessor implements ConfigurationFileProcessor {

    @Override
    public Map<String, String> getFileMap() {
        return new HashMap<String, String>() {{
            put("tao-ogc.properties", "/tao-ogc.properties");
        }};
    }

}
