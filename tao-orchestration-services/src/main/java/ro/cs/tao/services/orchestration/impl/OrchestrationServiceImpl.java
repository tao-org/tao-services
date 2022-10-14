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
import ro.cs.tao.component.TargetDescriptor;
import ro.cs.tao.datasource.beans.Parameter;
import ro.cs.tao.docker.ExecutionConfiguration;
import ro.cs.tao.execution.ExecutionException;
import ro.cs.tao.execution.callback.EndpointDescriptor;
import ro.cs.tao.execution.model.*;
import ro.cs.tao.execution.persistence.ExecutionJobProvider;
import ro.cs.tao.execution.persistence.ExecutionTaskProvider;
import ro.cs.tao.orchestration.Orchestrator;
import ro.cs.tao.orchestration.RunnableContextFactory;
import ro.cs.tao.orchestration.RunnableDelegateProvider;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.commons.dev.MockData;
import ro.cs.tao.services.interfaces.OrchestratorService;
import ro.cs.tao.services.interfaces.WorkflowService;
import ro.cs.tao.spi.ServiceRegistryManager;
import ro.cs.tao.workflow.ExternalGraphConverter;
import ro.cs.tao.workflow.WorkflowDescriptor;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service("orchestrationService")
public class OrchestrationServiceImpl implements OrchestratorService {

    @Autowired
    private ExecutionJobProvider jobProvider;

    @Autowired
    private ExecutionTaskProvider taskProvider;

    @Autowired
    private WorkflowService workflowService;

    @Override
    public long startWorkflow(long workflowId, String description, Map<String, Map<String, String>> inputs) throws ExecutionException {
        ensureUserExecutor(2);
        final ExecutionJob job = Orchestrator.getInstance().startWorkflow(SessionStore.currentContext(), workflowId, description, inputs);
        return job != null ? job.getId() : -1;
    }

    @Override
    public Map<String, List<Parameter>> getWorkflowParameters(long workflowId) throws PersistenceException {
        if (!isDevModeEnabled()) {
            return workflowService.getWorkflowParameters(workflowId);
        } else {
            return MockData.getMockParameters().get(workflowId);
        }
    }

    @Override
    public List<TargetDescriptor> getWorkflowOutputs(long workflowId) throws PersistenceException {
        if (!isDevModeEnabled()) {
            return workflowService.getWorkflowOutputs(workflowId);
        } else {
            return MockData.getMockOutputs().get(workflowId);
        }
    }

    @Override
    public ExecutionJob createJobExternal(long workflowId, String jobName, Map<String, Map<String, String>> inputs) throws ExecutionException {
        ensureUserExecutor(1);
        return Orchestrator.getInstance().createJob(SessionStore.currentContext(), workflowId, jobName, inputs);
    }

    @Override
    public void stopJob(long jobId) throws ExecutionException {
        Orchestrator.getInstance().stopJob(jobId);
    }

    @Override
    public void pauseJob(long jobId) throws ExecutionException {
        Orchestrator.getInstance().pauseJob(jobId);
    }

    @Override
    public void resumeJob(long jobId) throws ExecutionException {
        Orchestrator.getInstance().resumeJob(jobId);
    }

    @Override
    public int purgeJobs(String user) throws ExecutionException {
        return Orchestrator.getInstance().purgeJobs(user);
    }

    @Override
    public List<ExecutionTaskSummary> getRunningTasks(String userName) {
        final List<ExecutionTaskSummary> summaries = new ArrayList<>();
        final Set<ExecutionStatus> statuses = new HashSet<>();
        Collections.addAll(statuses, ExecutionStatus.RUNNING, ExecutionStatus.QUEUED_ACTIVE);
        final List<ExecutionJob> jobs = jobProvider.list(userName, statuses);
        for (ExecutionJob job : jobs) {
            summaries.addAll(getTasksStatus(job.getId()));
        }
        return summaries;
    }

    @Override
    public List<ExecutionTaskSummary> getTasksStatus(long jobId) {
        return taskProvider.getTasksStatus(jobId);

    }

    @Override
    public List<ExecutionJobSummary> getRunningJobs(String userName) {
        final List<ExecutionJobSummary> summaries = new ArrayList<>();
        final Set<ExecutionStatus> statuses = new HashSet<>();
        Collections.addAll(statuses, ExecutionStatus.RUNNING, ExecutionStatus.QUEUED_ACTIVE);
        final List<ExecutionJob> jobs = jobProvider.list(userName, statuses);
        List<ExecutionTaskSummary> tasksStatus;
        for (ExecutionJob job : jobs) {
            tasksStatus = getTasksStatus(job.getId());
            ExecutionJobSummary summary = new ExecutionJobSummary();
            summary.setId(job.getId());
            summary.setJobName(job.getName());
            summary.setUser(job.getUserName());
            if (job.getWorkflowId() != null) {
                final String workflowName = tasksStatus.size() > 0 ?
                        tasksStatus.get(0).getWorkflowName() :
                        workflowService.getFullDescriptor(job.getWorkflowId()).getName();
                summary.setWorkflowName(workflowName);
            }
            summary.setJobStatus(job.getExecutionStatus());
            summary.setJobStart(job.getStartTime());
            summary.setJobEnd(job.getEndTime());
            summary.setTaskSummaries(tasksStatus);
            summaries.add(summary);
        }
        // Sort by start timestamp descending (newest first)
        summaries.sort((o1, o2) -> o2.getJobStart().compareTo(o1.getJobStart()));
        return summaries;
    }

