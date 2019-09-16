package ro.cs.tao.services.scheduling.service;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.scheduling.AbstractJob;
import ro.cs.tao.scheduling.JobData;
import ro.cs.tao.scheduling.JobDescriptor;
import ro.cs.tao.scheduling.ScheduleManager;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.interfaces.SchedulingService;
import ro.cs.tao.services.model.scheduling.SchedulingInfo;
import ro.cs.tao.services.model.scheduling.SchedulingMode;
import ro.cs.tao.services.scheduling.job.WorkflowExecutionJob;
import ro.cs.tao.workflow.WorkflowDescriptor;

@Service("schedulingService")
public class SchedulingServiceImpl implements SchedulingService {

    @Autowired
    private PersistenceManager persistenceManager;
    
	@Override
	public List<SchedulingInfo> listUserSchedules() {
		final Principal principal = SessionStore.currentContext().getPrincipal();
		final List<SchedulingInfo> list = new LinkedList<>();
		final List<JobData> jobs = ScheduleManager.getUserSchedules(principal.getName());
		for (JobData job : jobs) {
			final JobDetail jobDetail = job.getJobDetail();
			final Trigger trigger = job.getTrigger();
			final TriggerState triggerState = job.getTriggerState();
			final JobDataMap jobDataMap = jobDetail.getJobDataMap();
			
			final SchedulingInfo info = new SchedulingInfo();
			
			// set the data
			info.setId(jobDetail.getKey().getName());
			info.setFriendlyName(jobDataMap.getString(AbstractJob.FRIENDLY_NAME_KEY));
			info.setMode((SchedulingMode)jobDataMap.get(WorkflowExecutionJob.EXECUTION_MODE_KEY));
			info.setState(triggerState == null ? TriggerState.NONE.name() : triggerState.name());
			
			// build a job descriptor from the trigger
			final JobDescriptor descriptor = JobDescriptor.buildDescriptor(trigger);
			info.setRepeatInterval(descriptor.getRepeatInterval());
			info.setStartTime(descriptor.getFireTime());
			
			// set the workflow related data
			final long workflowId = jobDataMap.getLong(WorkflowExecutionJob.WORKFLOW_ID_KEY); 
			info.setWorkflowId(workflowId);
			final WorkflowDescriptor workflowDescriptor = persistenceManager.getWorkflowDescriptor(workflowId);
			info.setWorkflowName(workflowDescriptor.getName());
			
			// set the list of job ids associated with the schedule
			info.setJobIds((List<Long>)jobDataMap.get(WorkflowExecutionJob.JOB_ID_KEY));
			
			list.add(info);
		}
		return list;
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
