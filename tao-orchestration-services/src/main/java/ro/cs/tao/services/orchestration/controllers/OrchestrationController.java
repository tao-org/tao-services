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

import io.swagger.v3.oas.annotations.tags.Tag;
import org.ggf.drmaa.SessionFactory;
import org.locationtech.jts.geom.Envelope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.EnumUtils;
import ro.cs.tao.datasource.param.CommonParameterNames;
import ro.cs.tao.drmaa.Environment;
import ro.cs.tao.execution.ExecutionException;
import ro.cs.tao.execution.ExecutionsManager;
import ro.cs.tao.execution.callback.EndpointDescriptor;
import ro.cs.tao.execution.model.*;
import ro.cs.tao.execution.persistence.ExecutionJobProvider;
import ro.cs.tao.execution.persistence.ExecutionTaskProvider;
import ro.cs.tao.messaging.Message;
import ro.cs.tao.messaging.Messaging;
import ro.cs.tao.messaging.Topic;
import ro.cs.tao.orchestration.Orchestrator;
import ro.cs.tao.security.SystemPrincipal;
import ro.cs.tao.serialization.GeometryAdapter;
import ro.cs.tao.serialization.JsonMapper;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.OrchestratorService;
import ro.cs.tao.services.orchestration.beans.TaskOutput;
import ro.cs.tao.services.orchestration.beans.TaskSummary;
import ro.cs.tao.services.orchestration.beans.WebServiceExecutionRequest;
import ro.cs.tao.utils.StringUtilities;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/orchestrator")
@Tag(name = "Orchestration", description = "Operations related to workflow execution")
public class OrchestrationController extends BaseController {

    @Autowired
    private OrchestratorService orchestrationService;
    @Autowired
    private ExecutionJobProvider executionJobManager;
    @Autowired
    private ExecutionTaskProvider executionTaskProvider;

