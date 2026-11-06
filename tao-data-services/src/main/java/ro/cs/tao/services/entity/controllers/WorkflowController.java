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

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ro.cs.tao.BaseException;
import ro.cs.tao.Sort;
import ro.cs.tao.SortDirection;
import ro.cs.tao.Tag;
import ro.cs.tao.component.*;
import ro.cs.tao.component.validation.ValidationException;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.docker.Container;
import ro.cs.tao.eodata.enums.Visibility;
import ro.cs.tao.messaging.Messaging;
import ro.cs.tao.messaging.Topic;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.UserProvider;
import ro.cs.tao.persistence.WorkflowNodeProvider;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.RoleRequired;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.entity.beans.WorkflowGroupNodeRequest;
import ro.cs.tao.services.interfaces.*;
import ro.cs.tao.services.model.execution.ExecutionTaskInfo;
import ro.cs.tao.services.model.workflow.WorkflowInfo;
import ro.cs.tao.spi.ServiceRegistry;
import ro.cs.tao.spi.ServiceRegistryManager;
import ro.cs.tao.user.User;
import ro.cs.tao.utils.FileUtilities;
import ro.cs.tao.utils.StringUtilities;
import ro.cs.tao.workflow.ParameterValue;
import ro.cs.tao.workflow.WorkflowDescriptor;
import ro.cs.tao.workflow.WorkflowNodeDescriptor;
import ro.cs.tao.workflow.WorkflowNodeGroupDescriptor;
import ro.cs.tao.workflow.enums.Status;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Cosmin Cara
 */
@RestController
@RequestMapping({ "/workflow", "/workflow/" })
@io.swagger.v3.oas.annotations.tags.Tag(name = "Workflows", description = "Operations related to workflows")
public class WorkflowController extends DataEntityController<WorkflowDescriptor, Long, WorkflowService> {

    @Autowired
    private ComponentService componentService;

    @Autowired
    private DataSourceComponentService dataSourceComponentService;

    @Autowired
    private WorkflowNodeProvider workflowNodeProvider;

    @Autowired
    private UserProvider userProvider;

    @Autowired
    private ConditionComponentService conditionComponentService;

    @Autowired
    private ContainerService containerService;

