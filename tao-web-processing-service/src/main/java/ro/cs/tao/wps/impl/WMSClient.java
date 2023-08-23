package ro.cs.tao.wps.impl;

import org.geotools.ows.ServiceException;
import org.geotools.ows.wms.*;
import org.geotools.ows.wms.xml.MetadataURL;
import ro.cs.tao.component.WebServiceAuthentication;
import ro.cs.tao.docker.Application;
import ro.cs.tao.docker.Container;
import ro.cs.tao.docker.ContainerType;
import ro.cs.tao.execution.wps.WPSHttpClient;
import ro.cs.tao.wps.beans.LayerInfo;

import java.io.IOException;
import java.net.URL;
import java.security.Principal;
import java.util.HashSet;
import java.util.List;

public class WMSClient {
    private final String url;
    private final WebServiceAuthentication authentication;
    private final Principal currentPrincipal;

    public WMSClient(String url, WebServiceAuthentication authentication, Principal principal) {
        this.url = url;
        this.authentication = authentication;
        this.currentPrincipal = principal;
    }

    public Container getCapabilities() throws ServiceException, IOException {
        final WebMapServer service = getService();
        final WMSCapabilities capabilities = service.getCapabilities();
        final Container container = new Container();
        container.setFormat(new HashSet<>() {{ add("WMS"); }});
        container.setId(url);
        container.setApplicationPath(url);
        container.setName(capabilities.getService().getName());
        container.setType(ContainerType.WMS);
        final Layer[] layers = WMSUtils.getNamedLayers(capabilities);
        for (Layer layer : layers) {
            final Application application = new Application();
            application.setName(layer.getName());
            final List<MetadataURL> metadataURLS = layer.getMetadataURL();
            if (metadataURLS != null && !metadataURLS.isEmpty()) {
                application.setPath(metadataURLS.get(0).getUrl().toString());
            }
            container.addApplication(application);
        }
        return container;
    }

    public LayerInfo describeLayer(String layerName) throws ServiceException, IOException {
        final WebMapServer service = getService();
        final WMSCapabilities capabilities = service.getCapabilities();
        final Layer layer = capabilities.getLayerList().stream().filter(l -> l.getName().equals(layerName)).findFirst().orElse(null);
        if (layer == null) {
            throw new IOException("Layer " + layerName  + " does not exist");
        }
        LayerInfo info = new LayerInfo();
        info.setName(layer.getName());
        info.setDescription(layer.getTitle());
        final CRSEnvelope box = layer.getLatLonBoundingBox();
        info.setCrs(layer.getSrs().iterator().next());
        double[][] bbox = new double[2][];
        bbox[0] = box.getLowerCorner().getCoordinate();
        bbox[1] = box.getUpperCorner().getCoordinate();
        info.setBoundingBox(bbox);
        info.setFormats(capabilities.getRequest().getGetMap().getFormats().toArray(new String[0]));
        info.setWmsVersion(capabilities.getVersion());
        final List<StyleImpl> styles = layer.getStyles();
        if (styles != null) {
            for (StyleImpl style : styles) {
                info.addStyle(style.getName());
            }
        }
        return info;
    }

    private WebMapServer getService() throws ServiceException, IOException {
        return new WebMapServer(new URL(this.url),
                                new WPSHttpClient(this.authentication),
                                null);
    }
}
