/*
 * Copyright (C) 2018 CS ROMANIA
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package ro.cs.tao.services.entity.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import ro.cs.tao.Tag;
import ro.cs.tao.component.StringIdentifiable;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.TopologyService;
import ro.cs.tao.topology.NodeDescription;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Cosmin Cara
 */
@RestController
@RequestMapping("/topology")
public class TopologyController extends DataEntityController<NodeDescription, String, TopologyService> {

    @RequestMapping(value = "/tags", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> listTags() {
        List<Tag> objects = service.getNodeTags();
        if (objects == null ) {
            objects = new ArrayList<>();
        }
        return prepareResult(objects.stream().map(Tag::getText).collect(Collectors.toList()));
    }

    @RequestMapping(value = "/active", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getActiveNodes() {
        return prepareResult(service.getNodes(true));
    }

    @RequestMapping(value = "/inactive", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getInactiveNodes() {
        return prepareResult(service.getNodes(false));
    }

    @RequestMapping(value = "/flavors", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getNodeFlavors() {
        return prepareResult(service.getNodeFlavors());
    }

    @RequestMapping(value = "/external", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> isExternalProviderAvailable() {
        return prepareResult(service.isExternalProviderAvailable());
    }

    @RequestMapping(value = "/available", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getNodeAffinities() {
        List<NodeDescription> activeNodes = service.getNodes(true);
        List<String> names = activeNodes.stream().map(StringIdentifiable::getId).sorted(Comparator.naturalOrder()).collect(Collectors.toList());
        names.add(0, "Any");
        return prepareResult(names);
    }

    /*@RequestMapping(path = "/{host:.+}", method = RequestMethod.GET, produces = "application/json")
    public void tunnelWebSSH(@PathVariable("host") String host, HttpServletRequest request, HttpServletResponse response) {
        try {
            HttpUriRequest proxiedRequest = createHttpUriRequest(request, host);
            CloseableHttpClient httpClient = HttpClients.createMinimal();
            HttpResponse proxiedResponse = httpClient.execute(proxiedRequest);
            writeToResponse(proxiedResponse, response);
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
    }

    private HttpUriRequest createHttpUriRequest(HttpServletRequest request, String host) throws URISyntaxException {
        final URI uri = new URI("http://" + host + ":4200/");
        final RequestBuilder rb = RequestBuilder.create(request.getMethod());
        rb.setUri(uri);
        final Enumeration<String> headerNames = request.getHeaderNames();
        while(headerNames.hasMoreElements()){
            String headerName = headerNames.nextElement();
            if(!headerName.equalsIgnoreCase("accept-encoding")) {
                rb.addHeader(headerName, request.getHeader(headerName));
            }
        }

        return rb.build();
    }

    private void writeToResponse(HttpResponse proxiedResponse, HttpServletResponse response) throws IOException {
        for (Header header : proxiedResponse.getAllHeaders()) {
            if ((! header.getName().equals("Transfer-Encoding")) || (! header.getValue().equals("chunked"))) {
                response.addHeader(header.getName(), header.getValue());
            }
        }
        try (InputStream is = proxiedResponse.getEntity().getContent();
             OutputStream os = response.getOutputStream()) {
            IOUtils.copy(is, os);
        }
    }*/
}
