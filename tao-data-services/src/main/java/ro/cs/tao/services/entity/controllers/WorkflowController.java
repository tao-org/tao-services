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
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.BaseException;
import ro.cs.tao.Sort;
import ro.cs.tao.SortDirection;
import ro.cs.tao.Tag;
import ro.cs.tao.component.ComponentLink;
import ro.cs.tao.component.ProcessingComponent;
import ro.cs.tao.eodata.enums.Visibility;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.WorkflowNodeProvider;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.entity.beans.WorkflowGroupNodeRequest;
import ro.cs.tao.services.interfaces.ComponentService;
import ro.cs.tao.services.interfaces.DataSourceComponentService;
import ro.cs.tao.services.interfaces.WorkflowBuilder;
import ro.cs.tao.services.interfaces.WorkflowService;
import ro.cs.tao.spi.ServiceRegistry;
import ro.cs.tao.spi.ServiceRegistryManager;
import ro.cs.tao.workflow.WorkflowDescriptor;
import ro.cs.tao.workflow.WorkflowNodeDescriptor;
import ro.cs.tao.workflow.WorkflowNodeGroupDescriptor;
import ro.cs.tao.workflow.enums.Status;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Cosmin Cara
 */
@RestController
@RequestMapping("/workflow")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Workflows", description = "Operations related to workflows")
public class WorkflowController extends DataEntityController<WorkflowDescriptor, Long, WorkflowService> {

    @Autowired
    private ComponentService componentService;

    @Autowired
    private DataSourceComponentService dataSourceComponentService;

    @Autowired
    private WorkflowNodeProvider workflowNodeProvider;

