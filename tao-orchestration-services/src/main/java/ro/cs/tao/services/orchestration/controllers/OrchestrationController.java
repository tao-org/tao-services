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

package ro.cs.tao.services.orchestration.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ro.cs.tao.execution.ExecutionException;
import ro.cs.tao.execution.model.ExecutionJobSummary;
import ro.cs.tao.execution.model.ExecutionTaskSummary;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.OrchestratorService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/orchestrator")
public class OrchestrationController extends BaseController {

    @Autowired
    private OrchestratorService orchestrationService;

    @RequestMapping(value = "/start/{id}", method = RequestMethod.POST)
    public ResponseEntity<ServiceResponse<?>> start(@PathVariable("id") long workflowId,
                                                    @RequestBody Map<String, Map<String, String>> input) {
        ResponseEntity<ServiceResponse<?>> response;
        try {
            response = prepareResult(orchestrationService.startWorkflow(workflowId, input));
        } catch (ExecutionException ex) {
            response = handleException(ex);
        }
        return response;
    }

    @RequestMapping(value = "/start/parameters/{id}", method = RequestMethod.GET)
    public ResponseEntity<ServiceResponse<?>> start(@PathVariable("id") long workflowId) {
        try {
            return prepareResult(orchestrationService.getWorkflowParameters(workflowId));
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    @RequestMapping(value = "/stop/{id}", method = RequestMethod.GET)
    public ResponseEntity<ServiceResponse<?>> stop(@PathVariable("id") long workflowId) {
        ResponseEntity<ServiceResponse<?>> response;
        try {
            orchestrationService.stopWorkflow(workflowId);
            response = prepareResult("Execution stopped", ResponseStatus.SUCCEEDED);
        } catch (ExecutionException ex) {
            response = handleException(ex);
        }
        return response;
    }

    @RequestMapping(value = "/pause/{id}", method = RequestMethod.GET)
    public ResponseEntity<ServiceResponse<?>> pause(@PathVariable("id") long workflowId) {
        ResponseEntity<ServiceResponse<?>> response;
        try {
            orchestrationService.pauseWorkflow(workflowId);
            response = prepareResult("Execution suspended", ResponseStatus.SUCCEEDED);
        } catch (ExecutionException ex) {
            response = handleException(ex);
        }
        return response;
    }

    @RequestMapping(value = "/resume/{id}", method = RequestMethod.GET)
    public ResponseEntity<ServiceResponse<?>> resume(@PathVariable("id") long workflowId) {
        ResponseEntity<ServiceResponse<?>> response;
        try {
            orchestrationService.resumeWorkflow(workflowId);
            response = prepareResult("Execution resumed", ResponseStatus.SUCCEEDED);
        } catch (ExecutionException ex) {
            response = handleException(ex);
        }
        return response;
    }

    @RequestMapping(value = "/running/tasks", method = RequestMethod.GET)
    public ResponseEntity<ServiceResponse<?>> getRunningTasks() {
        List<ExecutionTaskSummary> tasks = orchestrationService.getRunningTasks();
        if (tasks == null) {
            tasks = new ArrayList<>();
        }
        return prepareResult(tasks);
    }

    @RequestMapping(value = "/running/jobs", method = RequestMethod.GET)
    public ResponseEntity<ServiceResponse<?>> getRunningJobs() {
        List<ExecutionJobSummary> summaries = orchestrationService.getRunningJobs();
        if (summaries == null) {
            summaries = new ArrayList<>();
        }
        return prepareResult(summaries);
    }

    @RequestMapping(value = "/{jobId}", method = RequestMethod.GET)
    public ResponseEntity<ServiceResponse<?>> getJobTaskStatuses(@PathVariable("jobId") long jobId) {
        List<ExecutionTaskSummary> summaries = orchestrationService.getTasksStatus(jobId);
        if (summaries == null) {
            summaries = new ArrayList<>();
        }
        return prepareResult(summaries);
    }

    @RequestMapping(value = "/history", method = RequestMethod.GET)
    public ResponseEntity<ServiceResponse<?>> getJobsHistory() {
        List<ExecutionJobSummary> summaries = orchestrationService.getCompletedJobs();
        if (summaries == null) {
            summaries = new ArrayList<>();
        }
        return prepareResult(summaries);
    }
}
