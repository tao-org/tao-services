package ro.cs.tao.services.scheduling.service;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import ro.cs.tao.execution.model.ExecutionJob;
import ro.cs.tao.execution.persistence.ExecutionJobProvider;
import ro.cs.tao.persistence.WorkflowProvider;
import ro.cs.tao.scheduling.AbstractJob;
import ro.cs.tao.scheduling.JobData;
import ro.cs.tao.scheduling.JobDescriptor;
import ro.cs.tao.scheduling.ScheduleManager;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.interfaces.SchedulingService;
import ro.cs.tao.services.model.scheduling.JobInfo;
import ro.cs.tao.services.model.scheduling.SchedulingInfo;
import ro.cs.tao.services.model.scheduling.SchedulingMode;
import ro.cs.tao.services.scheduling.job.WorkflowExecutionJob;
import ro.cs.tao.workflow.WorkflowDescriptor;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;

@Service("schedulingService")
public class SchedulingServiceImpl implements SchedulingService {

    @Autowired
    private WorkflowProvider workflowProvider;
    @Autowired
	private ExecutionJobProvider jobProvider;
    
	@Override
	public List<SchedulingInfo> listUserSchedules() {
		try {
			final Principal principal = SessionStore.currentContext().getPrincipal();
			final List<SchedulingInfo> list = new LinkedList<>();
			final List<JobData> jobsData = ScheduleManager.getUserSchedules(principal.getName());
			for (JobData jobData : jobsData) {
				final JobDetail jobDetail = jobData.getJobDetail();
				final Trigger trigger = jobData.getTrigger();
				final TriggerState triggerState = jobData.getTriggerState();
				final JobDataMap jobDataMap = jobDetail.getJobDataMap();
				
				final SchedulingInfo info = new SchedulingInfo();
				
				// set the data
				info.setId(jobDetail.getKey().getName());
				info.setFriendlyName(jobDataMap.getString(AbstractJob.FRIENDLY_NAME_KEY));
				info.setMode((SchedulingMode)jobDataMap.get(WorkflowExecutionJob.EXECUTION_MODE_KEY));
				info.setState(triggerState == null ? TriggerState.NONE.name() : triggerState.name());
				info.setParameters((Map<String, Map<String, String>>)jobDataMap.get(WorkflowExecutionJob.INPUTS_KEY));
				
				// build a job descriptor from the trigger
				final JobDescriptor descriptor = JobDescriptor.buildDescriptor(trigger);
				info.setRepeatInterval(descriptor.getRepeatInterval());
				info.setStartTime(descriptor.getFireTime());
				
				// set the workflow related data
				final long workflowId = jobDataMap.getLong(WorkflowExecutionJob.WORKFLOW_ID_KEY); 
				info.setWorkflowId(workflowId);
				final WorkflowDescriptor workflowDescriptor = workflowProvider.get(workflowId);
				info.setWorkflowName(workflowDescriptor.getName());
				
				final List<Long> jobIds =(List<Long>)jobDataMap.get(WorkflowExecutionJob.JOB_ID_KEY);
				final List<JobInfo> jobs = new LinkedList<JobInfo>();
				if (jobIds != null) {
					for (Long jobId : jobIds) {
						final ExecutionJob job = jobProvider.get(jobId);
						final JobInfo jobInfo = new JobInfo();

						jobInfo.setJobId(jobId);
						jobInfo.setName(job.getName());
						jobInfo.setStartTime(job.getStartTime());
						jobInfo.setEndTime(job.getEndTime());
						jobInfo.setStatus(job.getExecutionStatus());

						jobs.add(jobInfo);
					}
				}
				
				// set the list of jobs associated with the schedule
				info.setJobs(jobs);
				
				list.add(info);
			}
			return list;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

	}

	@Override
	public String addExecutionSchedule(String name, LocalDateTime startTime, int repeatInterval, long workflowId,
			Map<String, Map<String, String>> inputs, final SchedulingMode mode) {
		
		final Map<String, Object> parameters = new HashMap<>();
		parameters.put(WorkflowExecutionJob.WORKFLOW_ID_KEY, workflowId);
		parameters.put(WorkflowExecutionJob.DESCRIPTION_KEY, name);
		parameters.put(WorkflowExecutionJob.INPUTS_KEY, inputs);
		parameters.put(WorkflowExecutionJob.EXECUTION_MODE_KEY, mode);
		parameters.put(AbstractJob.FRIENDLY_NAME_KEY, name);
		parameters.put(AbstractJob.USER_AUTHENTICATION_KEY, SecurityContextHolder.getContext().getAuthentication());
		
		WorkflowExecutionJob job = new WorkflowExecutionJob();

		// schedule with random id
		final JobKey key = ScheduleManager.schedule(job, UUID.randomUUID().toString(), startTime, repeatInterval, parameters);
		
		return key.getName();
	}

	@Override
	public String updateExecutionSchedule(String id, String name, LocalDateTime startTime, int repeatInterval, long workflowId,
			Map<String, Map<String, String>> inputs, final SchedulingMode mode) {
		
		final Map<String, Object> parameters = new HashMap<>();
		parameters.put(WorkflowExecutionJob.WORKFLOW_ID_KEY, workflowId);
		parameters.put(WorkflowExecutionJob.DESCRIPTION_KEY, name);
		parameters.put(WorkflowExecutionJob.INPUTS_KEY, inputs);
		parameters.put(WorkflowExecutionJob.EXECUTION_MODE_KEY, mode);
		parameters.put(AbstractJob.FRIENDLY_NAME_KEY, name);
		parameters.put(AbstractJob.USER_AUTHENTICATION_KEY, SecurityContextHolder.getContext().getAuthentication());
		
		WorkflowExecutionJob job = new WorkflowExecutionJob();

		// schedule with existing id
		final JobKey key = ScheduleManager.schedule(job, id, startTime, repeatInterval, parameters);
		
		return key.getName();
	}

	
	@Override
	public boolean removeExecutionSchedule(String scheduleID) {
		final Principal principal = SessionStore.currentContext().getPrincipal();
		final JobKey key = new JobKey(scheduleID, principal.getName());
		return ScheduleManager.remove(key);
	}

	@Override
	public void pauseExecutionSchedule(String scheduleID) {
		final Principal principal = SessionStore.currentContext().getPrincipal();
		final JobKey key = new JobKey(scheduleID, principal.getName());
		ScheduleManager.pause(key);
	}

	@Override
	public void resumeExecutionSchedule(String scheduleID) {
		final Principal principal = SessionStore.currentContext().getPrincipal();
		final JobKey key = new JobKey(scheduleID, principal.getName());
		ScheduleManager.resume(key);
	}
	
}
