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
import org.springframework.web.multipart.MultipartFile;
import ro.cs.tao.Tag;
import ro.cs.tao.component.StringIdentifiable;
import ro.cs.tao.orchestration.Orchestrator;
import ro.cs.tao.persistence.AdminKeyProvider;
import ro.cs.tao.persistence.NodeDBProvider;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.TopologyService;
import ro.cs.tao.topology.AdminKey;
import ro.cs.tao.topology.AdminKeyId;
import ro.cs.tao.topology.NodeDescription;
import ro.cs.tao.topology.ServiceDescription;
import ro.cs.tao.utils.StringUtilities;
import ro.cs.tao.utils.executors.MemoryUnit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    @Autowired
    private AdminKeyProvider adminKeyProvider;

    /**
     * List all the tags associated with topology nodes.
     */
    @RequestMapping(value = { "/tags", "/tags/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
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
    @RequestMapping(value = { "/active", "/active/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
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
    @RequestMapping(value = { "/flavors", "/flavors/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getNodeFlavors() {
        return prepareResult(service.getNodeFlavors());
    }

    /**
     * Checks if the OpenStack (or other) external provider is active or not.
     */
    @RequestMapping(value = { "/external", "/external/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> isExternalProviderAvailable() {
        return prepareResult(service.isExternalProviderAvailable());
    }

    /**
     * Returns a list of active node names (and 'Any') that will serve for assigning a component to be executed on a specific node.
     */
    @RequestMapping(value = { "/available", "/available/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
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
    @RequestMapping(value = { "/install", "/install/" }, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
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
    @RequestMapping(value = { "/{id:.+}", "/{id:.+}/" }, method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> save(@RequestBody NodeDescription entity) {
        prepareForSave(entity);
        return super.save(entity);
    }

    @RequestMapping(value = { "/{id:.+}", "/{id:.+}/" }, method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> save(@RequestPart("entity") NodeDescription entity,
                                                   @RequestPart(value = "sshKey", required = false) MultipartFile sshKey) {
        try {
            prepareForSave(entity);
            return persistSshKeyOnSuccess(super.save(entity), sshKey);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @Override
    @RequestMapping(value = { "/{id:.+}", "/{id:.+}/" }, method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> update(@PathVariable("id") String id, @RequestBody NodeDescription entity) {
        boolean isActive = entity.getActive();
        prepareForUpdate(entity);
        entity.setActive(isActive);
        return super.update(id, entity);
    }

    @RequestMapping(value = { "/{id:.+}", "/{id:.+}/" }, method = RequestMethod.PUT, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> update(@PathVariable("id") String id,
                                                     @RequestPart("entity") NodeDescription entity,
                                                     @RequestPart(value = "sshKey", required = false) MultipartFile sshKey) {
        try {
            prepareForUpdate(entity);
            return persistSshKeyOnSuccess(super.update(id, entity), sshKey);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    private void prepareForSave(NodeDescription entity) {
        entity.setAppId(Orchestrator.getInstance().getId());
        if (entity.getSshKey() != null && entity.getSshKey().trim().isEmpty()) {
            entity.setSshKey(null);
        }
        if (entity.getUserPass() != null && entity.getUserPass().trim().isEmpty()) {
            entity.setUserPass(null);
        }
        if (entity.getFlavor().getMemoryUnit() == null) {
            entity.getFlavor().setMemoryUnit(MemoryUnit.GB); // by default, consider GB as memory unit in cases when it's not specified
        }
    }

    private void prepareForUpdate(NodeDescription entity) {
        final NodeDescription initial = service.findById(entity.getId());
        entity.setFlavor(initial.getFlavor());
        entity.setActive(initial.getActive());
        entity.setVolatile(initial.getVolatile());
        entity.setServicesStatus(initial.getServicesStatus());
        entity.setTags(initial.getTags());
        if (entity.getSshKey() != null && entity.getSshKey().trim().isEmpty()) {
            entity.setSshKey(null);
        }
        if (entity.getUserPass() != null && entity.getUserPass().trim().isEmpty()) {
            entity.setUserPass(null);
        }
        if (entity.getAppId() == null) {
            entity.setAppId(initial.getAppId());
        }
        if (entity.getServerId() == null) {
            entity.setServerId(initial.getServerId());
        }
        if (entity.getOwner() == null) {
            entity.setOwner(initial.getOwner());
        }
    }

    private ResponseEntity<ServiceResponse<?>> persistSshKeyOnSuccess(ResponseEntity<ServiceResponse<?>> response,
                                                                      MultipartFile sshKey) throws IOException, PersistenceException {
        final ServiceResponse<?> body = response.getBody();
        if (body != null
                && body.getStatus() == ResponseStatus.SUCCEEDED
                && body.getData() instanceof NodeDescription node) {
            persistSshKey(sshKey, node);
        }
        return response;
    }

    private void persistSshKey(MultipartFile sshKey, NodeDescription node) throws IOException, PersistenceException {
        if (sshKey == null || sshKey.isEmpty()) {
            return;
        }
        final String key = new String(sshKey.getBytes(), StandardCharsets.UTF_8);
        if (key.trim().isEmpty()) {
            return;
        }
        final AdminKey adminKey = new AdminKey(currentOwner(), node.getId(), key);
        final AdminKeyId id = adminKey.getId();
        if (adminKeyProvider.get(id) == null) {
            adminKeyProvider.save(adminKey);
        } else {
            adminKeyProvider.update(adminKey);
        }
    }
}
