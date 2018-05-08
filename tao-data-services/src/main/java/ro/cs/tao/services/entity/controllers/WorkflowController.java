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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import ro.cs.tao.component.ComponentLink;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.services.commons.ServiceError;
import ro.cs.tao.services.entity.demo.WorkflowDemo;
import ro.cs.tao.services.interfaces.ComponentService;
import ro.cs.tao.services.interfaces.ContainerService;
import ro.cs.tao.services.interfaces.GroupComponentService;
import ro.cs.tao.services.interfaces.WorkflowService;
import ro.cs.tao.workflow.WorkflowDescriptor;
import ro.cs.tao.workflow.WorkflowNodeDescriptor;

import java.util.logging.Logger;

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

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private PersistenceManager persistenceManager;

    @RequestMapping(value = "/init", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> initialize(@RequestParam("otbContainer") String otbContainerName,
                                        @RequestParam("otbPath") String otbPath,
                                        @RequestParam("snapContainer") String snapContainerName,
                                        @RequestParam("snapPath") String snapPath) throws PersistenceException {
        WorkflowDemo.setPersistenceManager(persistenceManager);
        WorkflowDemo.setContainerService(containerService);
        WorkflowDemo.setComponentService(componentService);
        WorkflowDemo.setWorkflowService(workflowService);
        WorkflowDemo.initComponents(otbContainerName, otbPath, snapContainerName, snapPath);
        DataSourceComponent dataSourceComponent = WorkflowDemo.initDataSourceComponent();
        WorkflowDescriptor descriptor1 = WorkflowDemo.initWorkflow1();
        WorkflowDescriptor descriptor2 = WorkflowDemo.initWorkflow2();
        WorkflowDescriptor descriptor3 = WorkflowDemo.initWorkflow3();
        WorkflowDescriptor descriptor4 = WorkflowDemo.initWorkflow4();
        WorkflowDescriptor descriptor5 = WorkflowDemo.initWorkflow5(dataSourceComponent);
        WorkflowDescriptor descriptor6 = WorkflowDemo.initWorkflow6(dataSourceComponent);
        return new ResponseEntity<>(new WorkflowDescriptor[]
                { descriptor1, descriptor2, descriptor3, descriptor4, descriptor5, descriptor6 },
                HttpStatus.OK);
    }

    @RequestMapping(value = "/node", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<?> addNode(@RequestParam("workflowId") long workflowId,
                                    @RequestBody WorkflowNodeDescriptor node) {
        ResponseEntity<?> responseEntity;
        try {
            responseEntity = new ResponseEntity<>(workflowService.addNode(workflowId, node),
                                                  HttpStatus.OK);
        } catch (PersistenceException e) {
            Logger.getLogger(WorkflowController.class.getName()).severe(e.getMessage());
            responseEntity = new ResponseEntity<>(new ServiceError(e.getMessage()), HttpStatus.OK);
        }
        return responseEntity;
    }

    @RequestMapping(value = "/node", method = RequestMethod.PUT, produces = "application/json")
    public ResponseEntity<?> updateNode(@RequestParam("workflowId") long workflowId,
                                     @RequestBody WorkflowNodeDescriptor node) {
        ResponseEntity<?> responseEntity;
        try {
            responseEntity = new ResponseEntity<>(workflowService.updateNode(workflowId, node),
                                                  HttpStatus.OK);
        } catch (PersistenceException e) {
            Logger.getLogger(WorkflowController.class.getName()).severe(e.getMessage());
            responseEntity = new ResponseEntity<>(new ServiceError(e.getMessage()), HttpStatus.OK);
        }
        return responseEntity;
    }

    @RequestMapping(value = "/node", method = RequestMethod.DELETE, produces = "application/json")
    public ResponseEntity<?> removeNode(@RequestParam("workflowId") long workflowId,
                                        @RequestBody WorkflowNodeDescriptor node) {
        ResponseEntity<?> responseEntity;
        try {
            responseEntity = new ResponseEntity<>(workflowService.removeNode(workflowId, node),
                                                  HttpStatus.OK);
        } catch (PersistenceException e) {
            Logger.getLogger(WorkflowController.class.getName()).severe(e.getMessage());
            responseEntity = new ResponseEntity<>(new ServiceError(e.getMessage()), HttpStatus.OK);
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
            responseEntity = new ResponseEntity<>(workflowService.addLink(sourceNodeId, sourceTargetId,
                                                                          targetNodeId, targetSourceId),
                                                  HttpStatus.OK);
        } catch (PersistenceException e) {
            Logger.getLogger(WorkflowController.class.getName()).severe(e.getMessage());
            responseEntity = new ResponseEntity<>(new ServiceError(e.getMessage()), HttpStatus.OK);
        }
        return responseEntity;
    }

    @RequestMapping(value = "/link", method = RequestMethod.DELETE, produces = "application/json")
    public ResponseEntity<?> removeLink(@RequestParam("nodeId") long nodeId,
                                        @RequestBody ComponentLink link) {
        ResponseEntity<?> responseEntity;
        try {
            responseEntity = new ResponseEntity<>(workflowService.removeLink(nodeId, link), HttpStatus.OK);
        } catch (PersistenceException e) {
            Logger.getLogger(WorkflowController.class.getName()).severe(e.getMessage());
            responseEntity = new ResponseEntity<>(new ServiceError(e.getMessage()), HttpStatus.OK);
        }
        return responseEntity;
    }


}
