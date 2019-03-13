package ro.cs.tao.wps.config;

import ro.cs.tao.services.commons.config.ConfigurationFileProcessor;

public class ServiceConfigurationFileProcessor implements ConfigurationFileProcessor {
    @Override
    public String getFileName() { return "tao-wps.properties"; }

    @Override
    public String getFileResourceLocation() { return "/tao-wps.properties"; }
}
