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
import ro.cs.tao.EnumUtils;
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
import ro.cs.tao.persistence.UserProvider;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.commons.dev.MockData;
import ro.cs.tao.services.interfaces.OrchestratorService;
import ro.cs.tao.services.interfaces.WorkflowService;
import ro.cs.tao.spi.ServiceRegistryManager;
import ro.cs.tao.user.User;
import ro.cs.tao.user.UserStatus;
import ro.cs.tao.utils.Tuple;
import ro.cs.tao.workflow.ExternalGraphConverter;
import ro.cs.tao.workflow.WorkflowDescriptor;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service("orchestrationService")
public class OrchestrationServiceImpl implements OrchestratorService {

    @Autowired
    private ExecutionJobProvider jobProvider;

    @Autowired
    private ExecutionTaskProvider taskProvider;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private UserProvider userProvider;

    @Override
    public long startWorkflow(ExecutionRequest request) throws ExecutionException {
        ExecutionRequest modified = request;
        long id;
        if ((id = tryCreateTemporaryWorkflow(request)) > 0) {
            // Create a new request for the cloned workflow
            modified = new ExecutionRequest(request);
            modified.setWorkflowId(id);
        }
        final ExecutionJob job = Orchestrator.getInstance().startWorkflow(SessionStore.currentContext(), modified);
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
    public ExecutionJob scriptJob(ExecutionRequest request) throws ExecutionException {
        ExecutionRequest modified = request;
        long id;
        if ((id = tryCreateTemporaryWorkflow(request)) > 0) {
            // Create a new request for the cloned workflow
            modified = new ExecutionRequest(request);
            modified.setWorkflowId(id);
        }
        JobType type = EnumUtils.getEnumConstantByName(JobType.class, request.getJobType());
        return Orchestrator.getInstance().createJob(SessionStore.currentContext(),
                                                    modified.getWorkflowId(), modified.getName(),
                                                    modified.getParameters(), type);
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
    public int purgeJobs(String userId) throws ExecutionException {
        return Orchestrator.getInstance().purgeJobs(userId);
    }

    @Override
    public List<ExecutionTaskSummary> getRunningTasks(String userId) {
        final List<ExecutionTaskSummary> summaries = new ArrayList<>();
        final Set<ExecutionStatus> statuses = EnumSet.of(ExecutionStatus.RUNNING, ExecutionStatus.QUEUED_ACTIVE);
        final List<ExecutionJob> jobs = jobProvider.list(userId, statuses);
        for (ExecutionJob job : jobs) {
            List<ExecutionTaskSummary> taskList = getTasksStatus(job.getId());
            List<ExecutionTaskSummary> runningTasks = taskList.stream().filter(e -> statuses.contains(e.getTaskStatus())).findAny().stream().collect(Collectors.toList());
            for (ExecutionTaskSummary summary: runningTasks) {
                ExecutionTask task = job.getTasks().stream().filter(t -> t.getId() == summary.getTaskId()).findFirst().get();
                summary.setUserId(job.getUserId());
                summary.setComponentType(task instanceof DataSourceExecutionTask ? "ds" : "exec");
                summary.setJobName(task.getJob().getName());
                summary.setUsedCPU(task.getUsedCPU());
                summary.setUsedRAM(task.getUsedRAM());
                summary.setCommand(task.getCommand());
            }
            summaries.addAll(runningTasks);
        }
        return summaries;
    }

    @Override
    public List<ExecutionTaskSummary> getTasksStatus(long jobId) {
        return taskProvider.getTasksStatus(jobId);

    }

    @Override
    public List<ExecutionJobSummary> getRunningJobs(String userId) {
        final List<ExecutionJobSummary> summaries = new ArrayList<>();
        final Set<ExecutionStatus> statuses = new HashSet<>();
        Collections.addAll(statuses, ExecutionStatus.RUNNING, ExecutionStatus.QUEUED_ACTIVE);
        final List<ExecutionJob> jobs = jobProvider.list(userId, statuses);
        List<ExecutionTaskSummary> tasksStatus;
        for (ExecutionJob job : jobs) {
            tasksStatus = getTasksStatus(job.getId());
            ExecutionJobSummary summary = new ExecutionJobSummary();
            summary.setId(job.getId());
            summary.setJobName(job.getName());
            summary.setUserId(job.getUserId());
            if (job.getWorkflowId() != null) {
                final String workflowName = !tasksStatus.isEmpty() ?
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
        // Sort by status (running first, pending next) and start timestamp descending (the newest first)
        summaries.sort(((Comparator<ExecutionJobSummary>) (o1, o2) -> o2.getJobStatus().value().compareTo(o1.getJobStatus().value()))
                               .thenComparing((o1, o2) -> Long.compare(o2.getTaskSummaries().stream().filter(t -> t.getTaskStatus() == ExecutionStatus.RUNNING).count(),
                                                                       o1.getTaskSummaries().stream().filter(t -> t.getTaskStatus() == ExecutionStatus.RUNNING).count()))
                               .thenComparing((o1, o2) -> o2.getJobStart().compareTo(o1.getJobStart())));
        return summaries;
    }

    @Override
    public ExecutionJobSummary getJobInfo(long jobId) {
        final ExecutionJob job = jobProvider.get(jobId);
        ExecutionJobSummary summary = null;
        if (job != null) {
            List<ExecutionTaskSummary> tasksStatus = getTasksStatus(jobId);
            summary = new ExecutionJobSummary();
            summary.setId(job.getId());
            summary.setJobName(job.getName());
            summary.setUserId(job.getUserId());
            if (job.getWorkflowId() != null) {
                final String workflowName = !tasksStatus.isEmpty() ?
                                            tasksStatus.get(0).getWorkflowName() :
                                            workflowService.getFullDescriptor(job.getWorkflowId()).getName();
                summary.setWorkflowName(workflowName);
            }
            summary.setJobStatus(job.getExecutionStatus());
            summary.setJobStart(job.getStartTime());
            summary.setJobEnd(job.getEndTime());
            summary.setTaskSummaries(tasksStatus);
        }
        return summary;
    }

    @Override
    public List<ExecutionJobSummary> getCompletedJobs(String userId) {
        final List<ExecutionJobSummary> summaries = new ArrayList<>();
        final Set<ExecutionStatus> statuses = new HashSet<>();
        Collections.addAll(statuses, ExecutionStatus.SUSPENDED, ExecutionStatus.DONE,
                                     ExecutionStatus.FAILED, ExecutionStatus.CANCELLED);
        final List<ExecutionJob> jobs = jobProvider.list(userId, statuses);
        List<ExecutionTaskSummary> tasksStatus;
        for (ExecutionJob job : jobs) {
            tasksStatus = getTasksStatus(job.getId());
            ExecutionJobSummary summary = new ExecutionJobSummary();
            if (job.getWorkflowId() != null) {
                final String workflowName = !tasksStatus.isEmpty() ?
                                            tasksStatus.get(0).getWorkflowName() :
                                            workflowService.getFullDescriptor(job.getWorkflowId()).getName();
                summary.setWorkflowName(workflowName);
            }
            summary.setId(job.getId());
            summary.setJobName(job.getName());
            summary.setUserId(job.getUserId());
            summary.setJobStatus(job.getExecutionStatus());
            summary.setJobStart(job.getStartTime());
            summary.setJobEnd(job.getEndTime());
            summary.setTaskSummaries(tasksStatus);
            summaries.add(summary);
        }
        // Sort by end timestamp descending (newest first)
        summaries.sort((o1, o2) -> o2.getJobEnd() != null && o1.getJobEnd() != null ? o2.getJobEnd().compareTo(o1.getJobEnd()) : -1);
        return summaries;
    }

    @Override
    public Map<String, Queue<ExecutionJobSummary>> getQueuedUserJobs() {
        final Map<String, Queue<ExecutionJobSummary>> results = new LinkedHashMap<>();
        if (!ExecutionConfiguration.developmentModeEnabled()) {
            final Map<String, List<Long>> jobs = Orchestrator.getInstance().getQueuedJobs();
            for (Map.Entry<String, List<Long>> entry : jobs.entrySet()) {
                results.put(entry.getKey(), new ArrayDeque<>());
                final List<ExecutionJob> list = jobProvider.list(entry.getValue());
                for (ExecutionJob job : list) {
                    ExecutionJobSummary summary = new ExecutionJobSummary();
                    summary.setId(job.getId());
                    summary.setWorkflowName(workflowService.findById(job.getWorkflowId()).getName());
                    summary.setJobName(job.getName());
                    summary.setUserId(job.getUserId());
                    summary.setJobStatus(job.getExecutionStatus());
                    summary.setJobStart(job.getStartTime());
                    summary.setJobEnd(job.getEndTime());
                    results.get(entry.getKey()).offer(summary);
                }
            }
        } else {
                final List<User> users = userProvider.list(UserStatus.ACTIVE);
                for (User user : users) {
                    results.put(user.getId(), new ArrayDeque<>(createFakeJobs(user.getId(), 5)));
                }
            }
        return results;
    }

    @Override
    public Queue<ExecutionJobSummary> getQueuedJobs() {
        final Queue<ExecutionJobSummary> results = new ArrayDeque<>();
        if (!ExecutionConfiguration.developmentModeEnabled()) {
            final Queue<Tuple<Long, String>> jobs = Orchestrator.getInstance().getAllJobs();
            for (Tuple<Long, String> entry : jobs) {
                final ExecutionJob job = jobProvider.get(entry.getKeyOne());
                if (job == null) {
                    Orchestrator.getInstance().removeJob(entry.getKeyTwo(), entry.getKeyOne());
                    continue;
                }
                ExecutionJobSummary summary = new ExecutionJobSummary();
                summary.setId(job.getId());
                if (job.getWorkflowId() != null) {
                    summary.setWorkflowName(workflowService.findById(job.getWorkflowId()).getName());
                }
                summary.setJobName(job.getName());
                summary.setUserId(job.getUserId());
                summary.setJobStatus(job.getExecutionStatus());
                summary.setJobStart(job.getStartTime());
                summary.setJobEnd(job.getEndTime());
                results.offer(summary);
            }
        } else {
            final List<User> users = userProvider.list(UserStatus.ACTIVE);
            for (User user : users) {
                results.addAll(createFakeJobs(user.getId(), 5));
            }
        }
        return results;
    }

    @Override
    public Queue<ExecutionJobSummary> getQueuedJobs(String userId) {
        final Queue<ExecutionJobSummary> results = new ArrayDeque<>();
        if (!ExecutionConfiguration.developmentModeEnabled()) {
            final List<Long> jobs = Orchestrator.getInstance().getUserQueuedJobs(userId);
            final List<ExecutionJob> list = jobProvider.list(jobs);
            for (ExecutionJob job : list) {
                ExecutionJobSummary summary = new ExecutionJobSummary();
                summary.setId(job.getId());
                summary.setWorkflowName(workflowService.findById(job.getWorkflowId()).getName());
                summary.setJobName(job.getName());
                summary.setUserId(job.getUserId());
                summary.setJobStatus(job.getExecutionStatus());
                summary.setJobStart(job.getStartTime());
                summary.setJobEnd(job.getEndTime());
                results.offer(summary);
            }
        } else {
            results.addAll(createFakeJobs(userId, 5));
        }
        return results;
    }

    @Override
    public void moveJobUp(long jobId, String userId) {
        if (userId == null || userId.isBlank()) {
            Orchestrator.getInstance().moveJobToHead(jobId);
        } else {
            Orchestrator.getInstance().moveJobToHead(userId, jobId);
        }
    }

    @Override
    public void moveJobDown(long jobId, String userId) {
        if (userId == null || userId.isBlank()) {
            Orchestrator.getInstance().moveJobToTail(jobId);
        } else {
            Orchestrator.getInstance().moveJobToTail(userId, jobId);
        }
    }

    @Override
    public void deleteJobFromQueue(String userId, long jobId) throws PersistenceException {
        Orchestrator.getInstance().removeJob(userId, jobId);
        jobProvider.delete(jobId);
    }

    @Override
    public void clearJobHistory(String userId) throws PersistenceException {
        final Set<ExecutionStatus> statuses = new HashSet<>();
        Collections.addAll(statuses, ExecutionStatus.SUSPENDED, ExecutionStatus.DONE,
                           ExecutionStatus.FAILED, ExecutionStatus.CANCELLED);
        final List<ExecutionJob> jobs = jobProvider.list(userId, statuses);
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
                                                                                      callback);
            return job != null ? job.getId() : -1;
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
    }

    @Override
    public long invokeWPS(String identifier, Map<String, Map<String, String>> inputs) throws ExecutionException {
        try {
            final ExecutionJob job = Orchestrator.getInstance().invokeWPS(SessionStore.currentContext(), identifier, inputs);
            return job != null ? job.getId() : -1;
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
    }

    @Override
    public long invokeWMS(String identifier, Map<String, String> inputs) throws ExecutionException {
        try {
            final ExecutionJob job = Orchestrator.getInstance().invokeWMS(SessionStore.currentContext(), identifier, inputs);
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

    private long tryCreateTemporaryWorkflow(ExecutionRequest request) {
        final Map<Long, String> inputs = request.getInputs();
        if (inputs != null && !inputs.isEmpty()) {
            try {
                WorkflowDescriptor clone = workflowService.createTemporaryWorkflow(request.getWorkflowId(),
                                                                                   request.getName(),
                                                                                   inputs);
                return clone != null ? clone.getId() : 0;
            } catch (PersistenceException e) {
                throw new ExecutionException("Could not create cloned workflow. Reason: " + e.getMessage());
            }
        }
        return 0;
    }

    private List<ExecutionJobSummary> createFakeJobs(String userId, int count) {
        final List<ExecutionJobSummary> jobs = new ArrayList<>();
        final Random random = new Random();
        for (int i = 0; i < count; i++) {
            String suffix = userId.substring(0, 2) + count;
            ExecutionJobSummary summary = new ExecutionJobSummary();
            summary.setId(random.nextLong());
            summary.setWorkflowName("Fake workflow " + suffix);
            summary.setJobName("Fake job " + suffix);
            summary.setUserId(userId);
            summary.setJobStatus(ExecutionStatus.UNDETERMINED);
            summary.setJobStart(LocalDateTime.now().minusHours(count - i));
            jobs.add(summary);
        }
        return jobs;
    }
}
