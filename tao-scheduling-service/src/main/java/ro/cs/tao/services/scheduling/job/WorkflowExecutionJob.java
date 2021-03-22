package ro.cs.tao.services.scheduling.job;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.PersistJobDataAfterExecution;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import ro.cs.tao.execution.model.ExecutionJob;
import ro.cs.tao.execution.model.ExecutionStatus;
import ro.cs.tao.orchestration.Orchestrator;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.scheduling.AbstractJob;
import ro.cs.tao.security.SessionContext;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.bridge.spring.SpringContextBridge;
import ro.cs.tao.services.model.scheduling.SchedulingMode;
import ro.cs.tao.user.UserPreference;
import ro.cs.tao.utils.DateUtils;

import java.security.Principal;
import java.text.DateFormat;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Executors;

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
	public static final String EXECUTION_MODE_KEY = "executionMode";

	@Override
	public String groupName() {
		return SessionStore.currentContext().getPrincipal().getName();
	}

	@Override
	protected void executeImpl(JobDataMap dataMap, JobDetail jobDetail) {
		
		final Authentication userAuthentication = (Authentication) dataMap.get(AbstractJob.USER_AUTHENTICATION_KEY);
		SessionContext context = new SessionContext() {
            @Override
            protected Principal setPrincipal() {
                return userAuthentication;
            }

            @Override
            protected List<UserPreference> setPreferences() {
                try {
                    return persistenceManager != null ?
                            persistenceManager.getUserPreferences(getPrincipal().getName()) : null;
                } catch (PersistenceException e) {
                    logger.severe(e.getMessage());
                }
                return null;
            }

            @Override
            public int hashCode() {
                return getPrincipal() != null ? getPrincipal().getName().hashCode() : super.hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                if (!(obj instanceof SessionContext)) {
                    return false;
                }
                SessionContext other = (SessionContext) obj;
                return (this.getPrincipal() == null && other.getPrincipal() == null) ||
                        (this.getPrincipal().getName().equals(other.getPrincipal().getName()));
            }
        };
		
        final SecurityContext securityContext = new SecurityContextImpl(userAuthentication); 
        SecurityContextHolder.setContext(securityContext);
        
		final JobDataMap jobParams = jobDetail.getJobDataMap();
		final List<Long> jobIds;
        if (!jobParams.containsKey(JOB_ID_KEY)) {
        	// initialise an empty list because this is the first time the job is executed
        	jobIds = new ArrayList<Long>();
        } else {
        	// get the current list
        	jobIds = (List<Long>) jobParams.get(JOB_ID_KEY); 
        }
        
		final Map<String, Map<String, String>> inputs;
		// check if this job has already a TAO job id associated
		if (!jobIds.isEmpty()) {
			// a job was already started. Check if it is still running
			// get last job id.
 			final long jobId = jobIds.get(jobIds.size() - 1);
			final ExecutionJob taoJob = persistenceManager.getJobById(jobId);
			if (taoJob != null && taoJob.getExecutionStatus().value() < ExecutionStatus.DONE.value()) {
				// the job is not done. do nothing
				logger.info("The job named " + jobDetail.getKey().getName() + " for user " + jobDetail.getKey().getGroup() + " is already running!");
				return;
			}
			
			// retrieve the parameters
			inputs = (Map<String, Map<String, String>>)dataMap.get(INPUTS_KEY);
			
			// check if the execution mode required an update of the input parameters
			final SchedulingMode mode = (SchedulingMode) dataMap.get(EXECUTION_MODE_KEY);
			switch(mode) {
				case INCREMENTAL:
					// Update the startDate value from the inputs
					updateInputs(userAuthentication.getName(), inputs);
					break;
				case NORMAL:
				default:
					// Nothing to do
					break;
			}
		} else {
			// just retrieve the parameters
			inputs = (Map<String, Map<String, String>>)dataMap.get(INPUTS_KEY);
		}

		// send a new execution to the orchestrator
		final long workflowId = dataMap.getLongValue(WORKFLOW_ID_KEY);
		final String description = dataMap.getString(DESCRIPTION_KEY);
		
		
		final long jobId = Orchestrator.getInstance().startWorkflow(context, workflowId, description, new HashMap<String, Map<String,String>>(inputs),
                                                        new DelegatingSecurityContextExecutorService(
                                                        		Executors.newFixedThreadPool(2), 
                                                        		SecurityContextHolder.getContext()));
		
		// save the job id to the job details map
		jobIds.add(jobId);
		jobDetail.getJobDataMap().put(JOB_ID_KEY, jobIds);
		logger.info("Started job " + jobId);
	}

	@Override
	protected PersistenceManager persistenceManager() {
		return SpringContextBridge.services().getService(PersistenceManager.class);
	}

	private void updateInputs(final String userName, final Map<String, Map<String, String>> inputs) {
		
		// look for the nodes that define a start date
		for (final Entry<String,Map<String, String>> nodeData : inputs.entrySet()) {
			if (nodeData.getValue().containsKey("startDate")) {
				// get the footprint
				String footprint = nodeData.getValue().get("footprint");
				if (footprint == null) {
					footprint = "";
				}
				
				// get the date of the last product
				Date date = persistenceManager.getNewestProductDateForUser(userName, footprint);
				if (date == null) {
					// no product defined for the user. Continue with existing values
					continue;
				}
				
				// shift the date with one day into the future
				date = Date.from(date.toInstant().plus(1, ChronoUnit.DAYS));
				final DateFormat df = DateUtils.getFormatterAtLocal("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
				
				nodeData.getValue().put("startDate", df.format(date));
				// update endDate to today if it was already defined
				if (nodeData.getValue().containsKey("endDate")) {
					nodeData.getValue().put("endDate", df.format(new Date()));
				}
			}
		}
	}
}
