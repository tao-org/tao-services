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

package ro.cs.tao.services.orchestration.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import ro.cs.tao.datasource.beans.Parameter;
import ro.cs.tao.execution.ExecutionException;
import ro.cs.tao.execution.model.ExecutionJob;
import ro.cs.tao.execution.model.ExecutionJobSummary;
import ro.cs.tao.execution.model.ExecutionStatus;
import ro.cs.tao.execution.model.ExecutionTaskSummary;
import ro.cs.tao.orchestration.Orchestrator;
import ro.cs.tao.orchestration.RunnableContextFactory;
import ro.cs.tao.orchestration.RunnableDelegateProvider;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.services.commons.dev.MockData;
import ro.cs.tao.services.interfaces.OrchestratorService;
import ro.cs.tao.services.interfaces.WorkflowService;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service("orchestrationService")
public class OrchestrationServiceImpl implements OrchestratorService {

    @Autowired
    private PersistenceManager persistenceManager;

    @Autowired
    private ExecutorService executor;

    @Autowired
    private WorkflowService workflowService;

    @Override
    public long startWorkflow(long workflowId, Map<String, Map<String, String>> inputs) throws ExecutionException {
        return Orchestrator.getInstance().startWorkflow(workflowId, inputs,
                                                        new DelegatingSecurityContextExecutorService(Executors.newFixedThreadPool(2),
                                                                                                     SecurityContextHolder.getContext()));
    }

    @Override
    public Map<String, List<Parameter>> getWorkflowParameters(long workflowId) {
        if (!isDevModeEnabled()) {
            return workflowService.getWorkflowParameters(workflowId);
        } else {
            return MockData.getMockParameters().get(workflowId);
        }
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
    public List<ExecutionTaskSummary> getRunningTasks() {
        List<ExecutionTaskSummary> summaries = new ArrayList<>();
        Set<ExecutionStatus> statuses = new HashSet<>();
        Collections.addAll(statuses, ExecutionStatus.RUNNING, ExecutionStatus.QUEUED_ACTIVE);
        List<ExecutionJob> jobs = persistenceManager.getJobs(SecurityContextHolder.getContext().getAuthentication().getName(), statuses);
        for (ExecutionJob job : jobs) {
            summaries.addAll(getTasksStatus(job.getId()));
        }
        return summaries;
    }

    @Override
    public List<ExecutionTaskSummary> getTasksStatus(long jobId) {
        return persistenceManager.getTasksStatus(jobId);

    }

    @Override
    public List<ExecutionJobSummary> getRunningJobs() {
        List<ExecutionJobSummary> summaries = new ArrayList<>();
        Set<ExecutionStatus> statuses = new HashSet<>();
        Collections.addAll(statuses, ExecutionStatus.RUNNING, ExecutionStatus.QUEUED_ACTIVE);
        List<ExecutionJob> jobs = persistenceManager.getJobs(SecurityContextHolder.getContext().getAuthentication().getName(), statuses);
        for (ExecutionJob job : jobs) {
            List<ExecutionTaskSummary> tasksStatus = getTasksStatus(job.getId());
            ExecutionJobSummary summary = new ExecutionJobSummary();
            summary.setUser(job.getUserName());
            summary.setWorkflowName(tasksStatus.stream().findFirst().get().getWorkflowName());
            summary.setJobStatus(job.getExecutionStatus());
            summary.setJobStart(job.getStartTime());
            summary.setJobEnd(job.getEndTime());
            summary.setTaskSummaries(tasksStatus);
            summaries.add(summary);
        }
        return summaries;
    }

    @Override
    public List<ExecutionJobSummary> getCompletedJobs() {
        List<ExecutionJobSummary> summaries = new ArrayList<>();
        Set<ExecutionStatus> statuses = new HashSet<>();
        Collections.addAll(statuses, ExecutionStatus.SUSPENDED, ExecutionStatus.DONE,
                                     ExecutionStatus.FAILED, ExecutionStatus.CANCELLED);
        List<ExecutionJob> jobs = persistenceManager.getJobs(SecurityContextHolder.getContext().getAuthentication().getName(), statuses);
        for (ExecutionJob job : jobs) {
            List<ExecutionTaskSummary> tasksStatus = getTasksStatus(job.getId());
            ExecutionJobSummary summary = new ExecutionJobSummary();
            summary.setUser(job.getUserName());
            summary.setWorkflowName(tasksStatus.stream().findFirst().get().getWorkflowName());
            summary.setJobStatus(job.getExecutionStatus());
            summary.setJobStart(job.getStartTime());
            summary.setJobEnd(job.getEndTime());
            summary.setTaskSummaries(tasksStatus);
            summaries.add(summary);
        }
        return summaries;
    }

    @Bean
    private ExecutorService executor() {
        return new DelegatingSecurityContextExecutorService(Executors.newSingleThreadExecutor());
    }

    @PostConstruct
    private void initialize() {
        RunnableContextFactory.setDelegateProvider(new RunnableDelegateProvider() {
            @Override
            public Runnable wrap(Runnable runnable) {
                return new DelegatingSecurityContextRunnable(runnable, SecurityContextHolder.getContext());
            }
        });
        /*ExecutorServiceFactory.setDelegateProvider(new ExecutorServiceDelegateProvider() {
            @Override
            public ExecutorService createExecutor(int threads) {
                return executor;
            }
        });*/
        Orchestrator.getInstance().setPersistenceManager(persistenceManager);
    }
}
