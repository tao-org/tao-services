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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.EnumUtils;
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.execution.ExecutionException;
import ro.cs.tao.execution.callback.EndpointDescriptor;
import ro.cs.tao.execution.model.ExecutionJob;
import ro.cs.tao.execution.model.ExecutionJobSummary;
import ro.cs.tao.execution.model.ExecutionStatus;
import ro.cs.tao.execution.model.ExecutionTaskSummary;
import ro.cs.tao.messaging.Message;
import ro.cs.tao.messaging.Messaging;
import ro.cs.tao.messaging.Topic;
import ro.cs.tao.security.SystemPrincipal;
import ro.cs.tao.serialization.JsonMapper;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.OrchestratorService;
import ro.cs.tao.services.orchestration.beans.ExecutionRequest;
import ro.cs.tao.services.orchestration.beans.WPSExecutionRequest;
import ro.cs.tao.utils.StringUtilities;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/orchestrator")
public class OrchestrationController extends BaseController {

    @Autowired
    private OrchestratorService orchestrationService;

    @RequestMapping(value = "/start", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> start(@RequestBody ExecutionRequest request) {
        ResponseEntity<ServiceResponse<?>> response;
        try {
        	final long jobID = orchestrationService.startWorkflow(request.getWorkflowId(),
                                                                  request.getName(),
                                                                  request.getParameters());
        	if (jobID != -1) {
                response = prepareResult(jobID, "Execution started");
        	} else {
        		response = prepareResult("Cannot start execution. See error log for details", ResponseStatus.FAILED);
        	}
        } catch (ExecutionException ex) {
            response = handleException(ex);
        }
        return response;
    }

    @RequestMapping(value = "/parameters/{id}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getWorkflowParameters(@PathVariable("id") long workflowId) {
        try {
            return prepareResult(orchestrationService.getWorkflowParameters(workflowId));
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    @RequestMapping(value = "/outputs/{id}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getWorkflowOutputs(@PathVariable("id") long workflowId) {
        try {
            return prepareResult(orchestrationService.getWorkflowOutputs(workflowId));
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    @RequestMapping(value = "/stop/{id}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> stop(@PathVariable("id") long workflowId) {
        ResponseEntity<ServiceResponse<?>> response;
        try {
            orchestrationService.stopJob(workflowId);
            response = prepareResult("Execution stopped", ResponseStatus.SUCCEEDED);
        } catch (ExecutionException ex) {
            response = handleException(ex);
        }
        return response;
    }

    @RequestMapping(value = "/pause/{id}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> pause(@PathVariable("id") long workflowId) {
        ResponseEntity<ServiceResponse<?>> response;
        try {
            orchestrationService.pauseJob(workflowId);
            response = prepareResult("Execution suspended", ResponseStatus.SUCCEEDED);
        } catch (ExecutionException ex) {
            response = handleException(ex);
        }
        return response;
    }

    @RequestMapping(value = "/resume/{id}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> resume(@PathVariable("id") long workflowId) {
        ResponseEntity<ServiceResponse<?>> response;
        try {
            orchestrationService.resumeJob(workflowId);
            response = prepareResult("Execution resumed", ResponseStatus.SUCCEEDED);
        } catch (ExecutionException ex) {
            response = handleException(ex);
        }
        return response;
    }

    @RequestMapping(value = "/running/tasks", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getRunningTasks() {
        List<ExecutionTaskSummary> tasks =
                orchestrationService.getRunningTasks(isCurrentUserAdmin() ? null : currentUser());
        if (tasks == null) {
            tasks = new ArrayList<>();
        }
        return prepareResult(tasks);
    }

    @RequestMapping(value = "/running/jobs", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getRunningJobs() {
        List<ExecutionJobSummary> summaries = orchestrationService.getRunningJobs(isCurrentUserAdmin() ? null : currentUser());
        if (summaries == null) {
            summaries = new ArrayList<>();
        }
        return prepareResult(summaries);
    }

    @RequestMapping(value = "/{jobId}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getJobTaskStatuses(@PathVariable("jobId") long jobId) {
        List<ExecutionTaskSummary> summaries = orchestrationService.getTasksStatus(jobId);
        if (summaries == null) {
            summaries = new ArrayList<>();
        }
        return prepareResult(summaries);
    }

    @RequestMapping(value = "/history", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getJobsHistory() {
        List<ExecutionJobSummary> summaries = orchestrationService.getCompletedJobs(isCurrentUserAdmin() ? null : currentUser());
        if (summaries == null) {
            summaries = new ArrayList<>();
        }
        return prepareResult(summaries);
    }

    @RequestMapping(value = "/process", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> processExternalGraph(@RequestParam("graph") String jsonGraph,
                                                                   @RequestParam("callback") String callback,
                                                                   @RequestParam(name = "container", required = false) String container) {
        try {
            if (StringUtilities.isNullOrEmpty(jsonGraph)) {
                throw new Exception("Empty execution graph");
            }
            EndpointDescriptor callbackObj = null;
            if (callback == null) {
                throw new Exception("Empty callback");
            } else {
                callbackObj = JsonMapper.instance().readerFor(EndpointDescriptor.class).readValue(callback);
                if (callbackObj.getProtocol() == null) {
                    throw new Exception("No protocol specified for callback");
                }
                if (callbackObj.getHostName() == null) {
                    throw new Exception("No host specified for callback");
                }
                if (callbackObj.getPort() < 0 || callbackObj.getPort() > 65535) {
                    throw new Exception("Invalid port value for callback");
                }
            }
            final long jobId = orchestrationService.startExternalWorkflow(jsonGraph, container, callbackObj);
            return jobId != -1 ?
                    prepareResult(jobId, "Execution started") :
                    prepareResult("Cannot start execution. See error log for details", ResponseStatus.FAILED);
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    @RequestMapping(value = "/wps", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> invokeRemoteWPS(@RequestBody WPSExecutionRequest request) {
        try {
            if (request == null) {
                throw new Exception("Empty body");
            }
            final long jobId = orchestrationService.invokeWPS(request.getWpsId(),
                                                              new HashMap<String, Map<String, String>>() {{
                                                                  put(request.getWpsId(), request.getParameters());
                                                              }});
            return jobId != -1 ?
                    prepareResult(jobId, "Execution started") :
                    prepareResult("Cannot start execution. See error log for details", ResponseStatus.FAILED);
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    @RequestMapping(value = "/createJob", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> createJobTemplate(@RequestBody ExecutionRequest request) {
        ResponseEntity<ServiceResponse<?>> response;
        try {
            final ExecutionJob job = orchestrationService.createJobExternal(request.getWorkflowId(),
                                                                                    request.getName(), request.getParameters());
            if (job == null) {
                throw new ExecutionException("Failed to create JSON");
            }
            final Path jsonPath = Paths.get(SystemVariable.USER_FILES.value(), job.getId().toString() + ".json");
            response = prepareResult(jsonPath, "The JSON model for the job should be available in the path",
                                     ResponseStatus.SUCCEEDED, HttpStatus.OK);
        } catch (ExecutionException ex) {
            response = handleException(ex);
        }
        return response;
    }

    @RequestMapping(value = "/statusChanged", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> taskStatusChanged(@RequestParam("taskId") long taskId,
                                                            @RequestParam("status") String status,
                                                            @RequestParam(name = "message", required = false) String message) {
        try {
            final ExecutionStatus taskStatus = EnumUtils.getEnumConstantByName(ExecutionStatus.class, status);
            if (!StringUtilities.isNullOrEmpty(message)) {
                final Message msg = Message.create(SystemPrincipal.instance().getName(), this, taskStatus.name());
                msg.setMessage(message);
                Messaging.send(SystemPrincipal.instance(), Topic.EXECUTION.value(), taskId, msg, true);
            } else {
                Messaging.send(SystemPrincipal.instance(), Topic.EXECUTION.value(), taskId, taskStatus.name());
            }
            return prepareResult("", ResponseStatus.SUCCEEDED);
        } catch (Exception e) {
            return handleException(e);
        }
    }
}
