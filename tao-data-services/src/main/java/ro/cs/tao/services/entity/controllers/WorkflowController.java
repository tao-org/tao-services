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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.BaseException;
import ro.cs.tao.component.ComponentLink;
import ro.cs.tao.datasource.beans.Parameter;
import ro.cs.tao.eodata.enums.Visibility;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.services.base.SampleWorkflowBase;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.*;
import ro.cs.tao.spi.ServiceRegistry;
import ro.cs.tao.spi.ServiceRegistryManager;
import ro.cs.tao.workflow.WorkflowDescriptor;
import ro.cs.tao.workflow.WorkflowNodeDescriptor;
import ro.cs.tao.workflow.enums.Status;

import java.io.IOException;
import java.util.*;

/**
 * @author Cosmin Cara
 */
@Controller
@RequestMapping("/workflow")
public class WorkflowController extends DataEntityController<WorkflowDescriptor, WorkflowService> {

    @Autowired
    private ContainerService containerService;

    @Autowired
    private ComponentService componentService;

    @Autowired
    private GroupComponentService groupComponentService;

    @RequestMapping(value = "/status/{status}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getUserWorkflowsByStatus(@PathVariable("status") Status status) {
        return prepareResult(service.getUserWorkflowsByStatus(currentUser(), status));
    }

    @RequestMapping(value = "/visibility/{visibility}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getUserWorkflowsByVisibility(@PathVariable("visibility")Visibility visibility) {
        return prepareResult(service.getUserPublishedWorkflowsByVisibility(currentUser(), visibility));
    }

    @RequestMapping(value = "/public", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getOtherPublicWorkflows() {
        return prepareResult(service.getOtherPublicWorkflows(currentUser()));
    }

    @RequestMapping(value = "/clone", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> cloneWorkflow(@RequestParam("workflowId") long workflowId) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        WorkflowDescriptor source = getPersistenceManager().getWorkflowDescriptor(workflowId);
        try {
            if (source != null ) {
                responseEntity = prepareResult(service.clone(source));
            } else {
                responseEntity = prepareResult("Workflow not found", ResponseStatus.FAILED);
            }
        } catch (PersistenceException e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

    @RequestMapping(value = "/mock", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getMockWorkflows() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        Hibernate5Module hibernate5Module = new Hibernate5Module();
        hibernate5Module.disable(Hibernate5Module.Feature.USE_TRANSIENT_ANNOTATION);
        hibernate5Module.enable(Hibernate5Module.Feature.FORCE_LAZY_LOADING);
        objectMapper.registerModule(hibernate5Module);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        ObjectReader reader = objectMapper.reader().forType(WorkflowDescriptor.class);
        WorkflowDescriptor descriptor1 = reader.readValue(Mock.WF1);
        WorkflowDescriptor descriptor2 = reader.readValue(Mock.WF2);
        List<WorkflowDescriptor> descriptors = new ArrayList<>();
        descriptors.add(descriptor1);
        descriptors.add(descriptor2);
        return prepareResult(descriptors);
    }

    @RequestMapping(value = "/mock", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getMockWorkflow(@RequestParam("id") long mockId) throws IOException {
        Map<String, List<Parameter>> params = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        Hibernate5Module hibernate5Module = new Hibernate5Module();
        hibernate5Module.disable(Hibernate5Module.Feature.USE_TRANSIENT_ANNOTATION);
        hibernate5Module.enable(Hibernate5Module.Feature.FORCE_LAZY_LOADING);
        objectMapper.registerModule(hibernate5Module);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        ObjectReader reader = objectMapper.reader().forType(params.getClass());
        if (mockId == 1) {
            params = reader.readValue(Mock.WF1_Params);
        }
        if (mockId == 6) {
            params = reader.readValue(Mock.WF2_Params);
        }
        return prepareResult(params);
    }

    @RequestMapping(value = "/init", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> initializeSampleWorkflows() {
        ServiceRegistry<SampleWorkflow> registry = ServiceRegistryManager.getInstance().getServiceRegistry(SampleWorkflow.class);
        Set<SampleWorkflow> services = registry.getServices();
        if (services == null || services.size() == 0) {
            return prepareResult("No sample workflows found", ResponseStatus.FAILED);
        } else {
            SampleWorkflowBase.setPersistenceManager(getPersistenceManager());
            SampleWorkflowBase.setComponentService(componentService);
            SampleWorkflowBase.setWorkflowService(service);
            List<WorkflowDescriptor> descriptors = new ArrayList<>();
            BaseException aggregated = null;
            for (SampleWorkflow sample : services) {
                try {
                    descriptors.add(sample.createWorkflowDescriptor());
                } catch (PersistenceException e) {
                    if (aggregated == null) {
                        aggregated = new BaseException(e.getMessage());
                    }
                    aggregated.addAdditionalInfo(sample.getName(), e.getMessage());
                }
            }
            return aggregated != null ? handleException(aggregated) : prepareResult(descriptors);
        }
    }

    @RequestMapping(value = "/node", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> addNode(@RequestParam("workflowId") long workflowId,
                                                      @RequestBody WorkflowNodeDescriptor node) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            responseEntity = prepareResult(service.addNode(workflowId, node));
        } catch (PersistenceException e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

    @RequestMapping(value = "/subworkflow", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> addSubworkflow(@RequestParam("workflowId") long workflowId,
                                                             @RequestParam("subWorkflowId") long subWorkflowId,
                                                             @RequestParam("keepDataSources") boolean keepDataSources) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            WorkflowDescriptor master = getPersistenceManager().getWorkflowDescriptor(workflowId);
            if (master == null) {
                throw new PersistenceException(String.format("Workflow with identifier %s does not exist",
                                                             workflowId));
            }
            WorkflowDescriptor subGraph = getPersistenceManager().getWorkflowDescriptor(subWorkflowId);
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

    @RequestMapping(value = "/node", method = RequestMethod.PUT, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> updateNode(@RequestParam("workflowId") long workflowId,
                                     @RequestBody WorkflowNodeDescriptor node) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            responseEntity = prepareResult(service.updateNode(workflowId, node));
        } catch (PersistenceException e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

    @RequestMapping(value = "/node", method = RequestMethod.DELETE, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> removeNode(@RequestParam("workflowId") long workflowId,
                                        @RequestBody WorkflowNodeDescriptor node) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            service.removeNode(workflowId, node);
            responseEntity = prepareResult(String.format("Node with id '%s' was deleted", node.getId()),
                                           ResponseStatus.SUCCEEDED);
        } catch (PersistenceException e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

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
            } else {
                responseEntity = prepareResult("Could not add link", ResponseStatus.FAILED);
            }
        } catch (Exception e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

    @RequestMapping(value = "/link", method = RequestMethod.DELETE, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> removeLink(@RequestParam("nodeId") long nodeId,
                                                         @RequestBody ComponentLink link) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            responseEntity = prepareResult(service.removeLink(nodeId, link));
        } catch (PersistenceException e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

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

}
