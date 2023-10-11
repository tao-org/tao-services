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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.configuration.ConfigurationProvider;
import ro.cs.tao.persistence.managers.AuditManager;
import ro.cs.tao.quota.UserQuotaManager;
import ro.cs.tao.services.admin.mail.Constants;
import ro.cs.tao.services.auth.token.TokenManagementService;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.AdministrationService;
import ro.cs.tao.services.model.user.DisableUserInfo;
import ro.cs.tao.user.User;
import ro.cs.tao.user.UserStatus;
import ro.cs.tao.utils.StringUtilities;
import ro.cs.tao.utils.mail.MailSender;

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

    /**
     * Creates a new user
     * @param newUserInfo   The user account information
     */
    @RequestMapping(value = "/users", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> addNewUser(@RequestBody User newUserInfo) {
        if (!isCurrentUserAdmin()) {
            return prepareResult(null, HttpStatus.UNAUTHORIZED);
        }
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

    @RequestMapping(value = "/users/unicity", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getAllUsersUnicityInfo() {
        if (!isCurrentUserAdmin()) {
            return prepareResult(null, HttpStatus.UNAUTHORIZED);
        }
        return prepareResult(adminService.getAllUsersUnicityInfo());
    }

    /**
     * Lists user accounts by their status
     * @param activationStatus  The user status
     */
    @RequestMapping(value = "/users", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> findUsersByStatus(@RequestParam("status") UserStatus activationStatus) {
        if (!isCurrentUserAdmin()) {
            return prepareResult(null, HttpStatus.UNAUTHORIZED);
        }
        if (activationStatus == null) {
            return prepareResult("The expected request params are empty!", ResponseStatus.FAILED);
        }
        return prepareResult(adminService.findUsersByStatus(activationStatus));
    }

    /**
     * Lists the currently logged-in users
     */
    @RequestMapping(value = "/users/logged", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> findLoggedUsers() {
        if (!isCurrentUserAdmin()) {
            return prepareResult(null, HttpStatus.UNAUTHORIZED);
        }
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
    @RequestMapping(value = "/users/groups", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getGroups() {
        if (!isCurrentUserAdmin()) {
            return prepareResult(null, HttpStatus.UNAUTHORIZED);
        }
        return prepareResult(adminService.getGroups());
    }

    /**
     * Returns the detail of a user account
     * @param userId  The user identifier
     */
    @RequestMapping(value = "/users/{userId}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getUserInfo(@PathVariable("userId") String userId) {
        if (!isCurrentUserAdmin()) {
            return prepareResult(null, HttpStatus.UNAUTHORIZED);
        }
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
     * Updates the user account information
     * @param updatedUserInfo   The user account information
     */
    @RequestMapping(value = "/users/{userId}", method = RequestMethod.PUT, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> updateUserInfo(@PathVariable("userId") String userId,
                                                             @RequestBody User updatedUserInfo) {
        if (!isCurrentUserAdmin()) {
            return prepareResult(null, HttpStatus.UNAUTHORIZED);
        }
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
    @RequestMapping(value = "/users/{userId}/disable", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> disableUser(@PathVariable("userId") String userId, @RequestBody DisableUserInfo additionalDisableActions) {
        if (!isCurrentUserAdmin()) {
            return prepareResult(null, HttpStatus.UNAUTHORIZED);
        }
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
    @RequestMapping(value = "/users/{userId}", method = RequestMethod.DELETE, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> deleteUser(@PathVariable("userId") String userId) {
        if (!isCurrentUserAdmin()) {
            return prepareResult(null, HttpStatus.UNAUTHORIZED);
        }
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
     * Returns the aggregated time spent by users on this instance
     * @param userId (optional) The user identifier. If not specified, the aggregation is done for all users
     * @param from  (optional) The start of the interval to aggregate. If not specified, it is the first timestamp recorded
     * @param to    (optional) The end of the interval to aggregate. If not specified, it is the current moment.
     */
    @RequestMapping(value = "/users/sessions", method = RequestMethod.GET, produces = "application/json")
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
    @RequestMapping(value = "/users/sessions/total", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getTotalProcessingTime(@RequestParam(name = "userId", required = false) String userId) {
        if (!isCurrentUserAdmin() || !currentUser().equals(userId)) {
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
}
