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
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.services.interfaces.OrchestratorService;
import ro.cs.tao.services.interfaces.StorageService;
import ro.cs.tao.services.interfaces.WebProcessingService;
import ro.cs.tao.services.interfaces.WorkflowService;
import ro.cs.tao.services.model.workflow.WorkflowInfo;
import ro.cs.tao.workflow.WorkflowDescriptor;

import javax.tools.FileObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Service("webProcessingService")
public class WebProcessingServiceImpl implements WebProcessingService {

    @Autowired
    private PersistenceManager persistenceManager;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private OrchestratorService orchestratorService;

    @Autowired
    private StorageService<MultipartFile> storageService;

    @Override
    public List<WorkflowInfo> getCapabilities() {
        return workflowService.getPublicWorkflows();
    }

    @Override
    public ProcessInfo describeProcess(long workflowId) {
        final Map<String, List<Parameter>> workflowParameters = workflowService.getWorkflowParameters(workflowId);
        final List<TargetDescriptor> workflowOutputs = orchestratorService.getWorkflowOutputs(workflowId);

        final ProcessInfoImpl processInfo = new ProcessInfoImpl();
        processInfo.setParameters(workflowParameters);
        processInfo.setOutputs(workflowOutputs);

        final List<WorkflowInfo> workflowInfos = getCapabilities();
        for (WorkflowInfo info : workflowInfos) {
            if (info.getId() == workflowId) {
                processInfo.setWorkflowInfo(info);
                break;
            }
        }

        return processInfo;
    }

    @Override
    public long execute(long workflowId, Map<String, Map<String, String>> parameters) {
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
        final ExecutionJob jobById = persistenceManager.getJobById(jobId);
        return jobById;
//        return orchestratorService.getTasksStatus(jobId);
    }

    @Override
    public List<FileObject> getJobResult(long jobId) {
        throw new RuntimeException("not implemented");
    }

    public static class ProcessInfoImpl implements ProcessInfo {

        private Map<String, List<Parameter>> parameters;
        private List<TargetDescriptor> outputs;
        private WorkflowInfo workflowInfo;

        @Override
        public Map<String, List<Parameter>> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, List<Parameter>> parameters) {
            this.parameters = parameters;
        }

        @Override
        public List<TargetDescriptor> getOutputs() {
            return outputs;
        }

        public void setOutputs(List<TargetDescriptor> outputs) {
            this.outputs = outputs;
        }

        @Override
        public WorkflowInfo getWorkflowInfo() {
            return workflowInfo;
        }

        public void setWorkflowInfo(WorkflowInfo workflowInfo) {
            this.workflowInfo = workflowInfo;
        }
    }
}
