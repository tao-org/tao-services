package ro.cs.tao.datasource.remote.aws.parameters;

import ro.cs.tao.datasource.common.ParameterDescriptor;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Cosmin Cara
 */
public class LandsatParameterProvider implements ro.cs.tao.datasource.common.ParameterProvider {

    @Override
    public Map<String, ParameterDescriptor> getSupportedParameters() {
        return Collections.unmodifiableMap(new HashMap<String, ParameterDescriptor>() {{
            put("platformName", new ParameterDescriptor("platformName", String.class, true));
            put("sensingStart", new ParameterDescriptor("sensingStart", Date.class));
            put("sensingEnd", new ParameterDescriptor("sensingEnd", Date.class));
            put("path", new ParameterDescriptor("path", String.class));
            put("row", new ParameterDescriptor("row", String.class));
            put("footprint",  new ParameterDescriptor("footprint", String.class));
            put("cloudcoverpercentage", new ParameterDescriptor("cloudcoverpercentage", Double.class));
            put("productType", new ParameterDescriptor("productType", String.class));
            put("collection", new ParameterDescriptor("collection", String.class));
        }});
    }
}
