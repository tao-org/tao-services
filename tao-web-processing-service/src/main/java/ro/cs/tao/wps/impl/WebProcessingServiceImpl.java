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

package ro.cs.tao.wps.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ro.cs.tao.component.TargetDescriptor;
import ro.cs.tao.datasource.beans.Parameter;
import ro.cs.tao.execution.model.ExecutionJob;
import ro.cs.tao.execution.model.ExecutionStatus;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.services.interfaces.OrchestratorService;
import ro.cs.tao.services.interfaces.StorageService;
import ro.cs.tao.services.interfaces.WebProcessingService;
import ro.cs.tao.services.interfaces.WorkflowService;
import ro.cs.tao.services.model.FileObject;
import ro.cs.tao.services.model.workflow.WorkflowInfo;
import ro.cs.tao.workflow.WorkflowDescriptor;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Service("webProcessingService")
public class WebProcessingServiceImpl implements WebProcessingService {

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private OrchestratorService orchestratorService;

    @Autowired
    private PersistenceManager persistenceManager;

    @Autowired
    private StorageService<MultipartFile> storageService;
    long justStartedJobId;

    @Override
    public List<WorkflowInfo> getCapabilities() {
        return workflowService.getPublicWorkflows();
    }

    @Override
    public ProcessInfo describeProcess(long workflowId) {
        final ProcessInfoImpl processInfo = new ProcessInfoImpl();
        processInfo.setParameters(getWorkflowParametersSave(workflowId));
        processInfo.setOutputs(getWorkflowOutputsSave(workflowId));
        processInfo.setWorkflowInfo(getWorkflowInfoSafe(workflowId));
        return processInfo;
    }

    @Override
    public long execute(long workflowId, Map<String, Map<String, String>> parameters) {
        if (isDevModeEnabled()){
            justStartedJobId = 1769187931756938475L;
            return justStartedJobId;
        }
        long result = -1;
        try {
            WorkflowDescriptor descriptor = workflowService.findById(workflowId);
            if (descriptor != null) {
                String jobName = descriptor.getName() + " via WPS on " + LocalDateTime.now().format(DateTimeFormatter.ISO_TIME);
                result = orchestratorService.startWorkflow(workflowId, jobName, parameters);
            }
        } catch (PersistenceException e) {
            Logger.getLogger(WebProcessingService.class.getName()).severe(e.getMessage());
        }
        return result;
    }

    @Override
    public ExecutionJob getStatus(long jobId) {
        if (isDevModeEnabled()) {
            if(jobId == justStartedJobId) {
                final ExecutionJob executionJob = new ExecutionJob();
                executionJob.setWorkflowId(24L);
                executionJob.setExecutionStatus(ExecutionStatus.RUNNING);
                return executionJob;
            }
        }
        return persistenceManager.getJobById(jobId);
    }

    @Override
    public List<FileObject> getJobResult(long jobId) throws IOException {
        if (isDevModeEnabled()) {
            return new ArrayList<>();
        }
        return storageService.getJobResults(jobId);
    }

    private WorkflowInfo getWorkflowInfoSafe(long workflowId) {
        if (isDevModeEnabled()) {
            final WorkflowDescriptor descriptor = new WorkflowDescriptor();
            descriptor.setId(workflowId);
            descriptor.setName("MockWorkflowName");
            return new WorkflowInfo(descriptor);
        }
        try {
            return workflowService.getWorkflowInfo(workflowId);
        } catch (Exception e) {
            return null;
        }
    }

    private List<TargetDescriptor> getWorkflowOutputsSave(long workflowId) {
        try {
            return orchestratorService.getWorkflowOutputs(workflowId);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private Map<String, List<Parameter>> getWorkflowParametersSave(long workflowId) {
        try {
            return orchestratorService.getWorkflowParameters(workflowId);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public static class ProcessInfoImpl implements ProcessInfo {

        private Map<String, List<Parameter>> parameters;
        private List<TargetDescriptor> outputs;
        private WorkflowInfo workflowInfo;

        @Override
        public Map<String, List<Parameter>> getParameters() {
            return parameters;
        }

        void setParameters(Map<String, List<Parameter>> parameters) {
            this.parameters = parameters;
        }

        @Override
        public List<TargetDescriptor> getOutputs() {
            return outputs;
        }

        void setOutputs(List<TargetDescriptor> outputs) {
            this.outputs = outputs;
        }

        @Override
        public WorkflowInfo getWorkflowInfo() {
            return workflowInfo;
        }

        void setWorkflowInfo(WorkflowInfo workflowInfo) {
            this.workflowInfo = workflowInfo;
        }
    }
}
