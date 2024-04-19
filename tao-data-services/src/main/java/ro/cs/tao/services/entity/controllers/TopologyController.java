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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.Tag;
import ro.cs.tao.component.StringIdentifiable;
import ro.cs.tao.orchestration.Orchestrator;
import ro.cs.tao.persistence.NodeDBProvider;
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
    private NodeDBProvider nodeDBProvider;

    /**
     * List all the tags associated with topology nodes.
     */
    @RequestMapping(value = "/tags", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
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
    @RequestMapping(value = "/active", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getActiveNodes() {
        return prepareResult(service.getNodes(true));
    }
    /**
     * Returns the worker nodes that are not active.
     */
    @RequestMapping(value = "/inactive", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getInactiveNodes() {
        final List<NodeDescription> nodes = service.getNodes(false);
        nodes.removeIf(n -> n.getId().equalsIgnoreCase("localhost"));
        return prepareResult(nodes);
    }
    /**
     * Returns the list of supported node flavors.
     * Note: if the OpenStack provider is activated, it is the list provided by the remote environment.
     * Otherwise, it is a list of current nodes hardware configuration.
     */
    @RequestMapping(value = "/flavors", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getNodeFlavors() {
        return prepareResult(service.getNodeFlavors());
    }

    /**
     * Checks if the OpenStack (or other) external provider is active or not.
     */
    @RequestMapping(value = "/external", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> isExternalProviderAvailable() {
        return prepareResult(service.isExternalProviderAvailable());
    }

    /**
     * Returns a list of active node names (and 'Any') that will serve for assigning a component to be executed on a specific node.
     */
    @RequestMapping(value = "/available", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
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
    @RequestMapping(value = "/install", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
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
                serviceDescription = nodeDBProvider.getServiceDescription(serviceName, version);
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
    @RequestMapping(value = "/{id:.+}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> save(@RequestBody NodeDescription entity) {
        entity.setAppId(Orchestrator.getInstance().getId());
        return super.save(entity);
    }

    @Override
    @RequestMapping(value = "/{id:.+}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> update(@PathVariable("id") String id, @RequestBody NodeDescription entity) {
        final NodeDescription initial = service.findById(entity.getId());
        entity.setFlavor(initial.getFlavor());
        if (entity.getAppId() == null) {
            entity.setAppId(initial.getAppId());
        }
        if (entity.getServerId() == null) {
            entity.setServerId(initial.getServerId());
        }
        if (entity.getOwner() == null) {
            entity.setOwner(initial.getOwner());
        }
        return super.update(id, entity);
    }
}