    /**
     * Lists the workflows that are visible to the current user, optionally specifying the pagination of results.
     *
     * @param pageNumber    (optional) The page number
     * @param pageSize      (optional) The page size
     * @param sortByField   (optional) The field to sort on
     * @param sortDirection (optional) The sort direction
     */
    @Override
    @RequestMapping(value = "/", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> list(@RequestParam(name = "pageNumber", required = false) Optional<Integer> pageNumber,
                                                   @RequestParam(name = "pageSize", required = false) Optional<Integer> pageSize,
                                                   @RequestParam(name = "sortBy", required = false) Optional<String> sortByField,
                                                   @RequestParam(name = "sortDirection", required = false) Optional<SortDirection> sortDirection) {
        if (pageNumber.isPresent() && sortByField.isPresent()) {
            Sort sort = new Sort().withField(sortByField.get(), sortDirection.orElse(SortDirection.ASC));
            return prepareResult(service.list(pageNumber, pageSize, sort));
        } else {
            return prepareResult(service.getUserVisibleWorkflows(currentUser()));
        }
    }

    /**
     * Returns the descriptor of a workflow
     * @param id    The workflow identifier
     */
    @Override
    @RequestMapping(value = "/{id:.+}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> get(@PathVariable("id") Long id) {
        ResponseEntity<ServiceResponse<?>> response;
        WorkflowDescriptor entity = service.getFullDescriptor(id);
        if (entity == null) {
            response = prepareResult(String.format("Entity [%s] not found", id), ResponseStatus.FAILED);
        } else {
            entity.getNodes().forEach(n -> {
                if (n.getComponentId() != null) {
                    ProcessingComponent component = componentService.findById(n.getComponentId());
                    if (component != null
                            && (n.getAdditionalInfo() == null || n.getAdditionalInfo().stream().noneMatch(p -> "nodeAffinity".equals(p.getParameterName())))) {
                        n.addInfo("nodeAffinity", component.getNodeAffinity());
                    }
                }
            });
            response = prepareResult(entity);
        }
        return response;
    }

    /**
     * Lists all the user workflows having a given status.
     * @param status    The workflow status (@see {@link Status})
     */
    @RequestMapping(value = "/status/{status}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getUserWorkflowsByStatus(@PathVariable("status") Status status) {
        return prepareResult(service.getUserWorkflowsByStatus(currentUser(), status));
    }

    /**
     * Lists all the published user workflows by visibility
     * @param visibility    The workflow visibiliti (@see {@link Visibility})
     *
     */
    @RequestMapping(value = "/visibility/{visibility}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getUserWorkflowsByVisibility(@PathVariable("visibility") Visibility visibility) {
        return prepareResult(service.getUserPublishedWorkflowsByVisibility(currentUser(), visibility));
    }

    /**
     * List all the workflows published by users other than the current user.
     */
    @RequestMapping(value = "/public", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getOtherPublicWorkflows() {
        return prepareResult(service.getOtherPublicWorkflows(currentUser()));
    }

    /**
     * List all the published workflows.
     */
    @RequestMapping(value = "/public/all", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getPublicWorkflows() {
        return prepareResult(service.getPublicWorkflows());
    }

    /**
     * Duplicates a workflow, creating a new one with the same nodes and parameters.
     * @param workflowId    The identifier of the workflow to be duplicated
     */
    @RequestMapping(value = "/clone", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> cloneWorkflow(@RequestParam("workflowId") long workflowId) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        WorkflowDescriptor source = this.service.findById(workflowId);
        try {
            if (source != null ) {
                responseEntity = prepareResult(service.clone(source));
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
    @RequestMapping(value = "/tags", method = RequestMethod.GET)
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
    @RequestMapping(value = "/init", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> initializeSampleWorkflows(@RequestParam(name = "name", required = false) String name) {
        ServiceRegistry<WorkflowBuilder> registry = ServiceRegistryManager.getInstance().getServiceRegistry(WorkflowBuilder.class);
        Set<WorkflowBuilder> services = registry.getServices();
        if (services == null || services.size() == 0) {
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
    @RequestMapping(value = "/node", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> addNode(@RequestParam("workflowId") long workflowId,
                                                      @RequestBody WorkflowNodeDescriptor node) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            // until UI exposes this, default preserveOutput to true;
            node.setPreserveOutput(true);
            final ProcessingComponent component = componentService.findById(node.getComponentId());
            if (component != null && (node.getAdditionalInfo() == null || node.getAdditionalInfo().stream().noneMatch(p -> "nodeAffinity".equals(p.getParameterName())))) {
                node.addInfo("nodeAffinity", component.getNodeAffinity());
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
    @RequestMapping(value = "/group", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
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
    @RequestMapping(value = "/ungroup", method = RequestMethod.POST, produces = "application/json")
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
    @RequestMapping(value = "/subworkflow", method = RequestMethod.POST, produces = "application/json")
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
    @RequestMapping(value = "/node", method = RequestMethod.PUT, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> updateNode(@RequestParam("workflowId") long workflowId,
                                                         @RequestBody WorkflowNodeDescriptor node) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            responseEntity = prepareResult(service.updateNode(workflowId, node));
            record("Node " + node + " in workflow " + workflowId + " modified");
        } catch (PersistenceException e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

    /**
     * Updates several nodes at once.
     * @param workflowId    The identifier of the parent workflow
     * @param nodes         The nodes to be updated
     */
    @RequestMapping(value = "/nodes", method = RequestMethod.PUT, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> updateNodes(@RequestParam("workflowId") long workflowId,
                                                          @RequestBody List<WorkflowNodeDescriptor> nodes) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            responseEntity = prepareResult(service.updateNodes(workflowId, nodes));
            record("Nodes " + nodes.stream().map(WorkflowNodeDescriptor::getName).collect(Collectors.joining(",")) + " in workflow " + workflowId + " modified");
        } catch (PersistenceException e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

    /**
     * Updates only the positions (coordinates) on the design canvas of one or more nodes.
     * @param workflowId    The identifier of the parent workflow
     * @param positions     The new positions for each node identifier
     */
    @RequestMapping(value = "/positions", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
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
    @RequestMapping(value = "/node", method = RequestMethod.DELETE, consumes = "application/json", produces = "application/json")
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
    @RequestMapping(value = "/link", method = RequestMethod.POST, produces = "application/json")
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
    @RequestMapping(value = "/link", method = RequestMethod.DELETE, consumes = "application/json", produces = "application/json")
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
    @RequestMapping(value = "/{workflowId}/executions", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getWorkflowExecutions(@PathVariable("workflowId") long workflowId) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            responseEntity = prepareResult(service.getWorkflowExecutions(workflowId));
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
    @RequestMapping(value = "/{workflowId}/executions/{executionId}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getWorkflowExecutionTasks(@PathVariable("executionId") long executionId) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            responseEntity = prepareResult(service.getWorkflowExecutionTasks(executionId));
        } catch (PersistenceException e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

    /**
     * Converts a graph done in SNAP to a TAO workflow, provided that TAO has a SNAP container
     * @param graphXml  The SNAP graph
     */
    @RequestMapping(value = "/import/snap", method = RequestMethod.POST, consumes = "text/xml", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> importSnapGraph(@RequestBody String graphXml) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            responseEntity = prepareResult(service.snapGraphToWorkflow(graphXml));
        } catch (Exception e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

}
