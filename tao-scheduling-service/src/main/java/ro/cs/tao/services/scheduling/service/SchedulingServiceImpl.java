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
import org.springframework.stereotype.Service;

import ro.cs.tao.scheduling.AbstractJob;
import ro.cs.tao.scheduling.ScheduleManager;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.interfaces.SchedulingService;
import ro.cs.tao.services.model.scheduling.SchedulingInfo;
import ro.cs.tao.services.scheduling.job.WorkflowExecutionJob;

@Service("schedulingService")
public class SchedulingServiceImpl implements SchedulingService {

	@Override
	public List<SchedulingInfo> listUserSchedules() {
		final Principal principal = SessionStore.currentContext().getPrincipal();
		final List<SchedulingInfo> list = new LinkedList<>();
		final List<JobDetail> jobs = ScheduleManager.getUserSchedules(principal.getName());
		for (JobDetail job : jobs) {
			final JobDataMap jobDataMap = job.getJobDataMap();
			final SchedulingInfo info = new SchedulingInfo();
			info.setFriendlyName(jobDataMap.getString(AbstractJob.FRIENDLY_NAME_KEY));
			info.setId(job.getKey().getName());
			info.setState(jobDataMap.getString(ScheduleManager.JOB_STATUS_KEY));
			jobDataMap.remove(ScheduleManager.JOB_STATUS_KEY);
			list.add(info);
		}
		return list;
	}

	@Override
	public String addExecutionSchedule(String name, LocalDateTime startTime, int repeatInterval, long workflowId,
			Map<String, Map<String, String>> inputs, Map<String, Map<String, String>> additional) {
		
		final Map<String, Object> parameters = new HashMap<>();
		parameters.put(WorkflowExecutionJob.WORKFLOW_ID_KEY, workflowId);
		parameters.put(WorkflowExecutionJob.DESCRIPTION_KEY, name);
		parameters.put(WorkflowExecutionJob.INPUTS_KEY, inputs);
		parameters.put(WorkflowExecutionJob.ADDITIONAL_KEY, additional);
		parameters.put(AbstractJob.FRIENDLY_NAME_KEY, name);
		
		WorkflowExecutionJob job = new WorkflowExecutionJob();

		// schedule with random id
		final JobKey key = ScheduleManager.schedule(job, UUID.randomUUID().toString(), startTime, repeatInterval, parameters);
		
		return key.getName();
	}

	@Override
	public String updateExecutionSchedule(String id, String name, LocalDateTime startTime, int repeatInterval, long workflowId,
			Map<String, Map<String, String>> inputs, Map<String, Map<String, String>> additional) {
		
		final Map<String, Object> parameters = new HashMap<>();
		parameters.put(WorkflowExecutionJob.WORKFLOW_ID_KEY, workflowId);
		parameters.put(WorkflowExecutionJob.DESCRIPTION_KEY, name);
		parameters.put(WorkflowExecutionJob.INPUTS_KEY, inputs);
		parameters.put(WorkflowExecutionJob.ADDITIONAL_KEY, additional);
		parameters.put(AbstractJob.FRIENDLY_NAME_KEY, name);
		
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
