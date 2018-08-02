/*
 * Copyright (C) 2017 CS ROMANIA
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.component.ComponentLink;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.eodata.enums.Visibility;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.services.commons.ServiceError;
import ro.cs.tao.services.entity.demo.SampleWorkflows;
import ro.cs.tao.services.interfaces.ComponentService;
import ro.cs.tao.services.interfaces.ContainerService;
import ro.cs.tao.services.interfaces.GroupComponentService;
import ro.cs.tao.services.interfaces.WorkflowService;
import ro.cs.tao.workflow.WorkflowDescriptor;
import ro.cs.tao.workflow.WorkflowNodeDescriptor;
import ro.cs.tao.workflow.enums.Status;

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
    public ResponseEntity<?> getUserWorkflowsByStatus(@PathVariable("status") Status status) {
        return new ResponseEntity<>(service.getUserWorkflowsByStatus(currentUser(), status),
                                    HttpStatus.OK);
    }

    @RequestMapping(value = "/visibility/{visibility}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> getUserWorkflowsByVisibility(@PathVariable("visibility")Visibility visibility) {
        return new ResponseEntity<>(service.getUserPublishedWorkflowsByVisibility(currentUser(), visibility),
                                    HttpStatus.OK);
    }

    @RequestMapping(value = "/public", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> getOtherPublicWorkflows() {
        return new ResponseEntity<>(service.getOtherPublicWorkflows(currentUser()),
                                    HttpStatus.OK);
    }

    @RequestMapping(value = "/clone", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<?> cloneWorkflow(@RequestParam("workflowId") long workflowId) {
        ResponseEntity<?> responseEntity;
        WorkflowDescriptor source = persistenceManager.getWorkflowDescriptor(workflowId);
        try {
            responseEntity = new ResponseEntity<>(source != null ?
                                                          service.clone(source) :
                                                          new ServiceError("No such workflow"),
                                                  HttpStatus.OK);
        } catch (PersistenceException e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

    @RequestMapping(value = "/init", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> initialize(@RequestParam("otbContainer") String otbContainerName,
                                        @RequestParam("otbPath") String otbPath,
                                        @RequestParam("snapContainer") String snapContainerName,
                                        @RequestParam("snapPath") String snapPath) throws PersistenceException {
        SampleWorkflows.setComponentService(componentService);
        SampleWorkflows.setWorkflowService(service);
        DataSourceComponent dataSourceComponent = persistenceManager.getDataSourceInstance("Sentinel2-Amazon Web Services");
        WorkflowDescriptor descriptor1 = SampleWorkflows.initWorkflow1();
        WorkflowDescriptor descriptor2 = SampleWorkflows.initWorkflow2();
        WorkflowDescriptor descriptor3 = SampleWorkflows.initWorkflow3();
        WorkflowDescriptor descriptor4 = SampleWorkflows.initWorkflow4();
        WorkflowDescriptor descriptor5 = SampleWorkflows.initWorkflow5(dataSourceComponent);
        WorkflowDescriptor descriptor6 = SampleWorkflows.initWorkflow6(dataSourceComponent);
        return new ResponseEntity<>(new WorkflowDescriptor[]
                { descriptor1, descriptor2, descriptor3, descriptor4, descriptor5, descriptor6 },
                HttpStatus.OK);
    }

    @RequestMapping(value = "/node", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<?> addNode(@RequestParam("workflowId") long workflowId,
                                    @RequestBody WorkflowNodeDescriptor node) {
        ResponseEntity<?> responseEntity;
        try {
            responseEntity = new ResponseEntity<>(service.addNode(workflowId, node),
                                                  HttpStatus.OK);
        } catch (PersistenceException e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

    @RequestMapping(value = "/node", method = RequestMethod.PUT, produces = "application/json")
    public ResponseEntity<?> updateNode(@RequestParam("workflowId") long workflowId,
                                     @RequestBody WorkflowNodeDescriptor node) {
        ResponseEntity<?> responseEntity;
        try {
            responseEntity = new ResponseEntity<>(service.updateNode(workflowId, node),
                                                  HttpStatus.OK);
        } catch (PersistenceException e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

    @RequestMapping(value = "/node", method = RequestMethod.DELETE, produces = "application/json")
    public ResponseEntity<?> removeNode(@RequestParam("workflowId") long workflowId,
                                        @RequestBody WorkflowNodeDescriptor node) {
        ResponseEntity<?> responseEntity;
        try {
            service.removeNode(workflowId, node);
            responseEntity = new ResponseEntity<>("OK", HttpStatus.OK);
        } catch (PersistenceException e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

    @RequestMapping(value = "/link", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<?> addLink(@RequestParam("sourceNodeId") long sourceNodeId,
                                     @RequestParam("sourceTargetId") String sourceTargetId,
                                     @RequestParam("targetNodeId") long targetNodeId,
                                     @RequestParam("targetSourceId") String targetSourceId) {
        ResponseEntity<?> responseEntity;
        try {
            WorkflowNodeDescriptor descriptor = service.addLink(sourceNodeId, sourceTargetId, targetNodeId, targetSourceId);
            if (descriptor != null) {
                responseEntity = new ResponseEntity<>(descriptor, HttpStatus.OK);
            } else {
                responseEntity = new ResponseEntity<>(new ServiceError("Could not save link"), HttpStatus.OK);
            }
        } catch (Exception e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

    @RequestMapping(value = "/link", method = RequestMethod.DELETE, produces = "application/json")
    public ResponseEntity<?> removeLink(@RequestParam("nodeId") long nodeId,
                                        @RequestBody ComponentLink link) {
        ResponseEntity<?> responseEntity;
        try {
            responseEntity = new ResponseEntity<>(service.removeLink(nodeId, link), HttpStatus.OK);
        } catch (PersistenceException e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

    @RequestMapping(value = "/{workflowId}/executions", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> getWorkflowExecutions(@PathVariable("workflowId") long workflowId) {
        ResponseEntity<?> responseEntity;
        try {
            responseEntity = new ResponseEntity<>(service.getWorkflowExecutions(workflowId),
                                                  HttpStatus.OK);
        } catch (PersistenceException e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

    @RequestMapping(value = "/{workflowId}/executions/{executionId}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> getWorkflowExecutionTasks(@PathVariable("executionId") long executionId) {
        ResponseEntity<?> responseEntity;
        try {
            responseEntity = new ResponseEntity<>(service.getWorkflowExecutionTasks(executionId),
                                                  HttpStatus.OK);
        } catch (PersistenceException e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

}
