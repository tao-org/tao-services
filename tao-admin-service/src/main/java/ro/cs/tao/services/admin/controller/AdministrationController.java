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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
                final String activationEndpointUrl = configManager.getValue("tao.services.base") + "/user/activate/" + userInfo.getUsername();
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
        final Set<String> names = new HashSet<>();
        final List<User> users = new ArrayList<>();
        names.addAll(sessionRegistry.getAllPrincipals().stream().filter(u->!sessionRegistry.getAllSessions(u, false).isEmpty()).map(Object::toString).collect(Collectors.toList()));
        if (!names.isEmpty()) {
            users.addAll(adminService.getUsers(names));
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
     * @param username  The user account name
     */
    @RequestMapping(value = "/users/{username}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getUserInfo(@PathVariable("username") String username) {
        if (!isCurrentUserAdmin()) {
            return prepareResult(null, HttpStatus.UNAUTHORIZED);
        }
        if (StringUtilities.isNullOrEmpty(username)) {
            return prepareResult("The expected request params are empty!", ResponseStatus.FAILED);
        }
        try {
            final User userInfo = adminService.getUserInfo(username);
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
    @RequestMapping(value = "/users/{username}", method = RequestMethod.PUT, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> updateUserInfo(@RequestBody User updatedUserInfo) {
        if (!isCurrentUserAdmin()) {
            return prepareResult(null, HttpStatus.UNAUTHORIZED);
        }
        if (updatedUserInfo == null) {
            return prepareResult("The expected request body is empty!", ResponseStatus.FAILED);
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
     * @param username  The user account name
     * @param additionalDisableActions  Additional actions to be performed
     */
    @RequestMapping(value = "/users/{username}/disable", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> disableUser(@PathVariable("username") String username, @RequestBody DisableUserInfo additionalDisableActions) {
        if (!isCurrentUserAdmin()) {
            return prepareResult(null, HttpStatus.UNAUTHORIZED);
        }
        if (StringUtilities.isNullOrEmpty(username) || additionalDisableActions == null) {
            return prepareResult("The expected request params are empty!", ResponseStatus.FAILED);
        }
        try {
            // disable user
            adminService.disableUser(username, additionalDisableActions);
            return prepareResult(String.format("%s was disabled", username), ResponseStatus.SUCCEEDED);
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    /**
     * Deletes a user account
     * @param username  The user account name
     */
    @RequestMapping(value = "/users/{username}", method = RequestMethod.DELETE, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> deleteUser(@PathVariable("username") String username) {
        if (!isCurrentUserAdmin()) {
            return prepareResult(null, HttpStatus.UNAUTHORIZED);
        }
        if (StringUtilities.isNullOrEmpty(username)) {
            return prepareResult("The expected request params are empty!", ResponseStatus.FAILED);
        }
        try {
            adminService.deleteUser(username);
            // TODO delete private resources
            /*if (deletePrivateResources){

            }*/
            return prepareResult(String.format("User %s was deleted", username), ResponseStatus.SUCCEEDED);
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    private String constructEmailContentForAccountActivation(String userFullName, String activationLink){
        return Constants.MAIL_CONTENTS.replace("$USERNAME", userFullName).replace("$LINK", activationLink);
    }
}
