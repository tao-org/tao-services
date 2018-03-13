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

package ro.cs.tao.services.orchestration.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.execution.ExecutionException;
import ro.cs.tao.execution.model.ExecutionTask;
import ro.cs.tao.orchestration.Orchestrator;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.services.interfaces.OrchestratorService;

import javax.annotation.PostConstruct;
import java.util.List;

@Service("orchestrationService")
public class OrchestrationServiceImpl implements OrchestratorService {

    @Autowired
    private PersistenceManager persistenceManager;

    @Override
    public void startWorkflow(long workflowId) throws ExecutionException {
        Orchestrator.getInstance().startWorkflow(workflowId);
    }

    @Override
    public void stopWorkflow(long workflowId) throws ExecutionException {
        Orchestrator.getInstance().stopWorkflow(workflowId);
    }

    @Override
    public void pauseWorkflow(long workflowId) throws ExecutionException {
        Orchestrator.getInstance().pauseWorkflow(workflowId);
    }

    @Override
    public void resumeWorkflow(long workflowId) throws ExecutionException {
        Orchestrator.getInstance().resumeWorkflow(workflowId);
    }

    @Override
    public List<ExecutionTask> getRunningTasks() {
        return persistenceManager.getRunningTasks();
    }

    @PostConstruct
    private void initialize() {
        Orchestrator.getInstance().setPersistenceManager(persistenceManager);
    }
}
