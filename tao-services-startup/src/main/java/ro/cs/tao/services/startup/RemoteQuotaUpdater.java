/**
 * 
 */
package ro.cs.tao.services.startup;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;

import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.configuration.ConfigurationProvider;
import ro.cs.tao.execution.model.ResourceUsage;
import ro.cs.tao.execution.model.ResourceUsageReport;
import ro.cs.tao.execution.persistence.ResourceUsageProvider;
import ro.cs.tao.execution.persistence.ResourceUsageReportProvider;
import ro.cs.tao.persistence.AuditProvider;
import ro.cs.tao.persistence.UserProvider;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.topology.TopologyException;
import ro.cs.tao.topology.openstack.SwiftService;
import ro.cs.tao.user.SessionDuration;
import ro.cs.tao.user.User;
import ro.cs.tao.utils.CloseableHttpResponse;
import ro.cs.tao.utils.HttpMethod;
import ro.cs.tao.utils.NetUtils;

/**
 * Periodically inspect the remote quota and save it to the keyclock profile.
 * 
 * @author Lucian Barbulescu
 */
public class RemoteQuotaUpdater extends BaseLifeCycle {

	/** Pattern to recognize a GUID with the form hhhhhhhh-hhhh-hhhh-hhhh-hhhhhhhhhhhh. */
	private static final Pattern GUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}\\-(?:[0-9a-fA-F]{4}\\-){3}[0-9a-fA-F]{12}$");

	/** Template for the JSON data transmitted (including container size). */
	private final static String TEMPLATE_JSON_FULL = "{\"user_id\":\"%s\",\"usage_secs\":%d,\"usage_storage\":%d}";

	/** Template for the JSON data transmitted (without container size). */
	private final static String TEMPLATE_JSON_LITE = "{\"user_id\":\"%s\",\"usage_secs\":%d}";
	
	/** The periodic task. */
	private UpdateQuotaTask periodicTask = new UpdateQuotaTask();

	/** The timer used to run the periodic task. */
	private Timer timer = new Timer();
	
	/**	OpenStack swift service */
	private final SwiftService swiftService = new SwiftService();
	
    @Autowired
    private AuditProvider auditProvider;
    @Autowired
    private UserProvider userProvider;
    @Autowired
    private ResourceUsageProvider resourceUsageProvider;
    @Autowired
    private ResourceUsageReportProvider resourceUsageReportProvider;
	
	
//	/**	Keyclock client. */
//	private final KeycloakClient keyclockClient = new KeycloakClient();
	