    /**
     * Lists the workflows that are visible to the current user, optionally specifying the pagination of results.
     *
     * @param pageNumber    (optional) The page number
     * @param pageSize      (optional) The page size
     * @param sortByField   (optional) The field to sort on
     * @param sortDirection (optional) The sort direction
     * @param active        Active or Inactive workflows required
     */
    @RequestMapping(value = { "/", "" }, params = "active", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> listActive(@RequestParam(name = "pageNumber", required = false) Optional<Integer> pageNumber,
                                                   @RequestParam(name = "pageSize", required = false) Optional<Integer> pageSize,
                                                   @RequestParam(name = "sortBy", required = false) Optional<String> sortByField,
                                                   @RequestParam(name = "sortDirection", required = false) Optional<SortDirection> sortDirection,
                                                   @RequestParam(name = "active") int active) {
        final String ownerId = currentOwner();
        if (pageNumber.isPresent() && sortByField.isPresent()) {
            Sort sort = new Sort().withField(sortByField.get(), sortDirection.orElse(SortDirection.ASC));
            return prepareResult(service.getUserVisibleWorkflows(ownerId, active != 0,
                                                                 pageNumber.get(), pageSize.orElse(10), sort));
        } else {
            return prepareResult(service.getUserVisibleWorkflows(ownerId, active != 0));
        }
    }

    @RequestMapping(value = { "/count", "/count/" }, params = "active", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> countActive(@RequestParam(name = "active") int active) {
        return prepareResult(service.countUserVisibleWorkflows(currentOwner(), active != 0));
    }

    /**
     * Returns the descriptor of a workflow
     * @param id    The workflow identifier
     */
    @Override
    @RequestMapping(value = { "/{id:.+}", "/{id:.+}/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> get(@PathVariable("id") Long id) {
        ResponseEntity<ServiceResponse<?>> response;
        WorkflowDescriptor entity = service.getFullDescriptor(id);
        if (entity == null) {
            response = prepareResult(String.format("Entity [%s] not found", id), ResponseStatus.FAILED);
        } else {
            Map<String, List<String>> containerFormats = new HashMap<>();
            entity.getNodes().forEach(n -> {
                if (n.getComponentId() != null) {
                    TaoComponent component = componentService.findById(n.getComponentId());
                    if(component == null) {
                        component = conditionComponentService.findById(n.getComponentId());
                    }
                    if (component != null
                            && (n.getAdditionalInfo() == null || n.getAdditionalInfo().stream().noneMatch(p -> "nodeAffinity".equals(p.getParameterName())))
                            && component.getNodeAffinity() != null) {
                        n.addInfo("nodeAffinity", component.getNodeAffinity().getValue());
                    }
                    if (component instanceof ProcessingComponent) {
                        String format = component.getTargets().stream()
                                .filter(t -> !StringUtilities.isNullOrEmpty(t.getDataDescriptor().getFormatName()))
                                .map(t -> t.getDataDescriptor().getFormatName())
                                .findFirst().orElse(null);
                        String containerId = ((ProcessingComponent) component).getContainerId();
                        if (!containerFormats.containsKey(containerId)) {
                            Container container = containerService.findById(containerId);
                            if (container.getFormatNameParameter() != null) {
                                Set<String> formats = container.getFormat();
                                if (formats != null && !formats.isEmpty()) {
                                    containerFormats.put(containerId,
                                                         new ArrayList<>(formats).stream().sorted().collect(Collectors.toList()));
                                } else {
                                    containerFormats.put(containerId, new ArrayList<>());
                                }
                            }
                        }
                        List<String> list = containerFormats.get(containerId);
                        if (list != null) {
                            n.addInfo("formatName", format != null ? format : "");
                            if (!list.isEmpty()) {
                                n.addInfo("formatNameValues", String.join(",", list));
                            }
                        }
                    }
                }
            });
            response = prepareResult(entity);
        }
        return response;
    }

    /**
     * Retrieves the image associated with the specified workflow ID.
     *
     * @param workflowId the identifier of the workflow for which the image is to be retrieved
     * @return a ResponseEntity containing the service response with the workflow image or an error response in case of exceptions
     */
    @RequestMapping(value = { "/{id:.+}/image", "/{id:.+}/image/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getImage(@PathVariable("id") long workflowId) {
        try {
            return prepareResult(service.getWorkflowImage(workflowId));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * Adds an image to a specified workflow.
     *
     * @param workflowId the unique identifier of the workflow to which the image should be added
     * @param image the image file to be added, provided as a {@link MultipartFile}
     * @return a {@link ResponseEntity} containing a {@link ServiceResponse} object with the workflow details or error information
     */
    @RequestMapping(value = { "/{id:.+}/image", "/{id:.+}/image/" }, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> addImage(@PathVariable("id") long workflowId,
                                                       @RequestParam("image") MultipartFile image) {
        try {
            if (image != null) {
                service.addWorkflowImage(workflowId, Base64.getEncoder().encodeToString(image.getBytes()));
            }
            return prepareResult(service.getWorkflowInfo(workflowId));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * Updates the image associated with the specified workflow.
     *
     * @param workflowId the unique identifier of the workflow whose image is to be updated
     * @param image the new image file to associate with the workflow
     * @return a ResponseEntity containing the updated workflow information
     */
    @RequestMapping(value = { "/{id:.+}/image", "/{id:.+}/image/" }, method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> updateImage(@PathVariable("id") long workflowId,
                                                          @RequestParam("image") MultipartFile image) {
        try {
            if (image != null) {
                service.updateWorkflowImage(workflowId, Base64.getEncoder().encodeToString(image.getBytes()));
            }
            return prepareResult(service.getWorkflowInfo(workflowId));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * Deletes the image associated with the given workflow ID.
     *
     * @param workflowId the unique identifier of the workflow whose image is to be deleted
     * @return a ResponseEntity containing a ServiceResponse with the updated workflow information,
     *         or an appropriate error response in case of failure
     */
    @RequestMapping(value = { "/{id:.+}/image", "/{id:.+}/image/" }, method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> deleteImage(@PathVariable("id") long workflowId) {
        try {
            service.deleteWorkflowImage(workflowId);
            return prepareResult(service.getWorkflowInfo(workflowId));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = { "/{id:.+}/remove", "/{id:.+}/remove/" }, method = RequestMethod.DELETE)
    public ResponseEntity<ServiceResponse<?>> remove(@PathVariable("id") long id) {
        ResponseEntity<ServiceResponse<?>> response;
        try {
            service.removeWorkflow(id);
            response = prepareResult("", ResponseStatus.SUCCEEDED);
        } catch (PersistenceException e) {
            response = handleException(e);
        }
        return response;
    }

    /**
     * Lists all the user workflows having a given status.
     * @param status    The workflow status (@see {@link Status})
     */
    @RequestMapping(value = { "/status/{status}", "/status/{status}/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getUserWorkflowsByStatus(@PathVariable("status") Status status,
                                                                       @RequestParam(name = "userId", required = false) String userId) {
        if (!StringUtilities.isNullOrEmpty(userId) && !isCurrentUserAdmin()) {
            return unauthorizedResponse();
        }
        return prepareResult(service.getUserWorkflowsByStatus(userId == null ? currentOwner() : userId, status));
    }

    /**
     * Lists all the published user workflows by visibility
     * @param visibility    The workflow visibiliti (@see {@link Visibility})
     *
     */
    @RequestMapping(value = { "/visibility/{visibility}", "/visibility/{visibility}/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @RoleRequired(roles = ro.cs.tao.user.Role.ADMIN)
    public ResponseEntity<ServiceResponse<?>> getUserWorkflowsByVisibility(@PathVariable("visibility") Visibility visibility,
                                                                           @RequestParam(name = "userId", required = false) String userId) {
        return prepareResult(service.getUserPublishedWorkflowsByVisibility(currentOwner(), visibility));
    }

    /**
     * List all the workflows published by users other than the current user.
     */
    @RequestMapping(value = { "/public", "/public/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getOtherPublicWorkflows() {
        return prepareResult(service.getOtherPublicWorkflows(currentOwner()));
    }

    /**
     * List all the published workflows.
     */
    @RequestMapping(value = { "/public/all", "/public/all/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getPublicWorkflows() {
        return prepareResult(service.getPublicWorkflows());
    }

    /**
     * Duplicates a workflow, creating a new one with the same nodes and parameters.
     * @param workflowId    The identifier of the workflow to be duplicated
     */
    @RequestMapping(value = { "/clone", "/clone/" }, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> cloneWorkflow(@RequestParam("workflowId") long workflowId) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        WorkflowDescriptor source = this.service.findById(workflowId);
        try {
            if (source != null ) {
                final WorkflowDescriptor clone = service.clone(source, false);
                clone.setTemporary(false);
                responseEntity = prepareResult(service.update(clone));
                record("Cloned workflow " + workflowId);
            } else {
                responseEntity = prepareResult("Workflow not found", ResponseStatus.FAILED);
            }
        } catch (PersistenceException e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

    /**
     * List all the tags attached to workflows.
     */
    @RequestMapping(value = { "/tags", "/tags/" }, method = RequestMethod.GET)
    public ResponseEntity<ServiceResponse<?>> listTags() {
        List<Tag> objects = service.getWorkflowTags();
        if (objects == null ) {
            objects = new ArrayList<>();
        }
        return prepareResult(objects.stream().map(Tag::getText).collect(Collectors.toList()));
    }

    /**
     * Creates a workflow based on a pre-defined one.
     * @param name  The name of the pre-defined workflow
     */
    @RequestMapping(value = { "/init", "/init/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> initializeSampleWorkflows(@RequestParam(name = "name", required = false) String name) {
        ServiceRegistry<WorkflowBuilder> registry = ServiceRegistryManager.getInstance().getServiceRegistry(WorkflowBuilder.class);
        Set<WorkflowBuilder> services = registry.getServices();
        if (services == null || services.isEmpty()) {
            return prepareResult("No sample workflows found", ResponseStatus.FAILED);
        } else {
            List<WorkflowDescriptor> descriptors = new ArrayList<>();
            BaseException aggregated = null;
            for (WorkflowBuilder sample : services) {
                try {
                    if (name == null || name.isEmpty() || name.equals(sample.getName())) {
                        Logger.getLogger(getClass().getName()).fine(String.format("Creating workflow %s", sample.getName()));
                        WorkflowDescriptor descriptor = sample.createWorkflowDescriptor();
                        if (descriptor != null) {
                            descriptors.add(descriptor);
                        }
                    }
                } catch (Exception e) {
                    if (aggregated == null) {
                        aggregated = new BaseException(e.getMessage());
                    }
                    aggregated.addAdditionalInfo(sample.getName(), e.getMessage());
                }
            }
            return aggregated != null ? handleException(aggregated) : prepareResult(descriptors);
        }
    }

    /**
     * Adds a node to a workflow
     * @param workflowId    The workflow identifier
     * @param node          The new node
     */
    @RequestMapping(value = { "/node", "/node/" }, method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> addNode(@RequestParam("workflowId") long workflowId,
                                                      @RequestBody WorkflowNodeDescriptor node) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            // until UI exposes this, default preserveOutput to true;
            node.setPreserveOutput(true);
            TaoComponent component = componentService.findById(node.getComponentId());
            if(component == null)
                component = conditionComponentService.findById(node.getComponentId());
            if (component != null && (node.getAdditionalInfo() == null || node.getAdditionalInfo().stream().noneMatch(p -> "nodeAffinity".equals(p.getParameterName())))) {
                node.addInfo("nodeAffinity", component.getNodeAffinity() != null? component.getNodeAffinity().getValue() : NodeAffinity.Any.getValue());
            }
            responseEntity = prepareResult(service.addNode(workflowId, node));
            record("Node " + node.getName() + " added to workflow " + workflowId);
        } catch (PersistenceException e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

    /**
     * Creates a group of nodes in a workflow
     * @param request   Structure containing the workflow identifier, the group name and the identifiers of nodes to be
     *                  grouped
     */
    @RequestMapping(value = { "/group", "/group/" }, method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> addGroup(@RequestBody WorkflowGroupNodeRequest request) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            responseEntity = prepareResult(service.group(request.getWorkflowId(), request.getGroupNodeName(), request.getGroupNodeIds()));
            record("Node group " + request.getGroupNodeName() + " added to workflow " + request.getWorkflowId());
        } catch (PersistenceException e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

    /**
     * Removes a group of nodes and replaces it with the contained nodes.
     * @param groupId   The group identifier
     */
    @RequestMapping(value = { "/ungroup", "/ungroup/" }, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> ungroup(@RequestParam("groupId") long groupId) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            WorkflowNodeDescriptor descriptor = workflowNodeProvider.get(groupId);
            if (descriptor == null) {
                throw new IllegalArgumentException("Node does not exist");
            }
            if (!(descriptor instanceof WorkflowNodeGroupDescriptor)) {
                throw new IllegalArgumentException("Node is not a group node");
            }
            final WorkflowNodeGroupDescriptor groupDescriptor = (WorkflowNodeGroupDescriptor) descriptor;
            service.ungroup(groupDescriptor, false);
            responseEntity = prepareResult("Group removed", ResponseStatus.SUCCEEDED);
            record("Group " + groupId + " removed");
        } catch (PersistenceException e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

    /**
     * Inserts a workflow as a subworkflow of another workflow
     * @param workflowId        The identifier of the workflow in which to insert
     * @param subWorkflowId     The identifier of the workflow to be inserted
     * @param keepDataSources   Flag to indicate if to keep or not the sources of the workflow to be inserted
     *
     */
    @RequestMapping(value = { "/subworkflow", "/subworkflow/" }, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> addSubworkflow(@RequestParam("workflowId") long workflowId,
                                                             @RequestParam("subWorkflowId") long subWorkflowId,
                                                             @RequestParam("keepDataSources") boolean keepDataSources) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            WorkflowDescriptor master = this.service.findById(workflowId);
            if (master == null) {
                throw new PersistenceException(String.format("Workflow with identifier %s does not exist",
                                                             workflowId));
            }
            WorkflowDescriptor subGraph = this.service.findById(subWorkflowId);
            if (subGraph == null) {
                throw new PersistenceException(String.format("Workflow with identifier %s does not exist",
                                                             subWorkflowId));
            }
            responseEntity = prepareResult(service.importWorkflowNodes(master, subGraph, keepDataSources));
        } catch (PersistenceException e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

    /**
     * Updates the information of a node
     * @param workflowId    The identifier of the parent workflow
     * @param node          The node to be updated
     */
    @RequestMapping(value = { "/node", "/node/" }, method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> updateNode(@RequestParam("workflowId") long workflowId,
                                                         @RequestBody WorkflowNodeDescriptor node) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            validateValueTypes(node);
            responseEntity = prepareResult(service.updateNode(workflowId, node));
            record("Node " + node.getName() + " in workflow " + workflowId + " modified");
        } catch (Exception e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

    /**
     * Updates several nodes at once.
     * @param workflowId    The identifier of the parent workflow
     * @param nodes         The nodes to be updated
     */
    @RequestMapping(value = { "/nodes", "/nodes/" }, method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> updateNodes(@RequestParam("workflowId") long workflowId,
                                                          @RequestBody List<WorkflowNodeDescriptor> nodes) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            for (WorkflowNodeDescriptor node : nodes) {
                validateValueTypes(node);
            }
            responseEntity = prepareResult(service.updateNodes(workflowId, nodes));
            record("Nodes " + nodes.stream().map(WorkflowNodeDescriptor::getName).collect(Collectors.joining(",")) + " in workflow " + workflowId + " modified");
        } catch (Exception e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

    /**
     * Updates only the positions (coordinates) on the design canvas of one or more nodes.
     * @param workflowId    The identifier of the parent workflow
     * @param positions     The new positions for each node identifier
     */
    @RequestMapping(value = { "/positions", "/positions/" }, method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> updateNodesPositions(@RequestParam("workflowId") long workflowId,
                                                                   @RequestBody Map<Long, float[]> positions) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            service.updateNodesPositions(workflowId, positions);
            responseEntity = prepareResult("Positions updated", ResponseStatus.SUCCEEDED);
        } catch (PersistenceException e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

    /**
     * Removes a node from a workflow
     * @param workflowId    The identifier of the parent workflow
     * @param node          The node to be removed
     */
    @RequestMapping(value = { "/node", "/node/" }, method = RequestMethod.DELETE, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> removeNode(@RequestParam("workflowId") long workflowId,
                                                         @RequestBody WorkflowNodeDescriptor node) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            WorkflowNodeGroupDescriptor groupNode = workflowNodeProvider.getGroupNode(node.getId());
            if (groupNode != null) {
                throw new IllegalArgumentException("Cannot delete a node that is part of a group. Ungroup first.");
            }
            service.removeNode(workflowId, node);
            responseEntity = prepareResult(String.format("Node with id '%s' was deleted", node.getId()),
                                           ResponseStatus.SUCCEEDED);
            record("Node " + node.getName() + " from workflow " + workflowId + " deleted");
        } catch (PersistenceException e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

    /**
     * Creates a link between two nodes
     * @param sourceNodeId      The identifier of the source node (from where the link is coming)
     * @param sourceTargetId    The identifier of the output of the source node
     * @param targetNodeId      The identifier of the target node (where the link is going to)
     * @param targetSourceId    The identifier of the input of the target node
     */
    @RequestMapping(value = { "/link", "/link/" }, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> addLink(@RequestParam("sourceNodeId") long sourceNodeId,
                                                      @RequestParam("sourceTargetId") String sourceTargetId,
                                                      @RequestParam("targetNodeId") long targetNodeId,
                                                      @RequestParam("targetSourceId") String targetSourceId) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            WorkflowNodeDescriptor descriptor = service.addLink(sourceNodeId, sourceTargetId, targetNodeId, targetSourceId);
            if (descriptor != null) {
                responseEntity = prepareResult(descriptor);
                record(String.format("Link between nodes %d(target %s) and %d(source %s) created",
                                     sourceNodeId, sourceTargetId, targetNodeId, targetSourceId));
            } else {
                responseEntity = prepareResult("Could not add link", ResponseStatus.FAILED);
            }
        } catch (Exception e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

    /**
     * Removes a link from a workflow
     * @param nodeId    The identifier of the node in whose incoming links collection the link is
     * @param link      The link to be removed
     */
    @RequestMapping(value = { "/link", "/link/" }, method = RequestMethod.DELETE, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> removeLink(@RequestParam("nodeId") long nodeId,
                                                         @RequestBody ComponentLink link) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            responseEntity = prepareResult(service.removeLink(nodeId, link));
            record(String.format("Link between nodes %d(target %s) and %d(source %s) removed",
                                 link.getSourceNodeId(), link.getInput().getId(),
                                 nodeId, link.getOutput().getId()));
        } catch (PersistenceException e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

    /**
     * Lists the jobs that were executed using this workflow descriptor.
     * @param workflowId    The workflow identifier
     *
     */
    @RequestMapping(value = { "/{workflowId}/executions", "/{workflowId}/executions/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getWorkflowExecutions(@PathVariable("workflowId") long workflowId) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            responseEntity = prepareResult(service.getWorkflowExecutions(workflowId, currentOwner()));
        } catch (PersistenceException e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

    /**
     * Returns the details of a job
     * @param executionId   The job identifier
     *
     */
    @RequestMapping(value = { "/{workflowId}/executions/{executionId}", "/{workflowId}/executions/{executionId}/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getWorkflowExecutionTasks(@PathVariable("executionId") long executionId) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            List<ExecutionTaskInfo> taskInfos = service.getWorkflowExecutionTasks(executionId);
            Map<Long, String> nodeNames = workflowNodeProvider.getNodeNames(taskInfos.stream()
                                                                                     .map(t -> Long.parseLong(t.getWorkflowNodeId()))
                                                                                     .collect(Collectors.toSet()));
            taskInfos.forEach(t -> t.setWorkflowNodeId(nodeNames.get(Long.parseLong(t.getWorkflowNodeId()))));
            responseEntity = prepareResult(taskInfos);
        } catch (PersistenceException e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

    /**
     * Converts a graph done in SNAP to a TAO workflow, provided that TAO has a SNAP container
     * @param graphXml  The SNAP graph
     */
    @RequestMapping(value = { "/import/snap", "/import/snap/" }, method = RequestMethod.POST, consumes = "text/xml", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> importSnapGraph(@RequestBody String graphXml) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            responseEntity = prepareResult(service.snapGraphToWorkflow(graphXml));
        } catch (Exception e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

    /**
     * Import a workflow descriptor from a json file.
     * @param files    The one cwl file or directory containing the workflow descriptor
     *
     */
    @RequestMapping(value = { "/import/fromFiles", "/import/fromFiles/" }, method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ServiceResponse<?>> importWorkflow(@RequestParam(value = "file") List<MultipartFile> files, @RequestParam("importType") String importType) {
        final String userId = currentOwner();
        List<Path> fileList = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                Path targetBase = Paths.get(SystemVariable.CACHE.value());
                Path target = resolveMultiPartFile(file, targetBase);
                fileList.add(target);
            } catch (Exception e) {
                return prepareResult("File upload failed", ResponseStatus.FAILED);
            }
        }
        asyncExecute(() -> {
            try {
                WorkflowDescriptor workflowDescriptor = null;
                if (importType == null || importType.isEmpty()) {
                    Messaging.send(userId, Topic.ERROR.getCategory(),
                            "Empty import type");
                }
                if (fileList.isEmpty()) {
                    Messaging.send(userId, Topic.ERROR.getCategory(),
                            "Empty file list");
                }
                if (importType.toLowerCase().contains("cwl")) {
                    workflowDescriptor = service.importWorkflow(fileList, userId);
                } else {
                    try (InputStream stream = Files.newInputStream(fileList.getFirst())) {
                        workflowDescriptor = service.importWorkflow(stream, userId);
                    } catch (Exception e) {
                        warn(e.getMessage());
                    }
                }
                if (workflowDescriptor != null) {
                    Messaging.send(userId, Topic.INFORMATION.getCategory(),
                            "Workflow was created to the user [" + userId + "] workspace");
                } else {
                    Messaging.send(userId, Topic.INFORMATION.getCategory(),
                            "Unable to create Workflow to the user [" + userId + "] workspace");
                }
            } catch (Exception ex) {
                Logger.getLogger(getClass().getName()).warning(ex.getMessage());
            }
        });
        return prepareResult("CWL import queued", ResponseStatus.SUCCEEDED);
    }

    /**
     * Updates a workflow descriptor identified by its ID or publishes it, depending on the value of
     * the configuration key 'other.workflows.visibility'.
     * If this is set to SUBSCRIPTION, then only an update occurs.
     * If this is set to PUBLIC, and the new workflow status is READY, the workflow should be directly published.
     *
     * @param id the unique identifier of the workflow descriptor to be updated or publishe3d
     * @param entity an instance of {@link WorkflowDescriptor} containing the updated workflow; must not be null
     * @return a {@link ResponseEntity} wrapping the service response indicating the outcome of the operation
     */
    @RequestMapping(value = { "/{id:.+}", "/{id:.+}/" }, method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Override
    public ResponseEntity<ServiceResponse<?>> update(@PathVariable("id") Long id, @RequestBody WorkflowDescriptor entity) {
        if (entity != null && id != null && entity.getStatus() != null) {
            ResponseEntity<ServiceResponse<?>> response = super.update(id, entity);
            Visibility workflowVisibility = getConfiguredPublishedWorkflowVisibility();
            // If the configured visibility is SUBSCRIPTION, the workflow will only be saved.
            // Otherwise, the intermediate status READY should be replaced by PUBLISHED
            return Visibility.PUBLIC == workflowVisibility &&
                   entity.getStatus() == Status.READY &&
                   entity.getVisibility() == Visibility.PUBLIC
                    ? publish(id, null)
                    : response;
        } else {
            return handleException(new IllegalArgumentException("Invalid entity status"));
        }
    }

    /**
     * Marks a workflow as published.
     *
     * @param workflowId The workflow identifier
     *
     */
    @RequestMapping(value = { "/publish", "/publish/" }, method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> publish(@RequestParam("workflowId") long workflowId,
                                                      @RequestParam(name = "userId", required = false) String userId) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            final WorkflowInfo info = service.getWorkflowInfo(workflowId);
            if (info == null) {
                throw new IllegalArgumentException("[workflowId] Invalid argument");
            }
            final String uId = StringUtilities.isNullOrEmpty(userId) ? currentOwner() : userId;
            if (!info.getUserId().equals(userId) && !isCurrentUserAdmin()) {
                return prepareResult("Workflow should be published by the owner", HttpStatus.UNAUTHORIZED);
            }
            if (info.getStatus() != Status.READY) {
                throw new IllegalArgumentException("The workflow status does not allow publication");
            }
            final WorkflowDescriptor descriptor = service.findById(workflowId);
            descriptor.setStatus(Status.PUBLISHED);
            descriptor.setVisibility(getConfiguredPublishedWorkflowVisibility());
            responseEntity = prepareResult(service.update(descriptor));
        } catch (Exception e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

    /**
     * Subscribes a user to a workflow
     * @param userId    The user identifier
     * @param workflowId The workflow identifier
     *
     */
    @RequestMapping(value = { "/subscribe", "/subscribe/" }, method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> subscribe(@RequestParam(name = "userId", required = false) String userId,
                                                        @RequestParam("workflowId") long workflowId) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            if (!StringUtilities.isNullOrEmpty(userId) && !isCurrentUserAdmin() && !currentOwner().equals(userId)) {
                throw new IllegalArgumentException("Operation not allowed");
            }
            final String uId = userId != null ? userId : currentOwner();
            final User user = userProvider.get(uId);
            if (user == null) {
                throw new IllegalArgumentException("[userId] Invalid argument");
            }
            final WorkflowInfo info = service.getWorkflowInfo(workflowId);
            if (info == null) {
                throw new IllegalArgumentException("[workflowId] Invalid argument");
            } else if (info.getStatus() != Status.PUBLISHED || info.getVisibility() != getConfiguredPublishedWorkflowVisibility()) {
                throw new IllegalArgumentException("This workflow cannot be yet subscribed");
            }
            responseEntity = prepareResult(service.subscribeToWorkflow(uId, workflowId));
        } catch (Exception e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

    /**
     * Unsubscribes a user from a workflow
     * @param userId    The user identifier
     * @param workflowId The workflow identifier
     *
     */
    @RequestMapping(value = { "/unsubscribe", "/unsubscribe/" }, method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> unsubscribe(@RequestParam("userId") String userId,
                                                        @RequestParam("workflowId") long workflowId) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            final User user = userProvider.get(userId);
            if (user == null) {
                throw new IllegalArgumentException("[userId] Invalid argument");
            }
            final WorkflowInfo info = service.getWorkflowInfo(workflowId);
            if (info == null) {
                throw new IllegalArgumentException("[workflowId] Invalid argument");
            }
            service.unsubscribeFromWorkflow(userId, workflowId);
            responseEntity = prepareResult("Unsubscribed successfully", ResponseStatus.SUCCEEDED);
        } catch (Exception e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

    /**
     * Export a workflow descriptor as json file.
     * @param id    The workflow identifier
     *
     */
    @RequestMapping(value = { "/{id:.+}/export", "/{id:.+}/export/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void export(@PathVariable("id") long id, HttpServletResponse response) {
        try {
            WorkflowDescriptor entity;
            entity = service.getFullDescriptor(id);
            if (entity == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else {
                response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename="
                        + FileUtilities.ensureValidFileName(entity.getName())
                        + ".json");
                response.setStatus(HttpStatus.OK.value());
                response.getOutputStream().write(service.exportWorkflow(entity).getBytes());
            }
        } catch (Exception e) {
            error(e.getMessage());
        }
    }

    /**
     * Import a workflow descriptor from a json file.
     * @param file    The JSON file containing the workflow descriptor
     *
     */
    @RequestMapping(value = { "/import", "/import/" }, method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ServiceResponse<?>> importFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.getOriginalFilename() == null) {
                throw new IOException("Empty file");
            }
            try (InputStream stream = file.getInputStream()) {
                return prepareResult(service.importWorkflow(stream, currentOwner()));
            }
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * Handles the creation of resources from a set of component ID pairs.
     *
     * @param name the name identifier for the workflow; must not be null or empty.
     * @param pairs a 2D array of pairs representing the links between components;
     *              must not be null or empty.
     * @return a ResponseEntity containing the resulted workflow.
     */
    @RequestMapping(value = { "/create", "/create/" }, method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ServiceResponse<?>> createFromPairs(@RequestParam("name") String name,
                                                              @RequestParam("pairs") String[][] pairs) {
        try {
            if (StringUtilities.isNullOrEmpty(name) || pairs == null || pairs.length == 0) {
                throw new IOException("Empty list");
            }
            return prepareResult(service.createFromPairs(name, pairs));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @Override
    protected Long parseIdentifier(String value) {
        return Long.parseLong(value);
    }

    private void validateValueTypes(WorkflowNodeDescriptor node) throws ValidationException {
        final List<ParameterValue> customValues = node.getCustomValues();
        if (customValues != null) {
            List<String> errors = null;
            final ProcessingComponent component = componentService.findById(node.getComponentId());
            ConditionComponent conditionComponent = component == null ? conditionComponentService.findById(node.getComponentId()) : null;
            Set<ParameterDescriptor> parameters = component != null
                    ? component.getParameterDescriptors()
                    : conditionComponent != null
                        ? conditionComponent.getParameterDescriptors()
                        : new HashSet<>();
            for (Iterator<ParameterValue> it = customValues.iterator(); it.hasNext(); ) {
                ParameterValue customValue = it.next();
                ParameterDescriptor descriptor = null;
                for (ParameterDescriptor componentParam : parameters){
                    if (componentParam.getName().equals(customValue.getParameterName())) {
                        descriptor = componentParam;
                        break;
                    }
                    if (componentParam instanceof TemplateParameterDescriptor){
                        descriptor = ((TemplateParameterDescriptor) componentParam).getParameters()
                                .stream()
                                .filter(innerParam -> customValue.getParameterName()
                                        .equals(componentParam.getName() + "~" + innerParam.getName()))
                                .findFirst().orElse(null);
                        if(descriptor != null)
                            break;
                    }
                }
                if (descriptor != null) {
                    final String value = customValue.getParameterValue();
                    try {
                        if (!descriptor.javaType().isArrayType()) {
                            // Remove empty value
                            if (StringUtilities.isNullOrEmpty(value)) {
                                it.remove();
                            }
                            descriptor.javaType().parse(value);
                        } else {
                            // Remove empty array
                            if (StringUtilities.isNullOrEmpty(value) || "[]".equals(value)) {
                                it.remove();
                            }
                            descriptor.javaType().parseArray(value);
                        }
                    } catch (Exception e) {
                        if (errors == null) {
                            errors = new ArrayList<>();
                        }
                        errors.add(String.format("[%s.%s] %s is not of expected type (%s)",
                                                 node.getName(),
                                                 customValue.getParameterName(),
                                                 value,
                                                 descriptor.javaType().friendlyName()));
                    }
                }
            }
            if (errors != null) {
                throw new ValidationException(String.join("\n", errors));
            }
        }
    }

    private Visibility getConfiguredPublishedWorkflowVisibility() {
        final Visibility visibility;
        final String value = ConfigurationManager.getInstance().getValue("other.workflows.visibility");
        if (value != null && Enum.valueOf(Visibility.class, value.toUpperCase()).equals(Visibility.SUBSCRIPTION)) {
            visibility = Visibility.SUBSCRIPTION;
        } else {
            visibility = Visibility.PUBLIC;
        }
        return visibility;
    }

    private Path resolveMultiPartFile(MultipartFile file, Path targetBase) throws IOException {
        if (file == null) {
            throw new IOException("Failed to store empty file");
        }
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        if (fileName.contains("..")) {
            // This is a security check
            throw new IOException( "Cannot store docker file with relative path outside image directory " + fileName);
        }
        FileUtilities.createDirectories(targetBase);
        Path destination = targetBase.resolve(fileName);
        FileUtilities.createDirectories(destination.getParent());
        file.transferTo(destination.toAbsolutePath().toFile());
        return destination;
    }
}

