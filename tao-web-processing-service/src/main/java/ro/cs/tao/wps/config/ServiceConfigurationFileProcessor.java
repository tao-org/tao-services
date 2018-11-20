package ro.cs.tao.wps.config;

import ro.cs.tao.services.commons.config.ConfigurationFileProcessor;

public class ServiceConfigurationFileProcessor implements ConfigurationFileProcessor {
    @Override
    public String getConfigFileName() { return "tao-wps.properties"; }

    @Override
    public String getConfigFileResourceLocation() { return "/tao-wps.properties"; }
}
