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
import ro.cs.tao.datasource.beans.Parameter;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.services.interfaces.OrchestratorService;
import ro.cs.tao.services.interfaces.WebProcessingService;
import ro.cs.tao.services.interfaces.WorkflowService;
import ro.cs.tao.services.model.workflow.WorkflowInfo;
import ro.cs.tao.workflow.WorkflowDescriptor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Service("webProcessingService")
public class WebProcessingServiceImpl implements WebProcessingService {

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private OrchestratorService orchestratorService;

    @Override
    public List<WorkflowInfo> getCapabilities() {
        return workflowService.getPublicWorkflows();
    }

    @Override
    public Map<String, List<Parameter>> describeProcess(long workflowId) {
        return workflowService.getWorkflowParameters(workflowId);
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
}
