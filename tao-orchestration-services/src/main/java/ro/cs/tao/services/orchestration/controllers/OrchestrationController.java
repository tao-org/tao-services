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

package ro.cs.tao.services.orchestration.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ro.cs.tao.execution.ExecutionException;
import ro.cs.tao.execution.model.ExecutionTask;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.interfaces.OrchestratorService;
import ro.cs.tao.services.orchestration.beans.ServiceTask;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/orchestrator")
public class OrchestrationController extends BaseController {

    @Autowired
    private OrchestratorService orchestrationService;

    @RequestMapping(value = "/start/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> start(@PathVariable("id") long workflowId) {
        try {
            orchestrationService.startWorkflow(workflowId);
            return new ResponseEntity<>("Execution started", HttpStatus.OK);
        } catch (ExecutionException ex) {
            return new ResponseEntity<>(String.format("Execution cannot be started: %s", ex.getMessage()),
                                        HttpStatus.OK);
        }
    }
    @RequestMapping(value = "/stop/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> stop(@PathVariable("id") long workflowId) {
        try {
            orchestrationService.stopWorkflow(workflowId);
            return new ResponseEntity<>("Execution stopped", HttpStatus.OK);
        } catch (ExecutionException ex) {
            return new ResponseEntity<>(String.format("Execution cannot be stopped: %s", ex.getMessage()),
                    HttpStatus.OK);
        }
    }

    @RequestMapping(value = "/pause/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> pause(@PathVariable("id") long workflowId) {
        try {
            orchestrationService.startWorkflow(workflowId);
            return new ResponseEntity<>("Execution suspended", HttpStatus.OK);
        } catch (ExecutionException ex) {
            return new ResponseEntity<>(String.format("Execution cannot be suspended: %s", ex.getMessage()),
                    HttpStatus.OK);
        }
    }

    @RequestMapping(value = "/resume/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> resume(@PathVariable("id") long workflowId) {
        try {
            orchestrationService.startWorkflow(workflowId);
            return new ResponseEntity<>("Execution resumed", HttpStatus.OK);
        } catch (ExecutionException ex) {
            return new ResponseEntity<>(String.format("Execution cannot be resumed: %s", ex.getMessage()),
                    HttpStatus.OK);
        }
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ResponseEntity<List<ServiceTask>> getRunningTasks() {
        List<ExecutionTask> tasks = orchestrationService.getRunningTasks();
        if (tasks == null) {
            tasks = new ArrayList<>();
        }
        return new ResponseEntity<>(tasks.stream().map(t ->
                                            new ServiceTask(t.getId(), t.getWorkflowNodeId(),
                                                            t.getResourceId(), t.getExecutionNodeHostName(),
                                                            t.getStartTime())).collect(Collectors.toList()),
                                    HttpStatus.OK);
    }
}
