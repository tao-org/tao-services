/**
 * 
 */
package ro.cs.tao.services.startup;

import org.apache.http.util.EntityUtils;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.configuration.ConfigurationProvider;
import ro.cs.tao.execution.model.ResourceUsage;
import ro.cs.tao.execution.model.ResourceUsageReport;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.TransactionalMethod;
import ro.cs.tao.topology.openstack.SwiftService;
import ro.cs.tao.user.SessionDuration;
import ro.cs.tao.user.User;
import ro.cs.tao.user.UserStatus;
import ro.cs.tao.utils.CloseableHttpResponse;
import ro.cs.tao.utils.HttpMethod;
import ro.cs.tao.utils.NetUtils;
import ro.cs.tao.utils.StringUtilities;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Periodically inspect the remote quota and save it to the keyclock profile.
 * 
 * @author Lucian Barbulescu
 */
public class RemoteQuotaUpdater extends BaseLifeCycle {

	/** Pattern to recognize a GUID with the form hhhhhhhh-hhhh-hhhh-hhhh-hhhhhhhhhhhh. */
	private static final Pattern GUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}\\-(?:[0-9a-fA-F]{4}\\-){3}[0-9a-fA-F]{12}$");

	/** Template for the user data transmitted (including container size). */
	private final static String JSON_USER = "{\"user_id\":\"%s\",\"usage_secs\":%d,\"usage_storage\":%d}";

	/** Template for the batch data transmitted. */
	private final static String JSON_LIST = "{\"quota_list\":[%s]}";

	/** The periodic task. */
	private final UpdateQuotaTask periodicTask = new UpdateQuotaTask();

	/** The timer used to run the periodic task. */
	private final Timer timer = new Timer();
	
	/**	OpenStack swift service */
	private final SwiftService swiftService = new SwiftService();

	private String serviceAPIUrl;

	@Override
	public int priority() {
		// lowest priority task
		return 100;
	}

	@Override
	public void onStartUp() {
		// get the configured execution rate, in hours
        final String executionRateStr = ConfigurationManager.getInstance().getValue("tao.remote.quota.execution.rate", "1");
        // compute the execution rate, in milliseconds.
        long executionRate;
        try {
        	executionRate = Long.parseLong(executionRateStr) * 3600000;
        } catch (NumberFormatException ex) {
        	// add warning to the log 
        	logger.warning(() -> "The value " + executionRateStr + " provided to the property tao.remote.quota.execution.rate is not valid. Using the default value 1.");
        	// fall back to the 1-hour delay
        	executionRate = 3600000;
        }
		this.serviceAPIUrl = ConfigurationManager.getInstance().getValue("dunia.service.api.url");
		if (!StringUtilities.isNullOrEmpty(this.serviceAPIUrl)) {
			// run the task
			this.timer.scheduleAtFixedRate(periodicTask, 0, executionRate);
		}
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

		@Override
		public void run() {
			final List<User> users = new ArrayList<>();
			// Get the list of registered users that have quota enabled
			try {
				TransactionalMethod.withExceptionType(PersistenceException.class).execute(() -> {
					users.addAll(persistenceManager.users().list(UserStatus.ACTIVE));
				});
			} catch (PersistenceException e) {
				logger.severe(() -> "Error reading the users list. Received message: " + e.getMessage());
				// stop the execution
				return;
			}
			
			// Get all remote container sizes
			Map<String, Long> userContainerSizes = null;
			try {
				userContainerSizes = swiftService.getContainerSizes();
			} catch (IOException e) {
				logger.severe(() -> "Error reading the container sizes. Received message: " + e.getMessage());
				// stop the execution because no container size can be determined,
				// thus nothing can be uploaded
				return;
			}
			final List<String> userData = new ArrayList<>();
			final int includeSessionTime = ConfigurationManager.getInstance().getBooleanValue("quota.include.session.time") ? 1 : 0;
			for (User user : users) {
				// get the user ID
				final String userId = user.getId();
				
				// check if a container size was determined for this user
				if (userContainerSizes == null || !userContainerSizes.containsKey(userId)) {
					// go to next user
					logger.fine(() -> String.format("User %s has no storage, skipping", userId));
					continue;
				}
				
                final SessionDuration sessionDuration = persistenceManager.audit().getLastUserSession(userId);
                final ResourceUsageReport report = persistenceManager.resourceUsageReport().getByUserId(userId);
                int processingTime = 0;
                final List<ResourceUsage> usages;
                if (report != null) {
                    usages = persistenceManager.resourceUsage().getByUserIdSince(userId, report.getLastReportTime());
                } else {
                    usages = persistenceManager.resourceUsage().getByUserId(userId);
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
                long containerSize = userContainerSizes.get(userId);

                // update batch data
				processingTime += includeSessionTime * (sessionDuration != null ? sessionDuration.getDuration() : 0);
				// Either one must be non-zero
				if (processingTime > 0 || containerSize > 0) {
					userData.add(String.format(JSON_USER, userId, processingTime, containerSize));
					logger.fine(String.format("User %s has %dMB storage and %ds processing time",
											  userId, containerSize >> 20, processingTime));
				} else {
					logger.finest(() -> String.format("User %s has no storage and/or no processing time, skipping", userId));
				}
			}
			updateBatch(userData);
		}
		
	    private void updateUserData(String userId, String token, SessionDuration sessionDuration, int processingTime, long usedStorage) {
	        final ConfigurationProvider cfgProvider = ConfigurationManager.getInstance();
			final String serviceAPIHeader = cfgProvider.getValue("dunia.service.api.key");
			final int includeSessionTime = cfgProvider.getBooleanValue("quota.include.session.time") ? 1 : 0;
			final String header;
			if (serviceAPIHeader != null) {
				// The configuration value should be in the form header_name:header_value
				final String[] items = serviceAPIHeader.split(":");
				header = items[0];
				token = items[1];
			} else {
				header = "X-Auth-Token";
			}
			if (StringUtilities.isNullOrEmpty(token)) {
				// The API token is not configured, and hence if the user is not logged into TAO, there is no token
				logger.fine("API token for quota update is not configured and the user " + userId + " is offline");
				return;
			}
			// Either one must be non-zero
			if (usedStorage > 0 || processingTime > 0) {
				final String json = String.format(JSON_USER, userId, includeSessionTime * sessionDuration.getDuration() + processingTime, usedStorage);
				try (CloseableHttpResponse response = NetUtils.openConnection(HttpMethod.POST, serviceAPIUrl, header, token, json)) {
					if (response.getStatusLine().getStatusCode() > 201) {
						throw new IOException(EntityUtils.toString(response.getEntity()));
					} else {
						logger.finest(() -> String.format("Storace and processing quota updated for user %s", userId));
					}
				} catch (Exception e) {
					logger.severe(e.getMessage());
				}
			} else {
				logger.finest(() -> String.format("User %s has no storage and/or no processing time, skipping", userId));
			}
	    }

		private void updateBatch(List<String> userData) {
			final ConfigurationProvider cfgProvider = ConfigurationManager.getInstance();
			final String serviceAPIUrl = cfgProvider.getValue("dunia.service.api.batch.url", "https://dunia.esa.int/api/quota/tao/list");
			// If external URL is not defined, do nothing
			if (!StringUtilities.isNullOrEmpty(serviceAPIUrl)) {
				final String serviceAPIHeader = cfgProvider.getValue("dunia.service.api.key");
				final int includeSessionTime = cfgProvider.getBooleanValue("quota.include.session.time") ? 1 : 0;
				String header = null;
				String token = null;
				if (serviceAPIHeader != null) {
					// The configuration value should be in the form header_name:header_value
					final String[] items = serviceAPIHeader.split(":");
					header = items[0];
					token = items[1];
				}
				if (StringUtilities.isNullOrEmpty(token)) {
					// The API token is not configured, and hence if the user is not logged into TAO, there is no token
					logger.warning("Invalid API token for quota update");
					return;
				}
				if (userData != null && !userData.isEmpty()) {
					final String json = String.format(JSON_LIST, String.join(",", userData));
					try (CloseableHttpResponse response = NetUtils.openConnection(HttpMethod.POST, serviceAPIUrl, header, token, json)) {
						if (response.getStatusLine().getStatusCode() > 201) {
							throw new IOException(EntityUtils.toString(response.getEntity()));
						} else {
							logger.finest(() -> String.format("Quota updated for %d users", userData.size()));
						}
					} catch (Exception e) {
						logger.severe(e.getMessage());
					}
				}
			}
		}
	}
}
