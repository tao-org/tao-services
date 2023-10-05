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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.cs.tao.Tag;
import ro.cs.tao.component.StringIdentifiable;
import ro.cs.tao.persistence.NodeProvider;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.TopologyService;
import ro.cs.tao.topology.NodeDescription;
import ro.cs.tao.topology.ServiceDescription;
import ro.cs.tao.utils.StringUtilities;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Cosmin Cara
 */
@RestController
@RequestMapping("/topology")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Topology", description = "Operations related to TAO worker nodes")
public class TopologyController extends DataEntityController<NodeDescription, String, TopologyService> {

    @Autowired
    private NodeProvider nodeProvider;

    /**
     * List all the tags associated with topology nodes.
     */
    @RequestMapping(value = "/tags", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> listTags() {
        List<Tag> objects = service.getNodeTags();
        if (objects == null ) {
            objects = new ArrayList<>();
        }
        return prepareResult(objects.stream().map(Tag::getText).collect(Collectors.toList()));
    }

    /**
     * Returns the worker nodes that are active.
     */
    @RequestMapping(value = "/active", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getActiveNodes() {
        return prepareResult(service.getNodes(true));
    }
    /**
     * Returns the worker nodes that are not active.
     */
    @RequestMapping(value = "/inactive", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getInactiveNodes() {
        return prepareResult(service.getNodes(false));
    }
    /**
     * Returns the list of supported node flavors.
     * Note: if the OpenStack provider is activated, it is the list provided by the remote environment.
     * Otherwise, it is a list of current nodes hardware configuration.
     */
    @RequestMapping(value = "/flavors", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getNodeFlavors() {
        return prepareResult(service.getNodeFlavors());
    }

    /**
     * Checks if the OpenStack (or other) external provider is active or not.
     */
    @RequestMapping(value = "/external", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> isExternalProviderAvailable() {
        return prepareResult(service.isExternalProviderAvailable());
    }

    /**
     * Returns a list of active node names (and 'Any') that will serve for assigning a component to be executed on a specific node.
     */
    @RequestMapping(value = "/available", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getNodeAffinities() {
        List<NodeDescription> activeNodes = service.getNodes(true);
        List<String> names = activeNodes.stream().map(StringIdentifiable::getId).sorted(Comparator.naturalOrder()).collect(Collectors.toList());
        names.add(0, "Any");
        return prepareResult(names);
    }

    /**
     * Installs a necessary service on a node.
     * If the service name is not given, it will try to install all the necessary services.
     * If the service version is not given (but the service is given), it is assumed to be 1.0.
     *
     * @param nodeId The node (host) name
     * @param serviceName The name of the service
     * @param version   The version of the service
     */
    @RequestMapping(value = "/install", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> installServices(@RequestParam("nodeId") String nodeId,
                                                              @RequestParam(name = "service", required = false) String serviceName,
                                                              @RequestParam(name = "version", required = false) String version) {
        try {
            final NodeDescription node = service.findById(nodeId);
            if (node == null) {
                throw new IllegalArgumentException("No such node");
            }
            final ServiceDescription serviceDescription;
            if (!StringUtilities.isNullOrEmpty(serviceName)) {
                if (StringUtilities.isNullOrEmpty(version)) {
                    version = "1.0";
                }
                serviceDescription = nodeProvider.getServiceDescription(serviceName, version);
            } else {
                serviceDescription = null;
            }
            asyncExecute(() -> service.installServices(node, serviceDescription));
            return prepareResult("Services installation started on node " + nodeId);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @Override
    public ResponseEntity<ServiceResponse<?>> save(NodeDescription entity) {
        return super.save(entity);
    }

    @Override
    public ResponseEntity<ServiceResponse<?>> update(String id, NodeDescription entity) {
        return super.update(id, entity);
    }
}
