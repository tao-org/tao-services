package ro.cs.tao.services.scheduling.job;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.PersistJobDataAfterExecution;
import ro.cs.tao.execution.model.ExecutionJob;
import ro.cs.tao.execution.model.ExecutionRequest;
import ro.cs.tao.orchestration.Orchestrator;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.scheduling.AbstractJob;
import ro.cs.tao.security.SessionContext;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.security.UserPrincipal;
import ro.cs.tao.services.bridge.spring.SpringContextBridge;
import ro.cs.tao.services.model.scheduling.SchedulingMode;
import ro.cs.tao.user.UserPreference;
import ro.cs.tao.utils.DateUtils;

import java.security.Principal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Lucian Barbulescu
 *
 */
@PersistJobDataAfterExecution
public class WorkflowExecutionJob extends AbstractJob {

	/** The keys for the parameters used */
	public static final String BATCH_ID_KEY = "taoBatchId";
	public static final String WORKFLOW_ID_KEY = "workflowId";
	public static final String DESCRIPTION_KEY = "description";
	public static final String INPUTS_KEY = "inputs";
	public static final String EXECUTION_MODE_KEY = "executionMode";
	public static final String USER_ID_KEY = "userId";

	@Override
	public String groupName() {
		return SessionStore.currentContext().getPrincipal().getName();
	}

	@Override
	protected void executeImpl(JobDataMap dataMap, JobDetail jobDetail) {
		
		//final Authentication userAuthentication = (Authentication) dataMap.get(AbstractJob.USER_AUTHENTICATION_KEY);
		final String userId = dataMap.getString(WorkflowExecutionJob.USER_ID_KEY);
		SessionContext context = new SessionContext() {
            @Override
            public Principal setPrincipal(Principal principal) {
                //return userAuthentication;
            	return new UserPrincipal(userId);
            }

            @Override
            protected List<UserPreference> setPreferences() {
				return persistenceManager != null ?
						persistenceManager.users().listPreferences(getPrincipal().getName()) : null;
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
		
//        final SecurityContext securityContext = new SecurityContextImpl(userAuthentication); 
//        SecurityContextHolder.setContext(securityContext);
        
		final JobDataMap jobParams = jobDetail.getJobDataMap();
		final List<String> batchIds;
        if (!jobParams.containsKey(BATCH_ID_KEY)) {
        	// create an empty list
        	batchIds = new ArrayList<>();
        } else {
        	// get the list of batch ids
        	batchIds = (List<String>) jobParams.get(BATCH_ID_KEY); 
        }
        
		final Map<String, Map<String, String>> inputs;
		// check if this job has already started once
		if (!batchIds.isEmpty()) {
			// get the last batch id
			final String batchId = batchIds.get(batchIds.size() - 1);
			
			// check if any job associated with this batch is still running
			if (persistenceManager.jobs().isBatchRunning(batchId)) {
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
					//updateInputs(userAuthentication.getName(), inputs);
					updateInputs(userId, inputs);
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

		final Orchestrator orchestrator = Orchestrator.getInstance();
		final ExecutionRequest request = new ExecutionRequest();
		request.setWorkflowId(workflowId);
		request.setName(description);
		request.setLabel(description);
		request.setParameters(new HashMap<>(inputs));
		final ExecutionJob job = orchestrator.startWorkflow(context, request);
		
		// save the job id to the job details map
		if (job != null) {
			batchIds.add(job.getBatchId());
			jobDetail.getJobDataMap().put(BATCH_ID_KEY, batchIds);
			logger.info("Started job " + job.getId());
		} else {
			throw new RuntimeException("No job was created");
		}
	}

	@Override
	protected PersistenceManager persistenceManager() {
		return SpringContextBridge.services().getService(PersistenceManager.class);
	}

	private void updateInputs(final String userId, final Map<String, Map<String, String>> inputs) {
		
		// look for the nodes that define a start date
		for (final Entry<String,Map<String, String>> nodeData : inputs.entrySet()) {
			if (nodeData.getValue().containsKey("startDate")) {
				// get the footprint
				String footprint = nodeData.getValue().get("footprint");
				if (footprint == null) {
					footprint = "";
				}
				
				// get the date of the last product
				LocalDateTime date = persistenceManager.rasterData().getNewestProductDateForUser(userId, footprint);
				if (date == null) {
					// no product defined for the user. Continue with existing values
					continue;
				}
				
				// shift the date with one day into the future
				date = date.plusDays(1);
				final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
				
				nodeData.getValue().put("startDate", DateUtils.getFormatterAtLocal("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(date));
				// update endDate to today if it was already defined
				if (nodeData.getValue().containsKey("endDate")) {
					nodeData.getValue().put("endDate", df.format(LocalDateTime.now()));
				}
			}
		}
	}
}
