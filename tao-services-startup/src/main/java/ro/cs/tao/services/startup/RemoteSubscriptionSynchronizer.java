/**
 * 
 */
package ro.cs.tao.services.startup;

import org.apache.http.util.EntityUtils;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.datasource.remote.result.json.JSonResponseHandler;
import ro.cs.tao.datasource.remote.result.json.JsonResponseParser;
import ro.cs.tao.keycloak.KeycloakClient;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.subscription.ExternalFlavorSubscription;
import ro.cs.tao.subscription.ExternalResourceSubscription;
import ro.cs.tao.subscription.FlavorSubscription;
import ro.cs.tao.user.User;
import ro.cs.tao.user.UserType;
import ro.cs.tao.utils.CloseableHttpResponse;
import ro.cs.tao.utils.HttpMethod;
import ro.cs.tao.utils.NetUtils;
import ro.cs.tao.utils.StringUtilities;

import java.io.IOException;
import java.util.*;

/**
 * Periodically inspect the remote subscriptions and save it to the local database.
 *
 */
public class RemoteSubscriptionSynchronizer extends BaseLifeCycle {
	/** Execution rate for synchronization, defined in configuration */
	private static final String EXECUTION_RATE = "tao.remote.subscriptions.synchronize.rate";
	/** API key used for remote calls, defined in configuration */
	private static final String SUBSCRIPTION_API_KEY = "dunia.service.api.key";
	/** API URL for subscriptions endpoint, defined in configuration */
	private static final String SUBSCRIPTIONS_API_URL = "dunia.service.api.subscriptions.url";
	/** Keycloak attribute for user's plan */
	private static final String KEYCLOAK_USER_PLAN_ATTRIBUTE = "user_plan";
	/** The periodic task. */
	private final SynchronizeSubscriptionsTask periodicTask = new SynchronizeSubscriptionsTask();
	/** The timer used to run the periodic task. */
	private final Timer timer = new Timer();

	@Override
	public int priority() {
		// lowest priority task
		return 100;
	}

	@Override
	public void onStartUp() {
		// get the configured execution rate, in hours
        final String executionRateStr = ConfigurationManager.getInstance().getValue(RemoteSubscriptionSynchronizer.EXECUTION_RATE, "1");
        // compute the execution rate, in milliseconds.
        long executionRate;
        try {
        	executionRate = Long.parseLong(executionRateStr) * 3600000;
        } catch (NumberFormatException ex) {
        	// add warning to the log 
        	logger.warning(() -> "The value " + executionRateStr + " provided to the property " + RemoteSubscriptionSynchronizer.EXECUTION_RATE + " is not valid. Using the default value 1.");
        	// fall back to the 1-hour delay
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
	private class SynchronizeSubscriptionsTask extends TimerTask {

		class UserPlan {
			String userPlan;
			int cpu;
			int ram;
			int disk;
			String flavor;
		}

		@Override
		public void run() {
			updateExternalSubscriptions();
			updateUserExternalSubscriptions();
		}

		/**
		 * Synchronize external subscriptions to local database
		 */
		private void updateExternalSubscriptions() {
			List<UserPlan> userPlans = getUserPlanDetails();
			if(userPlans != null) {
				for(UserPlan userPlan : userPlans) {
					// Get subscription by name and update its details if exists or create a new one
					ExternalResourceSubscription externalResourceSubscription = persistenceManager.externalResourceSubscription().getSubscriptionByName(userPlan.userPlan);
					if(externalResourceSubscription == null) {
						externalResourceSubscription = new ExternalResourceSubscription();
						externalResourceSubscription.setName(userPlan.userPlan);
					}
					externalResourceSubscription.getFlavors().clear();

					//externalResourceSubscription.setObjectStorageGB(userPlan.disk);

					ExternalFlavorSubscription flavor = new ExternalFlavorSubscription();
					flavor.setFlavorId(userPlan.flavor);
					flavor.setCpu(userPlan.cpu);
					flavor.setRamGb(userPlan.ram);
					flavor.setDiskGB(userPlan.disk);
					externalResourceSubscription.addFlavor(flavor);

					try {
						persistenceManager.externalResourceSubscription().save(externalResourceSubscription);
						logger.finest("External resource subscriptions synchronized");
					} catch (PersistenceException e) {
						logger.severe("Error saving external resource subscription: " + e.getMessage());
					}
				}
			}
		}

		/**
		 * Retrieve all user plans
		 * @return user plan list or null
		 */
		private List<UserPlan> getUserPlanDetails() {
			final String serviceAPIUrl = ConfigurationManager.getInstance().getValue(RemoteSubscriptionSynchronizer.SUBSCRIPTIONS_API_URL);

			if (!StringUtilities.isNullOrEmpty(serviceAPIUrl)) {
				final String serviceAPIHeader = ConfigurationManager.getInstance().getValue(RemoteSubscriptionSynchronizer.SUBSCRIPTION_API_KEY);
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
					logger.warning("Invalid API token for remote subscriptions synchronize");
					return null;
				}

				try (CloseableHttpResponse response = NetUtils.openConnection(HttpMethod.GET, serviceAPIUrl, header, token, null)) {
					if (response.getStatusLine().getStatusCode() > 201) {
						throw new IOException(EntityUtils.toString(response.getEntity()));
					} else {
						JsonResponseParser<UserPlan> parser = new JsonResponseParser<>(new JSonResponseHandler<UserPlan>() {});
						return parser.parse(EntityUtils.toString(response.getEntity()));
					}
				} catch (Exception e) {
					logger.severe(e.getMessage());
					return null;
				}
			} else {
				logger.warning("Invalid API URL for remote subscriptions synchronize");
				return null;
			}
		}

		/**
		 * Checks Keycloak users plan and assign an external subscription if one is founded.
		 */
		private void updateUserExternalSubscriptions() {
			try {
				KeycloakClient client = new KeycloakClient();
				// Get local users
				List<User> users = persistenceManager.users().list();
				// Get local synchronized subscriptions
				List<ExternalResourceSubscription> externalResourceSubscriptionList = persistenceManager.externalResourceSubscription().list();
				Map<String, ExternalResourceSubscription> externalResourceSubscriptionMap = new HashMap<>();
				for(ExternalResourceSubscription externalResourceSubscription : externalResourceSubscriptionList) {
					// Clear subscription users
					externalResourceSubscription.setUsers(new HashSet<>());
					externalResourceSubscriptionMap.put(externalResourceSubscription.getName(), externalResourceSubscription);
				}

				// Loop through users and check users of type KEYCLOAK
				for(User user : users) {
					if(user.getUserType().equals(UserType.KEYCLOAK)) {
						// Get user preferences from Keycloak
						String userPlanName = client.getUserPlanName(user.getUsername());
						if(!StringUtilities.isNullOrEmpty(userPlanName)) {
							// Get local synchronized subscription by user's plan name
							ExternalResourceSubscription externalResourceSubscription = externalResourceSubscriptionMap.get(userPlanName);
							if(externalResourceSubscription != null) {
								externalResourceSubscription.addUser(user);
							}
						}
					}
				}

				// Update users for subscriptions
				for(ExternalResourceSubscription externalResourceSubscription : externalResourceSubscriptionMap.values()) {
					persistenceManager.externalResourceSubscription().update(externalResourceSubscription);
				}
				logger.finest("Users external subscriptions updated");

			} catch (Exception e) {
				logger.severe(e.getMessage());
			}
		}

	}
}
