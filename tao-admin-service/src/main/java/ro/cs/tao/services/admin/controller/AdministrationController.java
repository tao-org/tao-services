/*
 * Copyright (C) 2018 CS ROMANIA
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package ro.cs.tao.services.admin.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.EnumUtils;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.configuration.ConfigurationProvider;
import ro.cs.tao.messaging.Messaging;
import ro.cs.tao.messaging.Topic;
import ro.cs.tao.persistence.NodeFlavorProvider;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.ResourceSubscriptionProvider;
import ro.cs.tao.persistence.managers.AuditManager;
import ro.cs.tao.quota.UserQuotaManager;
import ro.cs.tao.security.AuthenticationMode;
import ro.cs.tao.services.admin.beans.Flavor;
import ro.cs.tao.services.admin.beans.ResourceSubscriptionRequest;
import ro.cs.tao.services.admin.beans.ResourceSubscriptionResponse;
import ro.cs.tao.services.admin.beans.UserBean;
import ro.cs.tao.services.admin.mail.Constants;
import ro.cs.tao.services.auth.token.TokenManagementService;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.RoleRequired;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.AdministrationService;
import ro.cs.tao.services.model.user.DisableUserInfo;
import ro.cs.tao.subscription.FlavorSubscription;
import ro.cs.tao.subscription.ResourceSubscription;
import ro.cs.tao.subscription.SubscriptionType;
import ro.cs.tao.topology.NodeFlavor;
import ro.cs.tao.topology.TopologyException;
import ro.cs.tao.user.User;
import ro.cs.tao.user.UserStatus;
import ro.cs.tao.utils.StringUtilities;
import ro.cs.tao.utils.TriConsumer;
import ro.cs.tao.utils.mail.MailSender;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Oana H.
 */
@RestController
@RequestMapping("/admin")
@Tag(name = "User management", description = "Admin operations related to user management")
public class AdministrationController extends BaseController {

    @Autowired
    private AdministrationService adminService;

    @Autowired
    private TokenManagementService tokenService;

    @Autowired
    private SessionRegistry sessionRegistry;

    @Autowired
    private AuditManager auditManager;

    @Autowired
    private ResourceSubscriptionProvider resourceSubscriptionProvider;

    @Autowired
    private NodeFlavorProvider nodeFlavorProvider;