//	/** Class to format the container size as a string. */
//	private final NumberFormat containerSizeFormat = DecimalFormat.getInstance();
	
	@Override
	public int priority() {
		// lowest priority task
		return 100;
	}

	@Override
	public void onStartUp() {
		// configure the representation of the container size
//		containerSizeFormat.setMinimumFractionDigits(2);
//		containerSizeFormat.setMaximumFractionDigits(2);
//		containerSizeFormat.setRoundingMode(RoundingMode.DOWN);
		
		// get the configured execution rate, in hours
        final String executionRateStr = ConfigurationManager.getInstance().getValue("tao.remote.quota.execution.rate", "1");
        // compute the execution rate, in miliseconds.
        long executionRate;
        try {
        	executionRate = Long.parseLong(executionRateStr) * 3600000;
        } catch (NumberFormatException ex) {
        	// add warning to the log 
        	logger.warning("The value " + executionRateStr + " provided to the property tao.remote.quota.execution.rate is not valid. Using the default value 1.");
        	// fall back to the 1 hour delay
        	executionRate = 3600000;
        }
        
        // run the task
        this.timer.scheduleAtFixedRate(periodicTask, 0, executionRate);
	}

	@Override
	public void onShutdown() {
		// cancel the timer
		this.timer.cancel();
	}

	
	/**
	 * Periodic task
	 */
	private class UpdateQuotaTask extends TimerTask {

//		/** One Gigabyte. */
//		private static final double GIGABYTES = 1000000000.0;
//
//		/** The label for the container size attribute. */
//		private static final String CONTAINER_SIZE = "container.size";
		
		@Override
		public void run() {
			// Get the list of registered users
			// TODO: Filter out INTERNAL users ?
			final List<User> users = userProvider.list();
			
			for (User user : users) {
				// get the user ID
				final String userId = user.getId();
				
                final SessionDuration sessionDuration = auditProvider.getLastUserSession(userId);
                final ResourceUsageReport report = resourceUsageReportProvider.getByUserId(userId);
                int processingTime = 0;
                final List<ResourceUsage> usages;
                if (report != null) {
                    usages = resourceUsageProvider.getByUserIdSince(userId, report.getLastReportTime());
                } else {
                    usages = resourceUsageProvider.getByUserId(userId);
                }
                if (usages != null) {
                    for (ResourceUsage usage : usages) {
                        processingTime += (int) Duration.between(usage.getStartTime(),
                                                                 usage.getEndTime() != null
                                                                    ? usage.getEndTime()
                                                                    : LocalDateTime.now()).toSeconds();
                    }
                }
                
                // get the user's storage
                long containerSize = -1;
                try {
					containerSize = swiftService.getContainerSize(userId);
    			} catch (IOException e) {
					logger.severe("Error reading the container size. Received message: " + e.getMessage());
				} catch (TopologyException e) {
					logger.fine("OpenStack authentication error. Received message: " + e.getMessage());
				}

                // update user's data
                updateUserData(userId, BaseController.tokenOf(userId), sessionDuration, processingTime, containerSize);
			}
			
//			// get the remote container sizes
//			final Map<String, Long> containerSizes = getUserContainerSizes();
//			if (containerSizes == null) {
//				// There was an error getting the container size. Finish current execution.
//				return;
//			} else if (containerSizes.size() == 0) {
//				logger.fine("There are no user containers defined!");
//				return;
//			}
//			
//			// update the keyclock profiles
//			containerSizes.entrySet().stream().forEach(e -> updateUserContainerSize(e.getKey(), e.getValue()));
		}
		
	    private void updateUserData(String userId, String token, SessionDuration sessionDuration, int processingTime, long usedStorage) {
	        final ConfigurationProvider cfgProvider = ConfigurationManager.getInstance();
	        final int includeSessionTime = cfgProvider.getBooleanValue("quota.include.session.time") ? 1 : 0;
	        final String serviceAPIUrl = cfgProvider.getValue("dunia.service.api.url", "https://dunia.esa.int/api/quota/tao");
	        final String serviceAPIHeader = cfgProvider.getValue("dunia.service.api.key");
	        final String header;
	        if (serviceAPIHeader != null) {
	            final String[] items = serviceAPIHeader.split(":");
	            header = items[0];
	            token = items[1];
	        } else {
	            header = "X-Auth-Token";
	        }
	        final String json;
	        if (usedStorage == -1) {
	        	json = String.format(TEMPLATE_JSON_LITE, userId, includeSessionTime * sessionDuration.getDuration() + processingTime);
	        } else {
	        	json = String.format(TEMPLATE_JSON_FULL, userId, includeSessionTime * sessionDuration.getDuration() + processingTime, usedStorage);
	        }
	        	
	        try (CloseableHttpResponse response = NetUtils.openConnection(HttpMethod.POST, serviceAPIUrl, header, token, json)) {
	            if (response.getStatusLine().getStatusCode() > 201) {
	                throw new IOException(EntityUtils.toString(response.getEntity()));
	            }
	        } catch (Exception e) {
	            logger.severe(e.getMessage());
	        }
	    }

		
//		/**
//		 * Update the container size attribute ro the keyclock repository.
//		 * 
//		 * @param userGUID the user unique identifier 
//		 * @param containerSize the size of the container, in GB.
//		 */
//		private void updateUserContainerSize(final String userGUID, final long containerSize) {
//			
//			
//			
//			// add the attribute to the map 
//			sizeAttributeMap.put(CONTAINER_SIZE, Arrays.asList(containerSizeStr));
//			
////			// update the keyclock profile
////			keyclockClient.updateUserProfileAttributes(userGUID, sizeAttributeMap);
//		}
//		
//		/**
//		 * Get the sizes for the containers that belong to users.
//		 * 
//		 * @return the sizes for the containers that belong to users
//		 */
//		private Map<String, Long> getUserContainerSizes() {
//			final Map<String, Long> containerSizes;
//			if (ExecutionConfiguration.developmentModeEnabled()) {
//				// Create some dummy data.
//				containerSizes = new HashMap<>();
//				containerSizes.put("test-tao", 0l);
//				containerSizes.put("dunia-dev-monitoring", 386l);
//				containerSizes.put("4c875165-678e-48f7-bd3b-cbb7556c4ece", 10456374823l);
//			} else {
//				try {
//					// Get the sizes from the server
//					containerSizes = swiftService.getContainerSizes();
//				} catch (IOException e) {
//					logger.severe("Error reading the container sizes. Received message: " + e.getMessage());
//					return null;
//				} catch (TopologyException e) {
//					logger.fine("OpenStack authentication error. Received message: " + e.getMessage());
//					return null;
//				}
//			}
//			
//			// Filter out the containers which are not GUIDs (not belonging to a user)
//			return containerSizes.entrySet()
//					.stream().filter(e -> GUID_PATTERN.matcher(e.getKey()).matches())
//					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
//		}
		
	}
	
}