    @Override
    public List<ExecutionJobSummary> getCompletedJobs(String userName) {
        final List<ExecutionJobSummary> summaries = new ArrayList<>();
        final Set<ExecutionStatus> statuses = new HashSet<>();
        Collections.addAll(statuses, ExecutionStatus.SUSPENDED, ExecutionStatus.DONE,
                                     ExecutionStatus.FAILED, ExecutionStatus.CANCELLED);
        final List<ExecutionJob> jobs = jobProvider.list(userName, statuses);
        List<ExecutionTaskSummary> tasksStatus;
        for (ExecutionJob job : jobs) {
            tasksStatus = getTasksStatus(job.getId());
            ExecutionJobSummary summary = new ExecutionJobSummary();
            if (job.getWorkflowId() != null) {
                final String workflowName = tasksStatus.size() > 0 ?
                        tasksStatus.get(0).getWorkflowName() :
                        workflowService.getFullDescriptor(job.getWorkflowId()).getName();
                summary.setWorkflowName(workflowName);
            }
            summary.setJobName(job.getName());
            summary.setUser(job.getUserName());
            summary.setJobStatus(job.getExecutionStatus());
            summary.setJobStart(job.getStartTime());
            summary.setJobEnd(job.getEndTime());
            summary.setTaskSummaries(tasksStatus);
            summaries.add(summary);
        }
        // Sort by end timestamp descending (newest first)
        summaries.sort((o1, o2) -> o2.getJobEnd() != null ? o2.getJobEnd().compareTo(o1.getJobEnd()) : -1);
        return summaries;
    }

    @Override
    public void clearJobHistory(String userName) throws PersistenceException {
        final Set<ExecutionStatus> statuses = new HashSet<>();
        Collections.addAll(statuses, ExecutionStatus.SUSPENDED, ExecutionStatus.DONE,
                           ExecutionStatus.FAILED, ExecutionStatus.CANCELLED);
        final List<ExecutionJob> jobs = jobProvider.list(userName, statuses);
        for (ExecutionJob job : jobs) {
            List<ExecutionTask> tasks = job.getTasks();
            for (ExecutionTask task : tasks) {
                taskProvider.delete(task);
            }
            job.setTasks(null);
            jobProvider.delete(job);
        }
    }

    @Override
    public long startExternalWorkflow(String jsonGraph, String container, EndpointDescriptor callback) throws ExecutionException {
        final Set<ExternalGraphConverter> services = ServiceRegistryManager.getInstance().getServiceRegistry(ExternalGraphConverter.class).getServices();
        if (services == null || services.isEmpty()) {
            throw new ExecutionException("No external graph converter modules found");
        }
        final ExternalGraphConverter converter = services.iterator().next();
        try {
            WorkflowDescriptor workflowDescriptor = null;
            if (ExecutionConfiguration.developmentModeEnabled()) {
                workflowDescriptor = workflowService.getDescriptor("Sen2Agri-Workflow-4875");
            }
            if (workflowDescriptor == null) {
                workflowDescriptor = converter.convert(jsonGraph, container);
            }
            final ExecutionJob job = Orchestrator.getInstance().startExternalWorkflow(SessionStore.currentContext(),
                                                                                      workflowDescriptor.getId(),
                                                                                      workflowDescriptor.getName(),
                                                                                      null,
                                                                                      new DelegatingSecurityContextExecutorService(Executors.newFixedThreadPool(2),
                                                                                                                                   SecurityContextHolder.getContext()),
                                                                                      callback);
            return job != null ? job.getId() : -1;
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
    }

    @Override
    public long invokeWPS(String identifier, Map<String, Map<String, String>> inputs) throws ExecutionException {
        try {
            final ExecutionJob job = Orchestrator.getInstance().startExecution(SessionStore.currentContext(), identifier, inputs,
                                                                               new DelegatingSecurityContextExecutorService(Executors.newFixedThreadPool(1),
                                                                                                 SecurityContextHolder.getContext()));
            return job != null ? job.getId() : -1;
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
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
    }

    private void ensureUserExecutor(int numThreads) {
        final Orchestrator orchestrator = Orchestrator.getInstance();
        final String user = SessionStore.currentContext().getPrincipal().getName();
        if (!orchestrator.getUserExecutors().containsKey(SessionStore.currentContext().getPrincipal().getName())) {
            orchestrator.getUserExecutors().put(user,
                                                new DelegatingSecurityContextExecutorService(Executors.newFixedThreadPool(numThreads),
                                                                                             SecurityContextHolder.getContext()));
        }
    }
}