    /**
     * Creates a new user
     * @param newUserInfo   The user account information
     */
    @RequestMapping(value = "/users", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @RoleRequired(roles = "admin")
    public ResponseEntity<ServiceResponse<?>> addNewUser(@RequestBody User newUserInfo) {
        /*if (!isCurrentUserAdmin()) {
            return prepareResult(null, HttpStatus.UNAUTHORIZED);
        }*/
        if (newUserInfo == null) {
            return prepareResult("The expected request body is empty!", ResponseStatus.FAILED);
        }
        try {
            final User userInfo = adminService.addNewUser(newUserInfo);
            if (userInfo != null) {
                //send email with activation link
                final MailSender mailSender = new MailSender();
                final ConfigurationProvider configManager = ConfigurationManager.getInstance();
                final String activationEndpointUrl = configManager.getValue("tao.services.base") + "/user/activate/" + userInfo.getId();
                final String userFullName = userInfo.getFirstName() + " " + userInfo.getLastName();
                final String activationEmailContent = constructEmailContentForAccountActivation(userFullName, activationEndpointUrl);
                mailSender.sendMail(userInfo.getEmail(), "TAO - User activation required", activationEmailContent, null);

                return prepareResult(userInfo);
            } else {
                return prepareResult(null, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    @RequestMapping(value = "/users/unicity", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @RoleRequired(roles = "admin")
    public ResponseEntity<ServiceResponse<?>> getAllUsersUnicityInfo() {
        /*if (!isCurrentUserAdmin()) {
            return prepareResult(null, HttpStatus.UNAUTHORIZED);
        }*/
        return prepareResult(adminService.getAllUsersUnicityInfo());
    }

    @RequestMapping(value = "/users/ids", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @RoleRequired(roles = "admin")
    public ResponseEntity<ServiceResponse<?>> getAllUserNames() {
        /*if (!isCurrentUserAdmin()) {
            return prepareResult(null, HttpStatus.UNAUTHORIZED);
        }*/
        return prepareResult(adminService.getAllUserNames());
    }

    /**
     * Lists user accounts by their status
     * @param activationStatus  The user status
     */
    @RequestMapping(value = "/users", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @RoleRequired(roles = "admin")
    public ResponseEntity<ServiceResponse<?>> findUsersByStatus(@RequestParam("status") UserStatus activationStatus) {
        /*if (!isCurrentUserAdmin()) {
            return prepareResult(null, HttpStatus.UNAUTHORIZED);
        }*/
        if (activationStatus == null) {
            return prepareResult("The expected request params are empty!", ResponseStatus.FAILED);
        }
        final List<User> users = adminService.findUsersByStatus(activationStatus);
        final Map<String, Integer> processingTime = auditManager.getAggregatedUsersProcessingTime();
        return prepareResult(users.stream().map(u -> {
            UserBean bean = new UserBean(u);
            if (processingTime.containsKey(u.getId())) {
                bean.setProcessingTime(processingTime.getOrDefault(u.getId(), 0));
            }
            return bean;
        }).collect(Collectors.toList()));
    }

    /**
     * Lists the currently logged-in users
     */
    @RequestMapping(value = "/users/logged", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @RoleRequired(roles = "admin")
    public ResponseEntity<ServiceResponse<?>> findLoggedUsers() {
        /*if (!isCurrentUserAdmin()) {
            return prepareResult(null, HttpStatus.UNAUTHORIZED);
        }*/
        final List<User> users = new ArrayList<>();
        final Set<String> ids = sessionRegistry.getAllPrincipals().stream().filter(u -> !sessionRegistry.getAllSessions(u, false).isEmpty()).map(Object::toString).collect(Collectors.toSet());
        if (!ids.isEmpty()) {
            users.addAll(adminService.getUsers(ids));
        }
        return prepareResult(users);
    }

    /**
     * Lists the existing user groups
     */
    @RequestMapping(value = "/users/groups", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @RoleRequired(roles = "admin")
    public ResponseEntity<ServiceResponse<?>> getGroups() {
        /*if (!isCurrentUserAdmin()) {
            return prepareResult(null, HttpStatus.UNAUTHORIZED);
        }*/
        return prepareResult(adminService.getGroups());
    }

    /**
     * Returns the detail of a user account
     * @param userId  The user identifier
     */
    @RequestMapping(value = "/users/{userId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @RoleRequired(roles = "admin")
    public ResponseEntity<ServiceResponse<?>> getUserInfo(@PathVariable("userId") String userId) {
        /*if (!isCurrentUserAdmin()) {
            return prepareResult(null, HttpStatus.UNAUTHORIZED);
        }*/
        if (StringUtilities.isNullOrEmpty(userId)) {
            return prepareResult("The expected request params are empty!", ResponseStatus.FAILED);
        }
        try {
            final User userInfo = adminService.getUserInfo(userId);
            if (userInfo != null) {
                return prepareResult(userInfo);
            } else {
                return prepareResult(null, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    /**
     * Returns the resource subscription of a user account
     * @param userId  The user identifier
     */
    @RequestMapping(value = "/users/{userId}/subscription", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @RoleRequired(roles = "admin")
    public ResponseEntity<ServiceResponse<?>> getUserActiveSubscription(@PathVariable("userId") String userId) {
        if (!adminService.isDevModeEnabled()) {
            /*if (!isCurrentUserAdmin()) {
                return prepareResult(null, HttpStatus.UNAUTHORIZED);
            }*/
            if (StringUtilities.isNullOrEmpty(userId)) {
                return prepareResult("The expected request params are empty!", ResponseStatus.FAILED);
            }
            try {
                final ResourceSubscription subscription = resourceSubscriptionProvider.getUserOpenSubscription(userId);
                if (subscription == null) {
                    return prepareResult(new ArrayList<>());
                }
                final ResourceSubscriptionResponse response = new ResourceSubscriptionResponse();
                response.setUserId(userId);
                response.setCreated(subscription.getCreated());
                response.setPaid(subscription.isPaid());
                response.setType(subscription.getType());
                response.setObjectStorageGB(subscription.getObjectStorageGB());
                final List<Flavor> flavors = new ArrayList<>();
                final Map<String, FlavorSubscription> flavorMap = subscription.getFlavors();
                if (flavorMap != null) {
                    for (Map.Entry<String, FlavorSubscription> entry : flavorMap.entrySet()) {
                        final FlavorSubscription value = entry.getValue();
                        Flavor flavor = new Flavor();
                        flavor.setFlavor(nodeFlavorProvider.get(value.getFlavorId()));
                        flavor.setQuantity(value.getQuantity());
                        flavor.setHdd(value.getHddGB());
                        flavor.setSsd(value.getSsdGB());
                        flavors.add(flavor);
                    }
                }
                response.setFlavors(flavors);
                return prepareResult(response);
            } catch (Exception ex) {
                return handleException(ex);
            }
        } else {
            final ResourceSubscriptionResponse response = new ResourceSubscriptionResponse();
            response.setUserId(userId);
            response.setCreated(LocalDateTime.now());
            response.setPaid(false);
            response.setType(SubscriptionType.FIXED_RESOURCES);
            response.setObjectStorageGB(10);
            final List<Flavor> flavors = new ArrayList<>();
            Flavor flavor = new Flavor();
            NodeFlavor nodeFlavor = new NodeFlavor();
            nodeFlavor.setId("xa.large");
            nodeFlavor.setCpu(4);
            nodeFlavor.setMemory(8192);
            nodeFlavor.setDisk(512);
            nodeFlavor.setSwap(16384);
            nodeFlavor.setRxtxFactor(0.5f);
            flavor.setFlavor(nodeFlavor);
            flavor.setQuantity(1);
            flavor.setHdd(2048);
            flavor.setSsd(384);
            flavors.add(flavor);
            response.setFlavors(flavors);
            return prepareResult(response);
        }
    }

    /**
     * Creates a resource subscription of a user account
     * @param userId  The user identifier
     * @param request The subscription entity request
     */
    @RequestMapping(value = "/users/{userId}/subscription", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @RoleRequired(roles = "admin")
    public ResponseEntity<ServiceResponse<?>> createUserSubscription(@PathVariable("userId") String userId,
                                                                     @RequestBody ResourceSubscriptionRequest request) {
        /*if (!isCurrentUserAdmin()) {
            return prepareResult(null, HttpStatus.UNAUTHORIZED);
        }*/
        Map<String, Map<String, Integer>> flavors;
        if (request == null || (flavors = request.getFlavors()) == null || flavors.isEmpty()) {
            return prepareResult("One or more required values are empty!", ResponseStatus.FAILED);
        }
        final TriConsumer<String, Exception, String> callback = makeCallbackMessage(userId);
        try {
            ResourceSubscription subscription = new ResourceSubscription();
            subscription.setUserId(userId);
            subscription.setType(request.getType());
            subscription.setCreated(request.getCreated() != null ? request.getCreated() : LocalDateTime.now());
            subscription.setEnded(null);
            subscription.setPaid(request.isPaid());
            subscription.setObjectStorageGB(request.getObjectStorageGB());
            final Map<String, FlavorSubscription> flavorMap = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, Integer>> flavor : flavors.entrySet()) {
                final NodeFlavor nodeFlavor = nodeFlavorProvider.get(flavor.getKey());
                final Map<String, Integer> values = flavor.getValue();
                int quantity = values.getOrDefault("quantity", 0);
                if (nodeFlavor == null || quantity <= 0) {
                    return prepareResult("Invalid flavor '" + flavor.getKey() + "'", ResponseStatus.FAILED);
                }
                FlavorSubscription flavorSubscription = new FlavorSubscription();
                flavorSubscription.setFlavorId(flavor.getKey());
                flavorSubscription.setQuantity(quantity);
                flavorSubscription.setHddGB(values.getOrDefault("hdd", 0));
                flavorSubscription.setSsdGB(values.getOrDefault("ssd", 0));
                flavorMap.put(flavor.getKey(), flavorSubscription);
            }
            subscription.setFlavors(flavorMap);
            resourceSubscriptionProvider.save(subscription);
            asyncExecute(() -> {
                try {
                    if(subscription.getType().equals(SubscriptionType.FIXED_RESOURCES)) {
                        adminService.initializeSubscription(subscription);
                    }
                } catch (PersistenceException e) {
                    throw new TopologyException(e);
                }
            }, "User subscription initialized", callback);
            return prepareResult("Subscription is being initialized");
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    /**
     * Updates a resource subscription of a user account
     * @param userId  The user identifier
     * @param request The subscription entity request
     */
    @RequestMapping(value = "/users/{userId}/subscription", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @RoleRequired(roles = "admin")
    public ResponseEntity<ServiceResponse<?>> updateUserSubscription(@PathVariable("userId") String userId,
                                                                     @RequestBody ResourceSubscriptionRequest request) {
        /*if (!isCurrentUserAdmin()) {
            return prepareResult(null, HttpStatus.UNAUTHORIZED);
        }*/
        Map<String, Map<String, Integer>> flavorsRequest;
        if (request == null || (flavorsRequest = request.getFlavors()) == null || flavorsRequest.isEmpty()) {
            return prepareResult("One or more required values are empty!", ResponseStatus.FAILED);
        }
        int created = 0, removed = 0, updated = 0;
        try {
            ResourceSubscription subscription = resourceSubscriptionProvider.getUserOpenSubscription(userId);
            if(subscription == null){
                return prepareResult("Subscription is not valid", ResponseStatus.FAILED);
            }
            subscription.setObjectStorageGB(request.getObjectStorageGB());
            subscription.setType(request.getType());
            subscription.setPaid(request.isPaid());
            if(!subscription.getType().equals(SubscriptionType.FIXED_RESOURCES)) {
                resourceSubscriptionProvider.update(subscription);
                return prepareResult("Subscription information updated. Pay per use subscription cannot update node information");
            }
            final TriConsumer<String, Exception, String> callback = makeCallbackMessage(userId);
            for (Iterator<Map.Entry<String, FlavorSubscription>> it = subscription.getFlavors().entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, FlavorSubscription> flavorEntry = it.next();
                String flavorId = flavorEntry.getKey();
                final Map<String, Integer> flavorResponseEntry = flavorsRequest.get(flavorId);
                FlavorSubscription flavorSubscription = flavorEntry.getValue();
                // Flavor has been deleted
                if (flavorResponseEntry == null || flavorSubscription.getQuantity() <= 0) {
                    it.remove();
                    asyncExecute(() -> {
                        try {
                            adminService.deleteFlavorNodesForSubscription(subscription, flavorSubscription, flavorSubscription.getQuantity());
                        } catch (PersistenceException | TopologyException e) {
                            throw new TopologyException(e);
                        }
                    }, flavorSubscription.getQuantity() + " nodes with flavor " + flavorId + " deleted", callback);
                    removed++;
                    // Flavor has been updated
                } else {
                    final Integer updatedQuantity = flavorResponseEntry.get("quantity");
                    int currentQuantity = flavorSubscription.getQuantity();
                    flavorSubscription.setQuantity(updatedQuantity);
                    flavorSubscription.setHddGB(flavorResponseEntry.getOrDefault("hdd", 0));
                    flavorSubscription.setSsdGB(flavorResponseEntry.getOrDefault("ssd", 0));
                    if (currentQuantity != updatedQuantity) {
                        if (updatedQuantity > currentQuantity) {
                            int quantityDifference = updatedQuantity - currentQuantity;
                            final String successMessage = "Nodes with flavor " + flavorId + " had "
                                    + quantityDifference + " quantity added";
                            asyncExecute(() -> {
                                try {
                                    adminService.createFlavorNodesForSubscription(subscription, flavorSubscription, quantityDifference);
                                } catch (PersistenceException | TopologyException e) {
                                    throw new TopologyException(e);
                                }
                            }, successMessage, callback);
                        } else {
                            //Delete nodes if new quantity is lower
                            int quantityDifference = currentQuantity - updatedQuantity;
                            final String successMessage = "Nodes with flavor " + flavorId + " had "
                                    + quantityDifference + " quantity deleted";
                            asyncExecute(() -> {
                                try {
                                    adminService.deleteFlavorNodesForSubscription(subscription, flavorSubscription, quantityDifference);

                                } catch (PersistenceException | TopologyException e) {
                                    throw new TopologyException(e);
                                }
                            }, successMessage, callback);
                        }
                    }
                    updated++;
                }
                flavorsRequest.remove(flavorId);
            }
            for (Map.Entry<String, Map<String, Integer>> flavor : flavorsRequest.entrySet()) {
                // New flavors have been added
                final NodeFlavor nodeFlavor = nodeFlavorProvider.get(flavor.getKey());
                final Map<String, Integer> values = flavor.getValue();
                if (nodeFlavor == null || values.getOrDefault("quantity", 0) <= 0) {
                    return prepareResult("Invalid flavor '" + flavor.getKey() + "'", ResponseStatus.FAILED);
                }
                int quantity = values.get("quantity");
                FlavorSubscription newFlavorSubscription = new FlavorSubscription();
                newFlavorSubscription.setFlavorId(flavor.getKey());
                newFlavorSubscription.setHddGB(values.getOrDefault("hdd", 0));
                newFlavorSubscription.setSsdGB(values.getOrDefault("ssd", 0));
                newFlavorSubscription.setQuantity(quantity);
                subscription.getFlavors().put(flavor.getKey(), newFlavorSubscription);
                final String successMessage = "Node with flavor " + nodeFlavor.getId() + " created";
                asyncExecute(() -> {
                    try {
                        adminService.createFlavorNodesForSubscription(subscription, newFlavorSubscription, quantity);
                    } catch (PersistenceException | TopologyException e) {
                        throw new TopologyException(e);
                    }
                }, successMessage, callback);
                created++;
            }
            resourceSubscriptionProvider.update(subscription);
            return prepareResult(String.format("Subscriptions: %d created, %d updated, %d removed",
                                               created, updated, removed));
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    /**
     * Closes a resource subscription of a user account
     * @param userId  The user identifier
     */
    @RequestMapping(value = "/users/{userId}/subscription", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @RoleRequired(roles = "admin")
    public ResponseEntity<ServiceResponse<?>> closeUserSubscription(@PathVariable("userId") String userId) {
        /*if (!isCurrentUserAdmin()) {
            return prepareResult(null, HttpStatus.UNAUTHORIZED);
        }*/
        try {
            ResourceSubscription subscription = resourceSubscriptionProvider.getUserOpenSubscription(userId);
            if (subscription == null) {
                throw new IllegalArgumentException("No such subscription");
            }
            if (subscription.getEnded() != null) {
                throw new IllegalAccessException("Subscription already closed");
            }
            subscription.setEnded(LocalDateTime.now());
            resourceSubscriptionProvider.update(subscription);
            //delete nodes as required
            if(!subscription.getType().equals(SubscriptionType.FIXED_RESOURCES)) {
                return prepareResult("Subscription information deleted. Pay per use subscription cannot delete nodes.");
            }
            for(Map.Entry<String, FlavorSubscription> entry : subscription.getFlavors().entrySet()){
                final TriConsumer<String, Exception, String> callback = makeCallbackMessage(userId);
                asyncExecute(() -> {
                    try {
                        adminService.deleteFlavorNodesForSubscription(subscription, entry.getValue(), entry.getValue().getQuantity());
                    } catch (PersistenceException | TopologyException e) {
                        throw new TopologyException(e);
                    }
                }, "All nodes deleted for user subscription", callback);
            }
            return prepareResult("Subscription closed", ResponseStatus.SUCCEEDED);
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    /**
     * Updates the user account information
     * @param updatedUserInfo   The user account information
     */
    @RequestMapping(value = "/users/{userId}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @RoleRequired(roles = "admin")
    public ResponseEntity<ServiceResponse<?>> updateUserInfo(@PathVariable("userId") String userId,
                                                             @RequestBody User updatedUserInfo) {
        /*if (!isCurrentUserAdmin()) {
            return prepareResult(null, HttpStatus.UNAUTHORIZED);
        }*/
        if (updatedUserInfo == null) {
            return prepareResult("The expected request body is empty!", ResponseStatus.FAILED);
        }
        if (!Objects.equals(updatedUserInfo.getId(), userId)) {
            return prepareResult("Invalid identifier");
        }
        try {
            final User userInfo = adminService.updateUserInfo(updatedUserInfo);
            if (userInfo != null) {
            	if (userInfo.getInputQuota() != -1) {
            		// compute user quota
            		UserQuotaManager.getInstance().updateUserInputQuota(SecurityContextHolder.getContext().getAuthentication());
            	}
            	if (userInfo.getProcessingQuota() != -1) {
            		// compute user quota
            		UserQuotaManager.getInstance().updateUserProcessingQuota(SecurityContextHolder.getContext().getAuthentication());
            	}
            	
                return prepareResult(userInfo);
            } else {
                return prepareResult(null, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    /**
     * Disables a user account
     * @param userId  The user identifier
     * @param additionalDisableActions  Additional actions to be performed
     */
    @RequestMapping(value = "/users/{userId}/disable", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @RoleRequired(roles = "admin")
    public ResponseEntity<ServiceResponse<?>> disableUser(@PathVariable("userId") String userId, @RequestBody DisableUserInfo additionalDisableActions) {
        /*if (!isCurrentUserAdmin()) {
            return prepareResult(null, HttpStatus.UNAUTHORIZED);
        }*/
        if (StringUtilities.isNullOrEmpty(userId) || additionalDisableActions == null) {
            return prepareResult("The expected request params are empty!", ResponseStatus.FAILED);
        }
        try {
            // disable user
            adminService.disableUser(userId, additionalDisableActions);
            return prepareResult(String.format("%s was disabled", userId), ResponseStatus.SUCCEEDED);
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    /**
     * Deletes a user account
     * @param userId  The user identifier
     */
    @RequestMapping(value = "/users/{userId}", method = RequestMethod.DELETE, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @RoleRequired(roles = "admin")
    public ResponseEntity<ServiceResponse<?>> deleteUser(@PathVariable("userId") String userId) {
        /*if (!isCurrentUserAdmin()) {
            return prepareResult(null, HttpStatus.UNAUTHORIZED);
        }*/
        if (StringUtilities.isNullOrEmpty(userId)) {
            return prepareResult("The expected request params are empty!", ResponseStatus.FAILED);
        }
        try {
            adminService.deleteUser(userId);
            // TODO delete private resources
            /*if (deletePrivateResources){

            }*/
            return prepareResult(String.format("User %s was deleted", userId), ResponseStatus.SUCCEEDED);
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    /**
     * Sends a message from an administrator to a logged-in user.
     *
     * @param userId  The user identifier
     * @param message The message
     */
    @RequestMapping(value = "/message", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @RoleRequired(roles = "admin")
    public ResponseEntity<ServiceResponse<?>> messageUser(@RequestParam("userId") String userId,
                                                          @RequestParam("message") String message) {
        /*if (!isCurrentUserAdmin()) {
            return prepareResult(null, HttpStatus.UNAUTHORIZED);
        }*/
        if (StringUtilities.isNullOrEmpty(userId)) {
            return prepareResult("The expected request params are empty!", ResponseStatus.FAILED);
        }
        if (sessionRegistry.getAllPrincipals().stream().noneMatch(u -> u.toString().equals(userId)
                                                                    && !sessionRegistry.getAllSessions(u, false).isEmpty())) {
            return prepareResult(String.format("No user session is active for %s!", userId), ResponseStatus.FAILED);
        }
        try {
            adminService.messageUser(userId, message);
            return prepareResult(String.format("Message sent to %s", userId), ResponseStatus.SUCCEEDED);
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    /**
     * Sends a message from an administrator to all logged-in users.
     *
     * @param message The message
     */
    @RequestMapping(value = "/message/all", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @RoleRequired(roles = "admin")
    public ResponseEntity<ServiceResponse<?>> messageUsers(@RequestParam("message") String message) {
        /*if (!isCurrentUserAdmin()) {
            return prepareResult(null, HttpStatus.UNAUTHORIZED);
        }*/
        try {
            final List<String> activeUsers = sessionRegistry.getAllPrincipals().stream()
                                                        .filter(u -> !sessionRegistry.getAllSessions(u, false).isEmpty())
                                                        .map(u -> u instanceof Principal ? ((Principal) u).getName() : u.toString())
                                                        .collect(Collectors.toList());
            for (String principal : activeUsers) {
                try {
                    adminService.messageUser(principal, message);
                } catch (Exception ignored) { }
            }
            return prepareResult(String.format("Message sent to: %s", String.join(",", activeUsers)), ResponseStatus.SUCCEEDED);
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    /**
     * Forces the keycloak redirection after an impersonation action (on keycloak)
     *
     * @param userId    The user to impersonate
     */
    @RequestMapping(value = "/impersonate", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @RoleRequired(roles = "admin")
    public ResponseEntity<ServiceResponse<?>> impersonate(@RequestParam("userId") String userId, HttpServletRequest request) throws IOException {
        try {
            /*if (!isCurrentUserAdmin()) {
                throw new IllegalArgumentException();
            } else {*/
                final AuthenticationMode authMode = EnumUtils.getEnumConstantByName(AuthenticationMode.class,
                                                                                    ConfigurationManager.getInstance()
                                                                                                        .getValue("authentication.mode", "local")
                                                                                                        .toUpperCase());
                if (authMode != AuthenticationMode.KEYCLOAK) {
                    throw new IllegalArgumentException("Auth mode not Keycloak");
                }
                String url = ConfigurationManager.getInstance().getValue("keycloak.auth-server-url");
                String realm = ConfigurationManager.getInstance().getValue("keycloak.realm");
                String client = ConfigurationManager.getInstance().getValue("keycloak.resource");
                request.getSession().invalidate();
                return prepareResult(url + "/realms/" + realm + "/protocol/openid-connect/auth?response_type=code&scope=openid&client_id="
                        + client + "&redirect_uri=" + ConfigurationManager.getInstance().getValue("tao.services.base") + "/ui/login.html");

            //}
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * Returns the aggregated time spent by users on this instance
     * @param userId (optional) The user identifier. If not specified, the aggregation is done for all users
     * @param from  (optional) The start of the interval to aggregate. If not specified, it is the first timestamp recorded
     * @param to    (optional) The end of the interval to aggregate. If not specified, it is the current moment.
     */
    @RequestMapping(value = "/users/sessions", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getSessionsTime(@RequestParam(name = "userId", required = false) String userId,
                                                              @RequestParam(name = "from", required = false) LocalDateTime from,
                                                              @RequestParam(name = "to", required = false) LocalDateTime to) {
        if (!isCurrentUserAdmin() || !currentUser().equals(userId)) {
            return prepareResult(null, HttpStatus.UNAUTHORIZED);
        }
        try {
            if (from != null && to != null) {
                if (StringUtilities.isNullOrEmpty(userId)) {
                    return prepareResult(auditManager.getAllSessions(from, to));
                } else {
                    return prepareResult(auditManager.getUserSessions(userId, from, to));
                }
            } else {
                LocalDateTime start = from != null ? from : LocalDateTime.MIN;
                LocalDateTime end = to != null ? to : LocalDateTime.now();
                if (StringUtilities.isNullOrEmpty(userId)) {
                    return prepareResult(auditManager.getAllSessions(from, to));
                } else {
                    return prepareResult(auditManager.getUserSessions(userId, start, end));
                }
            }
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    /**
     * Returns the aggregated time consumed by users for processing. The values returned are in minutes.
     *
     * @param userId (optional) The user identifier. If not specified, the aggregation is done for all users
     */
    @RequestMapping(value = "/users/sessions/total", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getTotalProcessingTime(@RequestParam(name = "userId", required = false) String userId) {
        if (!isCurrentUserAdmin() && !currentUser().equals(userId)) {
            return prepareResult(null, HttpStatus.UNAUTHORIZED);
        }
        try {
            if (StringUtilities.isNullOrEmpty(userId)) {
                return prepareResult(auditManager.getAggregatedUsersProcessingTime());
            } else {
                return prepareResult(new AbstractMap.SimpleEntry<>(userId,
                                                                   auditManager.getAggregatedUserProcessingTime(userId)));
            }
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    private String constructEmailContentForAccountActivation(String userFullName, String activationLink){
        return Constants.MAIL_CONTENTS.replace("$USERNAME", userFullName).replace("$LINK", activationLink);
    }

    private TriConsumer<String, Exception, String> makeCallbackMessage(String userId){
        return (u, e, s) -> {
            String topic;
            String message;
            if (e == null) {
                topic = Topic.INFORMATION.getCategory();
                message = s;
            } else {
                topic = Topic.WARNING.getCategory();
                message = e.getMessage();
            }

            Messaging.send(userId, topic, message);
        };
    }
}
