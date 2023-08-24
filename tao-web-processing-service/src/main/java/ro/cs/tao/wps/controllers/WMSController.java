package ro.cs.tao.wps.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.cs.tao.component.*;
import ro.cs.tao.component.enums.ParameterType;
import ro.cs.tao.component.enums.ProcessingComponentVisibility;
import ro.cs.tao.docker.Container;
import ro.cs.tao.docker.ContainerType;
import ro.cs.tao.eodata.enums.DataFormat;
import ro.cs.tao.security.SystemPrincipal;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.interfaces.ContainerService;
import ro.cs.tao.services.interfaces.WMSComponentService;
import ro.cs.tao.services.interfaces.WebServiceAuthenticationService;
import ro.cs.tao.wps.beans.LayerInfo;
import ro.cs.tao.wps.impl.WMSClient;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

@RestController
@RequestMapping("/wms")
public class WMSController extends BaseController {

    private final static Set<String> allowedRequests = new HashSet<String>() {{
        add("GetCapabilities"); add("GetMap");
    }};

    @Autowired
    private ContainerService containerService;
    @Autowired
    private WebServiceAuthenticationService webServiceAuthenticationService;
    @Autowired
    private WMSComponentService wmsComponentService;

    @RequestMapping(value = "/inspect", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<?> inspectRemote(@RequestParam(name = "request") String requestType,
                                           @RequestParam(name = "remoteAddress") String endpoint,
                                           @RequestParam(name = "capability", required = false) String capability,
                                           @RequestParam(name = "authentication", required = false) String authentication,
                                           @RequestParam(name = "save", required = false) Boolean save) {
        try {
            try {
                final URL url = new URL(endpoint);
            } catch (MalformedURLException mex) {
                throw new IllegalArgumentException("[remoteAddress] Malformed URL", mex);
            }
            if (!allowedRequests.contains(requestType)) {
                throw new IllegalArgumentException("[request] Unsupported value");
            }
            final WebServiceAuthentication auth = new ObjectMapper().readerFor(WebServiceAuthentication.class).readValue(authentication);
            final WMSClient client = new WMSClient(endpoint, auth, currentPrincipal());
            if ("GetCapabilities".equalsIgnoreCase(requestType)) {
                Container capabilities = client.getCapabilities();
                if (Boolean.TRUE.equals(save)) {
                    capabilities.setOwnerId(SystemPrincipal.instance().getName());
                    capabilities = containerService.save(capabilities);
                }
                return prepareResult(capabilities);
            } else {
                if (StringUtils.isEmpty(capability)) {
                    throw new IllegalArgumentException("[capability] Must supply a value");
                }
                Container container = containerService.listByType(ContainerType.WMS).stream()
                                                      .filter(c -> endpoint.equals(c.getApplicationPath()))
                                                      .findFirst().orElse(null);
                if (container == null) {
                    throw new IllegalArgumentException("WMS service not previously registered");
                }
                final LayerInfo layerInfo = client.describeLayer(capability);
                WMSComponent component = new WMSComponent();
                component.setId(endpoint + "~" + capability);
                component.setService(container);
                component.setCapabilityName(capability);
                component.setLabel(layerInfo.getName());
                String description = layerInfo.getDescription();
                final double[][] boundingBox = layerInfo.getBoundingBox();
                if (boundingBox != null) {
                    description += String.format(" [Bounding box: between (%.4f, %.4f) and (%.4f, %.4f)]",
                                                 boundingBox[0][0], boundingBox[0][1],
                                                 boundingBox[1][0], boundingBox[1][1]);
                }
                component.setDescription(description);
                component.setRemoteAddress(endpoint);
                component.setVisibility(ProcessingComponentVisibility.USER);
                component.setOwner(currentUser());
                component.setVersion("WMS " + layerInfo.getWmsVersion());
                List<ParameterDescriptor> parameters = new ArrayList<>();

                ParameterDescriptor descriptor = new ParameterDescriptor();
                descriptor.setId("bbox");
                descriptor.setName("bbox");
                descriptor.setLabel("Bounding box");
                descriptor.setDescription("Bounding box");
                descriptor.setType(ParameterType.REGULAR);
                descriptor.setDataType(double[].class);
                parameters.add(descriptor);

                descriptor = new ParameterDescriptor();
                descriptor.setId("srs");
                descriptor.setName("srs");
                descriptor.setLabel("SRS");
                descriptor.setDescription("Spatial reference system");
                descriptor.setType(ParameterType.REGULAR);
                descriptor.setDataType(String.class);
                descriptor.setDefaultValue(layerInfo.getCrs() != null ? layerInfo.getCrs() : "EPSG:4326");
                parameters.add(descriptor);

                final String[] formats = layerInfo.getFormats();
                if (formats != null && formats.length > 0) {
                    descriptor = new ParameterDescriptor();
                    descriptor.setId("format");
                    descriptor.setName("format");
                    descriptor.setLabel("Format");
                    descriptor.setDescription("Raster format");
                    descriptor.setType(ParameterType.REGULAR);
                    descriptor.setDataType(String.class);
                    descriptor.setDefaultValue(formats[0]);
                    descriptor.setValueSet(formats);
                    parameters.add(descriptor);
                }

                final List<String> styles = layerInfo.getStyles();
                if (styles != null && !styles.isEmpty()) {
                    descriptor = new ParameterDescriptor();
                    descriptor.setId("style");
                    descriptor.setName("style");
                    descriptor.setLabel("Style");
                    descriptor.setDescription("Style");
                    descriptor.setType(ParameterType.REGULAR);
                    descriptor.setDataType(String.class);
                    descriptor.setValueSet(styles.toArray(new String[0]));
                    parameters.add(descriptor);
                }

                component.setParameters(parameters);

                SourceDescriptor sourceDescriptor = new SourceDescriptor();
                sourceDescriptor.setId(UUID.randomUUID().toString());
                sourceDescriptor.setName("in");
                sourceDescriptor.setCardinality(-1);
                sourceDescriptor.setParentId(component.getId());
                sourceDescriptor.setDataDescriptor(new DataDescriptor() {{
                    setFormatType(DataFormat.OTHER);
                }});

                TargetDescriptor targetDescriptor = new TargetDescriptor();
                targetDescriptor.setId(UUID.randomUUID().toString());
                targetDescriptor.setName("out");
                targetDescriptor.setCardinality(1);
                targetDescriptor.setParentId(component.getId());
                targetDescriptor.setDataDescriptor(new DataDescriptor() {{
                    setFormatType(DataFormat.RASTER);
                }});
                component.addTarget(targetDescriptor);

                if (Boolean.TRUE.equals(save)) {
                    component = wmsComponentService.save(component);
                }
                return prepareResult(new WMSComponentBean(component));
            }
        } catch (Exception e) {
            return handleException(e);
        }
    }
}
