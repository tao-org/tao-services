/**
 * 
 */
package ro.cs.tao.services.scheduling.job;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.PersistJobDataAfterExecution;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;
import org.springframework.security.core.context.SecurityContextHolder;

import ro.cs.tao.execution.model.ExecutionJob;
import ro.cs.tao.execution.model.ExecutionStatus;
import ro.cs.tao.orchestration.Orchestrator;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.scheduling.AbstractJob;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.bridge.spring.SpringContextBridge;

/**
 * @author Lucian Barbulescu
 *
 */
@PersistJobDataAfterExecution
public class WorkflowExecutionJob extends AbstractJob {

	/** The keys for the parameters used */
	public static final String JOB_ID_KEY = "taoJobId";
	public static final String WORKFLOW_ID_KEY = "workflowId";
	public static final String DESCRIPTION_KEY = "description";
	public static final String INPUTS_KEY = "inputs";
	public static final String ADDITIONAL_KEY = "additional";

	@Override
	public String groupName() {
		return SessionStore.currentContext().getPrincipal().getName();
	}

	@Override
	protected void executeImpl(JobDataMap dataMap, JobDetail jobDetail) {
		// check if this job has already a TAO job id associated
		final JobDataMap jobParams = jobDetail.getJobDataMap();
		if (jobParams.containsKey(JOB_ID_KEY)) {
			// a job was already started. Check if it is still running
			final long jobId = jobParams.getLongValue(JOB_ID_KEY);
			final ExecutionJob taoJob = persistenceManager.getJobById(jobId);
			if (taoJob != null && taoJob.getExecutionStatus().value() < ExecutionStatus.DONE.value()) {
				// the job is not done. do nothing
				logger.info("The job named " + jobDetail.getKey().getName() + " for user " + jobDetail.getKey().getGroup() + " is already running!");
				return;
			}
		}

		// send a new execution to the orchestrator
		final long workflowId = dataMap.getLongValue(WORKFLOW_ID_KEY);
		final String description = dataMap.getString(DESCRIPTION_KEY);
		final Map<String, Map<String, String>> inputs = (Map<String, Map<String, String>>)dataMap.get(INPUTS_KEY);
		final Map<String, Map<String, String>> additional = (Map<String, Map<String, String>>)dataMap.get(ADDITIONAL_KEY);
		final long jobId = Orchestrator.getInstance().startWorkflow(workflowId, description, new HashMap<String, Map<String,String>>(inputs),
                                                        new DelegatingSecurityContextExecutorService(Executors.newFixedThreadPool(2),
                                                                                                     SecurityContextHolder.getContext()));
		
		// prepare the inputs for the next run
		if (additional != null) {
			updateInputs(inputs, additional);
		}
		
		
		// save the job id to the job details map
		jobDetail.getJobDataMap().put(JOB_ID_KEY, jobId);
		logger.info("Started job " + jobId);
	}

	@Override
	protected PersistenceManager persistenceManager() {
		return SpringContextBridge.services().getService(PersistenceManager.class);
	}

	private void updateInputs(final Map<String, Map<String, String>> inputs, final Map<String, Map<String, String>> additional) {
		// TODO: decide how to perform the update
	}
}
