package ro.cs.tao.wps.impl;

import com.bc.wps.api.WpsServerContext;
import com.bc.wps.api.WpsServiceInstance;
import com.bc.wps.api.WpsServiceProvider;
import com.bc.wps.api.exceptions.WpsRuntimeException;
import com.bc.wps.utilities.PropertiesWrapper;

import java.io.IOException;

/**
 * @author Sabine
 */
public class WpsSpi implements WpsServiceProvider {

    @Override
    public String getId() {
        return "TAO";
    }

    @Override
    public String getName() {
        return "TAO WPS Server";
    }

    @Override
    public String getDescription() {
        return "This is a TAO WPS implementation";
    }

    @Override
    public WpsServiceInstance createServiceInstance(WpsServerContext wpsServerContext) {
        try {
            PropertiesWrapper.loadConfigFile("tao-wps.properties");
        } catch (IOException exception) {
            throw new WpsRuntimeException("Unable to load tao-wps.properties file", exception);
        }
        return new WebProcessingServiceImpl();
    }
}
