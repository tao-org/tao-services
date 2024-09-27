/**
 * 
 */
package ro.cs.tao.services.startup;

import org.apache.http.util.EntityUtils;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.configuration.ConfigurationProvider;
import ro.cs.tao.keycloak.KeycloakClient;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.managers.ExternalResourceSubscriptionManager;
import ro.cs.tao.security.Token;
import ro.cs.tao.serialization.BaseSerializer;
import ro.cs.tao.serialization.MediaType;
import ro.cs.tao.serialization.SerializerFactory;
import ro.cs.tao.subscription.ExternalFlavorSubscription;
import ro.cs.tao.subscription.ExternalResourceSubscription;
import ro.cs.tao.subscription.FlavorSubscription;
import ro.cs.tao.subscription.ResourceSubscription;
import ro.cs.tao.subscription.SubscriptionType;
import ro.cs.tao.subscription.UserPlan;
import ro.cs.tao.topology.NodeFlavor;
import ro.cs.tao.user.User;
import ro.cs.tao.user.UserType;
import ro.cs.tao.utils.CloseableHttpResponse;
import ro.cs.tao.utils.ExceptionUtils;
import ro.cs.tao.utils.HttpMethod;
import ro.cs.tao.utils.NetUtils;
import ro.cs.tao.utils.StringUtilities;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Periodically inspects the remote subscriptions and saves them to the local database.
 * Subscriptions are assigned to users of type Keycloak if they have one set in the Keycloak attribute "user_plan".
 * Internal subscriptions are updated according to external subscriptions.
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
	/** Keycloak user account for accessing the remote API **/
	private static final String KEYCLOAK_API_USER = "dunia.service.api.user";
	/** Keycloak account password for accessing the remote API **/
	private static final String KEYCLOAK_API_PWD = "dunia.service.api.password";
	/** The periodic task. */
	private final SynchronizeSubscriptionsTask periodicTask = new SynchronizeSubscriptionsTask();
	/** The timer used to run the periodic task. */
	private final Timer timer = new Timer();
	private String serviceAPIUrl;

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
		this.serviceAPIUrl = ConfigurationManager.getInstance().getValue(RemoteSubscriptionSynchronizer.SUBSCRIPTIONS_API_URL);
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
	private class SynchronizeSubscriptionsTask extends TimerTask {

		@Override
		public void run() {
			updateExternalSubscriptions();
			updateUserExternalSubscriptions();
			updateUsersSubscriptionsFromExternalSubscriptions();
		}

		/**
		 * Synchronize external subscriptions to local database
		 */
		private void updateExternalSubscriptions() {
			List<UserPlan> userPlans = getUserPlanDetails();
			if (userPlans != null) {
				final ExternalResourceSubscriptionManager subscriptionManager = persistenceManager.externalResourceSubscription();
				for (UserPlan userPlan : userPlans) {
					// Get subscription by name and update its details if exists or create a new one
					ExternalResourceSubscription externalResourceSubscription = subscriptionManager.getSubscriptionByName(userPlan.getUserPlan());
					if (externalResourceSubscription == null) {
						externalResourceSubscription = new ExternalResourceSubscription();
						externalResourceSubscription.setName(userPlan.getUserPlan());
					}
					externalResourceSubscription.getFlavors().clear();

					// User's plan disk represents disk or object storage
					externalResourceSubscription.setObjectStorageGB(userPlan.getDisk());

					ExternalFlavorSubscription flavor = new ExternalFlavorSubscription();
					flavor.setFlavorId(userPlan.getFlavor());
					flavor.setCpu(userPlan.getCpu());
					flavor.setRamGb(userPlan.getRam());
					flavor.setDiskGB(userPlan.getDisk());
					externalResourceSubscription.addFlavor(flavor);

					try {
						if (externalResourceSubscription.getId() == null) {
							subscriptionManager.save(externalResourceSubscription);
						} else {
							subscriptionManager.update(externalResourceSubscription);
						}
					} catch (PersistenceException e) {
						logger.severe("Error saving external resource subscription: " + e.getMessage());
					}
				}
				logger.fine("External resource subscriptions synchronized");
			}
		}

		/**
		 * Retrieve all user plans
		 * @return user plan list or null
		 */
		private List<UserPlan> getUserPlanDetails() {
			final ConfigurationProvider configurationProvider = ConfigurationManager.getInstance();
			String header = null;
			String token = null;
			final String user = configurationProvider.getValue(KEYCLOAK_API_USER);
			final String serviceAPIHeader = configurationProvider.getValue(RemoteSubscriptionSynchronizer.SUBSCRIPTION_API_KEY);
			if (!StringUtilities.isNullOrEmpty(serviceAPIHeader)) {
				// The configuration value should be in the form header_name:header_value
				final String[] items = serviceAPIHeader.split(":");
				header = items[0];
				token = items[1];
			} else if (!StringUtilities.isNullOrEmpty(user)) {
				final String pass = configurationProvider.getValue(KEYCLOAK_API_PWD);
				KeycloakClient client = new KeycloakClient();
				final Token kToken = client.newToken(user, pass);
				header = "Authorization";
				token = "Bearer " + kToken.getToken();
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
					final BaseSerializer<UserPlan> serializer = SerializerFactory.create(UserPlan.class, MediaType.JSON);
					return serializer.deserialize(UserPlan.class, EntityUtils.toString(response.getEntity()));
				}
			} catch (Exception e) {
				logger.severe(e.getMessage());
				logger.severe(ExceptionUtils.getStackTrace(logger, e));
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
				for (ExternalResourceSubscription externalResourceSubscription : externalResourceSubscriptionList) {
					// Clear subscription users
					externalResourceSubscription.setUsers(new HashSet<>());
					externalResourceSubscriptionMap.put(externalResourceSubscription.getName(), externalResourceSubscription);
				}

				// Loop through users and check users of type KEYCLOAK
				for (User user : users) {
					if (UserType.KEYCLOAK.equals(user.getUserType())) {
						// Get user preferences from Keycloak
						String userPlanName = client.getUserPlanName(user.getUsername());
						if (!StringUtilities.isNullOrEmpty(userPlanName)) {
							// Get local synchronized subscription by user's plan name
							ExternalResourceSubscription externalResourceSubscription = externalResourceSubscriptionMap.get(userPlanName);
							if (externalResourceSubscription != null) {
								externalResourceSubscription.addUser(user);
							}
						}
					}
				}

				// Update users for subscriptions
				for (ExternalResourceSubscription externalResourceSubscription : externalResourceSubscriptionMap.values()) {
					persistenceManager.externalResourceSubscription().update(externalResourceSubscription);
				}
				logger.fine("Users external subscriptions updated");

			} catch (Exception e) {
				logger.severe(e.getMessage());
				logger.severe(ExceptionUtils.getStackTrace(logger, e));
			}
		}

		private void updateUsersSubscriptionsFromExternalSubscriptions() {
			try {
				// Create or update users subscriptions
				// Get external subscriptions
				List<ExternalResourceSubscription> externalResourceSubscriptionList = persistenceManager.externalResourceSubscription().list();
				for (ExternalResourceSubscription externalResourceSubscription : externalResourceSubscriptionList) {
					// Get users with external subscriptions
					Set<User> userSet = externalResourceSubscription.getUsers();
					for (User user : userSet) {
						Map<String, ExternalFlavorSubscription> externalFlavorSubscriptionMap = externalResourceSubscription.getFlavors();
						Optional<ExternalFlavorSubscription> externalFlavorSubscriptionOptional = externalFlavorSubscriptionMap.values().stream().findFirst();
						if(externalFlavorSubscriptionOptional.isPresent()) {
							// Get flavor based on external subscription flavor characteristics
							NodeFlavor nodeFlavor = persistenceManager.nodeFlavors().getMatchingFlavor(externalFlavorSubscriptionOptional.get().getCpu(), externalFlavorSubscriptionOptional.get().getRamGb());
							if(nodeFlavor != null) {
								// Get user's active subscription
								ResourceSubscription resourceSubscription = persistenceManager.resourceSubscription().getUserOpenSubscription(user.getId());
								if(resourceSubscription == null) {
									// New subscription
									resourceSubscription = new ResourceSubscription();
									resourceSubscription.setUserId(user.getId());
									resourceSubscription.setCreated(LocalDateTime.now());
									resourceSubscription.setEnded(null);
								}
								// Override properties
								resourceSubscription.setType(SubscriptionType.PAY_PER_USE);
								resourceSubscription.setPaid(false);
								resourceSubscription.setObjectStorageGB(externalResourceSubscription.getObjectStorageGB());

								final Map<String, FlavorSubscription> flavorMap = new LinkedHashMap<>();
								FlavorSubscription flavorSubscription = new FlavorSubscription();
								flavorSubscription.setFlavorId(nodeFlavor.getId());
								flavorSubscription.setQuantity(1);
								flavorSubscription.setHddGB(null);
								flavorSubscription.setSsdGB(null);
								flavorMap.put(nodeFlavor.getId(), flavorSubscription);

								resourceSubscription.setFlavors(flavorMap);
								if(resourceSubscription.getId() == null) {
									persistenceManager.resourceSubscription().save(resourceSubscription);
								} else {
									persistenceManager.resourceSubscription().update(resourceSubscription);
								}
							}
						}
					}
				}

				// End users subscriptions if matching external subscription and user doesn't have an external subscription assigned
				List<User> users = persistenceManager.users().list();
				for (User user : users) {
					if(UserType.KEYCLOAK.equals(user.getUserType())) {
						ResourceSubscription activeResourceSubscription = persistenceManager.resourceSubscription().getUserOpenSubscription(user.getId());
						if(activeResourceSubscription != null) {
							for (ExternalResourceSubscription externalResourceSubscription : externalResourceSubscriptionList) {
								Set<User> externalResourceSubscriptionUsers = externalResourceSubscription.getUsers();
								// If the user is assigned to this external subscription
								if(externalResourceSubscriptionUsers != null && !externalResourceSubscriptionUsers.contains(user)) {
									if(subscriptionIsMatchingExternalSubscription(activeResourceSubscription, externalResourceSubscription)) {
										// End active subscription
										activeResourceSubscription.setEnded(LocalDateTime.now());
										persistenceManager.resourceSubscription().update(activeResourceSubscription);
									}
								}
							}
						}
					}
				}
				logger.fine("Users subscriptions updated from external subscriptions");
			} catch (Exception e) {
				logger.severe(e.getMessage());
				logger.severe(ExceptionUtils.getStackTrace(logger, e));
			}

		}

		private boolean subscriptionIsMatchingExternalSubscription(ResourceSubscription resourceSubscription, ExternalResourceSubscription externalResourceSubscription) {
			if(resourceSubscription == null || externalResourceSubscription == null) return false;

			// Subscriptions created from external subscriptions have the type PAY_PER_USE
			if(!SubscriptionType.PAY_PER_USE.equals(resourceSubscription.getType())) return false;

			Map<String, FlavorSubscription> flavorSubscriptionMap = resourceSubscription.getFlavors();
			Map<String, ExternalFlavorSubscription> externalFlavorSubscriptionMap = externalResourceSubscription.getFlavors();
			if(flavorSubscriptionMap == null || externalFlavorSubscriptionMap == null || flavorSubscriptionMap.keySet().size() != externalFlavorSubscriptionMap.keySet().size()) return false;

			Optional<FlavorSubscription> flavorSubscriptionOptional = flavorSubscriptionMap.values().stream().findFirst();
			Optional<ExternalFlavorSubscription> externalFlavorSubscriptionOptional = externalFlavorSubscriptionMap.values().stream().findFirst();
			if(externalFlavorSubscriptionOptional.isPresent() && flavorSubscriptionOptional.isPresent()) {
				// Get flavor based on external subscription flavor characteristics
				NodeFlavor nodeFlavor = persistenceManager.nodeFlavors().getMatchingFlavor(externalFlavorSubscriptionOptional.get().getCpu(), externalFlavorSubscriptionOptional.get().getRamGb());
				// Same number of flavors (currently one for external subscriptions)
				if (nodeFlavor != null) {
                    return flavorSubscriptionOptional.get().getFlavorId().equals(nodeFlavor.getId());
				}
			}
			return false;
		}


	}
}