    /**
     * Returns the available parameters for a workflow
     * @param workflowId    The workflow identifier
     */
    @RequestMapping(value = "/environment", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getEnvironments() {
        try {
            Set<Environment> environments = SessionFactory.getEnvironments();
            if (environments == null) {
                environments = new HashSet<>();
            }
            final List<Environment> list = environments.stream().sorted().toList();
            return prepareResult(list);
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    /**
     * Starts the execution of a workflow
     * @param request   Structure containing the workflow identifier, parameters, job type and name
     * @see JobType
     */
    @RequestMapping(value = "/start", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> start(@RequestBody ExecutionRequest request) {
        ResponseEntity<ServiceResponse<?>> response;
        try {
        	final long jobID = orchestrationService.startWorkflow(request);
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

    /**
     * Returns the available parameters for a workflow
     * @param workflowId    The workflow identifier
     */
    @RequestMapping(value = "/parameters/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getWorkflowParameters(@PathVariable("id") long workflowId) {
        try {
            return prepareResult(orchestrationService.getWorkflowParameters(workflowId));
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    /**
     * Returns the outputs of a workflow execution (a workflow can have more than one execution)
     * @param workflowId    The workflow identifier
     */
    @RequestMapping(value = "/outputs/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getWorkflowOutputs(@PathVariable("id") long workflowId) {
        try {
            return prepareResult(orchestrationService.getWorkflowOutputs(workflowId));
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    /**
     * Stops a running job
     * @param jobId The job identifier
     */
    @RequestMapping(value = "/stop", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> stop(@RequestParam("jobId") long jobId) {
        ResponseEntity<ServiceResponse<?>> response;
        try {
            orchestrationService.stopJob(jobId);
            response = prepareResult("Execution stopped", ResponseStatus.SUCCEEDED);
        } catch (ExecutionException ex) {
            response = handleException(ex);
        }
        return response;
    }

    /**
     * Pauses a running job
     * @param jobId The job identifier
     */
    @RequestMapping(value = "/pause", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> pause(@RequestParam("jobId") long jobId) {
        ResponseEntity<ServiceResponse<?>> response;
        try {
            orchestrationService.pauseJob(jobId);
            response = prepareResult("Execution suspended", ResponseStatus.SUCCEEDED);
        } catch (ExecutionException ex) {
            response = handleException(ex);
        }
        return response;
    }

    /**
     * Resumes to execution a paused job
     * @param jobId The job identifier
     */
    @RequestMapping(value = "/resume", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> resume(@RequestParam("jobId") long jobId) {
        ResponseEntity<ServiceResponse<?>> response;
        try {
            orchestrationService.resumeJob(jobId);
            response = prepareResult("Execution resumed", ResponseStatus.SUCCEEDED);
        } catch (ExecutionException ex) {
            response = handleException(ex);
        }
        return response;
    }

    /**
     * Removes all pending jobs of the given user.
     * If no userId is given, it will remove all pending jobs from the job queue.
     *
     * @param userId    The user identifier
     */
    @RequestMapping(value = "/purge", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> purgeJobs(@RequestParam(name = "userId", required = false) String userId) {
        ResponseEntity<ServiceResponse<?>> response;
        try {
            if (!isCurrentUserAdmin()) {
                response = prepareResult("Unauthorized", HttpStatus.UNAUTHORIZED);
            } else {
                response = prepareResult(orchestrationService.purgeJobs(userId));
            }
        } catch (ExecutionException ex) {
            response = handleException(ex);
        }
        return response;
    }

    /**
     * Returns the running tasks (steps of a job) of the current user
     */
    @RequestMapping(value = "/running/tasks", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getRunningTasks() {
        List<ExecutionTaskSummary> tasks =
                orchestrationService.getRunningTasks(isCurrentUserAdmin() ? null : currentUser());
        if (tasks == null) {
            tasks = new ArrayList<>();
        }
        return prepareResult(tasks.stream().map(TaskSummary::toTaskSummary).collect(Collectors.toList()));
    }

    /**
     * Returns the output and the command line of a task
     */
    @RequestMapping(value = "/tasks/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getTaskOutput(@PathVariable("id") long taskId) {
        try {
            final ExecutionTaskSummary task = executionTaskProvider.getTaskStatus(taskId);
            if (task == null) {
                throw new IllegalArgumentException("Task does not exist");
            }
            final TaskOutput taskOutput = new TaskOutput();
            taskOutput.setTaskId(task.getTaskId());
            taskOutput.setOutput(task.getOutput());
            taskOutput.setCommand(task.getCommand());
            return prepareResult(taskOutput);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * Returns the running jobs of the current user.
     * If the user is in the admin group, returns all the running jobs.
     */
    @RequestMapping(value = "/running/jobs", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getRunningJobs() {
        List<ExecutionJobSummary> summaries = orchestrationService.getRunningJobs(isCurrentUserAdmin() ? null : currentUser());
        if (summaries == null) {
            summaries = new ArrayList<>();
        }
        return prepareResult(summaries);
    }

    /**
     * Returns the running jobs of a specific user.
     * Only an administrator can call perform this operation.
     * @param userId  The user identifier
     */
    @RequestMapping(value = "/running/jobs/{userId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getRunningJobsForUser(@PathVariable("userId") String userId) {
        if (!currentUser().equals(userId) && !isCurrentUserAdmin()) {
            return prepareResult(null, HttpStatus.UNAUTHORIZED);
        }
        List<ExecutionJobSummary> summaries = orchestrationService.getRunningJobs(userId);
        if (summaries == null) {
            summaries = new ArrayList<>();
        }
        return prepareResult(summaries);
    }

    /**
     * Returns the queued jobs for all the users (only if the current user is an admin)
     */
    @RequestMapping(value = "/queued/jobs", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getQueuedJobs(@RequestParam(name = "grouped", required = false) Boolean grouped) {
        if (!isCurrentUserAdmin()) {
            return prepareResult(null, HttpStatus.UNAUTHORIZED);
        }
        if (grouped != null && grouped) {
            Map<String, Queue<ExecutionJobSummary>> summaries = orchestrationService.getQueuedUserJobs();
            if (summaries == null) {
                summaries = new HashMap<>();
            }
            return prepareResult(summaries);
        } else {
            Queue<ExecutionJobSummary> summaries = orchestrationService.getQueuedJobs();
            if (summaries == null) {
                summaries = new ArrayDeque<>();
            }
            return prepareResult(summaries);
        }
    }

    /**
     * Returns the queued jobs of a specific user.
     * @param userId  The user identifier
     */
    @RequestMapping(value = "/queued/jobs/{userId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getQueuedJobsForUser(@PathVariable("userId") String userId) {
        if (!currentUser().equals(userId) && !isCurrentUserAdmin()) {
            return prepareResult(null, HttpStatus.UNAUTHORIZED);
        }
        Queue<ExecutionJobSummary> summaries = orchestrationService.getQueuedJobs(userId);
        if (summaries == null) {
            summaries = new ArrayDeque<>();
        }
        return prepareResult(summaries);
    }

    @RequestMapping(value = "/queued/jobs", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> moveJobInQueue(@RequestParam(name = "user", required = false) String userId,
                                                             @RequestParam("jobId") long jobId,
                                                             @RequestParam("operation") QueueOperation operation) {
        if (!currentUser().equals(userId) && !isCurrentUserAdmin()) {
            return prepareResult(null, HttpStatus.UNAUTHORIZED);
        }
        try {
            switch (operation) {
                case MOVE_UP:
                    orchestrationService.moveJobUp(jobId, userId);
                    break;
                case MOVE_DOWN:
                    orchestrationService.moveJobDown(jobId, userId);
                    break;
                case REMOVE:
                    orchestrationService.deleteJobFromQueue(userId, jobId);
                    break;
            }
            return prepareResult("Job queue operation completed", ResponseStatus.SUCCEEDED);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * Returns the summaries of tasks of a job
     * @param jobId The job identifier
     */
    @RequestMapping(value = "/{jobId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getJobTaskStatuses(@PathVariable("jobId") long jobId) {
        List<ExecutionTaskSummary> summaries = orchestrationService.getTasksStatus(jobId);
        if (summaries == null) {
            summaries = new ArrayList<>();
        }
        return prepareResult(summaries);
    }

    /**
     * Returns the outputs of a job
     * @param jobId The job identifier
     */
    @RequestMapping(value = "/{jobId}/output", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getJobOutput(@PathVariable("jobId") long jobId) {
        ExecutionJob job = executionJobManager.get(jobId);
        String output;
        if (job == null) {
            output = null;
        } else {
            if (job.getExecutionStatus() != ExecutionStatus.RUNNING) {
                output = job.orderedTasks().stream().map(ExecutionTask::getLog).collect(Collectors.joining("\n-----\n"));
            } else {
                output = ExecutionsManager.getInstance().getJobOutput(jobId);
            }
        }
        return prepareResult(output);
    }

    /**
     * Returns the summaries of finished jobs of a user.
     * If the user is not specified:
     *  - for an administrator it returns the summaries of all finished jobs
     *  - for a user it returns the summaries of all his/her finished jobs
     * @param userId  The user account identifier
     */
    @RequestMapping(value = {"/history","/history/{userId}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getJobsHistory(@PathVariable("userId") String userId) {
        List<ExecutionJobSummary> summaries = orchestrationService.getCompletedJobs(userId != null ? userId : (isCurrentUserAdmin() ? null : currentUser()));
        if (summaries == null) {
            summaries = new ArrayList<>();
        }
        return prepareResult(summaries);
    }

    /**
     * Clears the job history for a user.
     * If the user is not specified it clears the jobs of the current user
     * @param userId  The user identifier
     */
    @RequestMapping(value = {"/history", "/history/{userId}"}, method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> clearJobsHistory(@PathVariable("userId") String userId) {
        if (!currentUser().equals(userId) && !isCurrentUserAdmin()) {
            return prepareResult(null, HttpStatus.UNAUTHORIZED);
        }
        try {
            orchestrationService.clearJobHistory(userId != null ? userId : currentUser());
            return prepareResult("Job history deleted", ResponseStatus.SUCCEEDED);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/process", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
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

    /**
     * Performs a remote Web Processing Service invocation.
     * @param request   The invocation parameters
     */
    @RequestMapping(value = "/wps", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> invokeRemoteWPS(@RequestBody WebServiceExecutionRequest request) {
        try {
            if (request == null) {
                throw new Exception("Empty body");
            }
            final Map<String, String> parameters = request.getParameters();
            if (parameters != null) {
                if (parameters.containsKey(CommonParameterNames.START_DATE)) {
                    parameters.put("service~startDate", parameters.get(CommonParameterNames.START_DATE));
                    parameters.remove(CommonParameterNames.START_DATE);
                }
                if (parameters.containsKey(CommonParameterNames.END_DATE)) {
                    parameters.put("service~endDate", parameters.get(CommonParameterNames.END_DATE));
                    parameters.remove(CommonParameterNames.END_DATE);
                }
                if (parameters.containsKey(CommonParameterNames.FOOTPRINT)) {
                    parameters.put("service~footprint", parameters.get(CommonParameterNames.FOOTPRINT));
                    parameters.remove(CommonParameterNames.FOOTPRINT);
                }
                if (parameters.containsKey("userId")) {
                    parameters.put("service~userId", parameters.get("userId"));
                    parameters.remove("userId");
                }
                if (parameters.containsKey("name")) {
                    parameters.put("service~name", parameters.get("name"));
                    parameters.remove("name");
                }
                if (parameters.containsKey("additionalSupport")) {
                    parameters.put("service~additionalSupport", parameters.get("additionalSupport"));
                    parameters.remove("additionalSupport");
                }
                if (parameters.containsKey("additionalDataSpecification")) {
                    parameters.put("service~additionalDataSpecification", parameters.get("additionalDataSpecification"));
                    parameters.remove("additionalDataSpecification");
                }
                if (parameters.containsKey("observationData")) {
                    parameters.put("service~observationData", parameters.get("observationData"));
                    parameters.remove("observationData");
                }
                if (parameters.containsKey("availability")) {
                    parameters.put("service~availability", parameters.get("availability"));
                    parameters.remove("availability");
                }
            }
            final long jobId = orchestrationService.invokeWPS(request.getWebServiceId(),
                                                              new HashMap<String, Map<String, String>>() {{
                                                                  put(request.getWebServiceId(), parameters);
                                                              }});
            return jobId != -1 ?
                    prepareResult(jobId, "Execution started") :
                    prepareResult("Cannot start execution. See error log for details", ResponseStatus.FAILED);
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    /**
     * Performs a remote Web Map Service invocation.
     * @param request   The invocation parameters
     */
    @RequestMapping(value = "/wms", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> invokeRemoteWMS(@RequestBody WebServiceExecutionRequest request) {
        try {
            if (request == null) {
                throw new Exception("Empty body");
            }
            final Map<String, String> parameters = request.getParameters();
            if (parameters.containsKey("footprint")) {
                final String footprint = parameters.get("footprint");
                if (!StringUtilities.isNullOrEmpty(footprint)) {
                    final Envelope envelope = new GeometryAdapter().marshal(footprint).getEnvelopeInternal();
                    String bbox = envelope.getMinX() + "," + envelope.getMinY() + "," + envelope.getMaxX() + "," + envelope.getMaxY();
                    parameters.replace("bbox", bbox);
                }
                parameters.remove("footprint");
            }
            final long jobId = orchestrationService.invokeWMS(request.getWebServiceId(), parameters);
            return jobId != -1 ?
                   prepareResult(jobId, "Execution started") :
                   prepareResult("Cannot start execution. See error log for details", ResponseStatus.FAILED);
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    /**
     * Exports a workflow execution to a specified format.
     * The workflow is not actually executed, but its execution is simulated so that inputs and outputs of tasks
     * are properly computed.
     * @param request   The export request
     */
    @RequestMapping(value = "/script", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> scriptJob(@RequestBody ExecutionRequest request) {
        ResponseEntity<ServiceResponse<?>> response;
        try {
            final ExecutionJob job = orchestrationService.scriptJob(request);
            if (job == null) {
                throw new ExecutionException("Failed to create JSON");
            }
            response = prepareResult(job.getJobOutputPath(),
                                     "The script model for the job should be available in the path",
                                     ResponseStatus.SUCCEEDED, HttpStatus.OK);
        } catch (ExecutionException ex) {
            response = handleException(ex);
        }
        return response;
    }

    /**
     * Callback to signal the status change of a task.
     * This is intended to be used by external systems calling TAO.
     * @param taskId    The task identifier
     * @param status    The new task status
     * @param message   (optional) A message associated with the status change
     */
    @RequestMapping(value = "/statusChanged", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
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

    @RequestMapping(value = "/start/forced", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> forceStart(@RequestParam("jobId") long jobId) {
        try {
            if (jobId <= 0) {
                throw new IllegalArgumentException("Invalid jobId");
            }
            final ExecutionJob job = executionJobManager.get(jobId);
            if (job == null) {
                throw new IllegalArgumentException("Job does not exist");
            }
            Orchestrator.getInstance().queueJob(job);
            return prepareResult("Job " + jobId + " queued", ResponseStatus.SUCCEEDED);
        } catch (Exception e) {
            return handleException(e);
        }
    }
}
