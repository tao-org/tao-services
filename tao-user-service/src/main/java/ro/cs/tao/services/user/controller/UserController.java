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
package ro.cs.tao.services.user.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.configuration.ConfigurationProvider;
import ro.cs.tao.persistence.AuditProvider;
import ro.cs.tao.persistence.RepositoryProvider;
import ro.cs.tao.services.auth.token.TokenManagementService;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.RoleRequired;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.RepositoryWatcherService;
import ro.cs.tao.services.interfaces.UserService;
import ro.cs.tao.services.model.user.ResetPasswordInfo;
import ro.cs.tao.services.user.impl.UserInfo;
import ro.cs.tao.user.SessionDuration;
import ro.cs.tao.user.User;
import ro.cs.tao.user.UserPreference;
import ro.cs.tao.user.UserType;
import ro.cs.tao.utils.FileUtilities;
import ro.cs.tao.utils.StringUtilities;
import ro.cs.tao.workspaces.Repository;
import ro.cs.tao.workspaces.RepositoryType;

import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * @author Oana H.
 */
@RestController
@RequestMapping("/user")
@Tag(name = "User Management", description = "Operations related to user management")
public class UserController extends BaseController {

    @Autowired
    private UserService userService;

    @Autowired
    private TokenManagementService tokenService;

    @Autowired
    private RepositoryWatcherService repositoryWatcherService;

    @Autowired
    private RepositoryProvider repositoryProvider;

    @Autowired
    private AuditProvider auditProvider;

    /**
     * Returns the groups of users.
     */
    @RequestMapping(value = "/groups/", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getGroups() {
        return prepareResult(userService.getGroups());
    }

    /**
     * Activates the given user account.
     * @param userId    The user account identifier
     */
    @RequestMapping(value = "/activate/{userId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> activate(@PathVariable("userId") String userId) {
        if (StringUtilities.isNullOrEmpty(userId)) {
            return prepareResult("The expected request params are empty!", ResponseStatus.FAILED);
        }
        try {
            userService.activateUser(userId);

            // we need to know if the user is TAO internal or external
            final User userInfo = userService.getUserInfo(userId);
            final ConfigurationProvider configManager = ConfigurationManager.getInstance();
            repositoryWatcherService.registerUser(userId);
            if (userInfo.getUserType() == UserType.INTERNAL) {
                // internal TAO authenticated users need to set a password
                // we need a redirect to TAO reset password page from activation email within email that hits this endpoint
                final String passwordResetKey = UUID.randomUUID().toString();
                // save the reset key on user entity
                userInfo.setPasswordResetKey(passwordResetKey);
                userService.updateUserInfo(userInfo);


                final String passwordResetUIUrl = configManager.getValue("tao.ui.base") + configManager.getValue("tao.ui.password.reset") + "?rk=" + passwordResetKey;
                HttpHeaders headers = new HttpHeaders();
                headers.add("Location", passwordResetUIUrl);
                return prepareResult(headers, HttpStatus.TEMPORARY_REDIRECT);
            }
            else {
                // external authenticated users have already a password in the external authentication mechanism
                // redirect to TAO login page from activation email within email that hits this endpoint
                final String loginUIUrl = configManager.getValue("tao.ui.base") + configManager.getValue("tao.ui.login");
                HttpHeaders headers = new HttpHeaders();
                headers.add("Location", loginUIUrl);
                return prepareResult(headers, HttpStatus.TEMPORARY_REDIRECT);
            }

        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    /**
     * Initializes the default repositories for the given user account.
     * If the repositories were already initialized, nothing happens.
     * Only an administrator is allowed to call this endpoint.
     *
     * @param userId The user account identifier
     */
    @RequestMapping(value = "/initialize", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @RoleRequired(roles = "admin")
    public ResponseEntity<ServiceResponse<?>> createWorkspaces(@RequestParam("userId") String userId) {
        try {
            if (StringUtilities.isNullOrEmpty(userId)) {
                throw new IllegalArgumentException("Invalid user id");
            }
            //if (isCurrentUserAdmin() || SystemPrincipal.instance().getName().equals(currentUser())) {
                userService.createWorkspaces(userId);
            /*} else {
                return prepareResult("Not authorized", HttpStatus.UNAUTHORIZED);
            }*/
            return prepareResult("Workspace initialized", ResponseStatus.SUCCEEDED);
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    /**
     * Resets the password of the given user account
     * @param userId    The user account identifier
     * @param resetPasswordInfo Structure holding the reset key and the new password
     */
    @RequestMapping(value = "/{userId}/reset", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> resetPassword(@PathVariable("userId") String userId, @RequestBody ResetPasswordInfo resetPasswordInfo) {
        if (StringUtilities.isNullOrEmpty(userId) || resetPasswordInfo == null ||
                StringUtilities.isNullOrEmpty(resetPasswordInfo.getResetKey()) || StringUtilities.isNullOrEmpty(resetPasswordInfo.getNewPassword())) {
            return prepareResult("The expected request params are empty!", ResponseStatus.FAILED);
        }
        if (!currentUser().equals(userId) || !isCurrentUserAdmin()) {
            return prepareResult("Not allowed", ResponseStatus.FAILED);
        }
        try {
            userService.resetPassword(userId, resetPasswordInfo);
            return prepareResult("Password reset successfully!", ResponseStatus.SUCCEEDED);

        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    /**
     * Returns the user profile
     * @param userId    The user account identifier
     */
    @RequestMapping(value = "/{userId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getUserInfo(@PathVariable("userId") String userId) {
        if (StringUtilities.isNullOrEmpty(userId)) {
            return prepareResult("The expected request params are empty!", ResponseStatus.FAILED);
        }
        try {
            final User user = userService.getUserInfo(userId);
            if (user != null) {
                final int processingTime = auditProvider.getAggregatedUserProcessingTime(userId);
                final List<SessionDuration> sessions = auditProvider.getUserSessions(userId);
                int sessionTime = sessions != null ? (int) sessions.stream().mapToDouble(SessionDuration::getDuration).sum() : 0;
                final UserInfo userInfo = new UserInfo(user, sessionTime, processingTime);
                if (userInfo.getActualInputQuota() == -1) {
                    Repository repository = getLocalRepository(currentUser());
                    userInfo.setActualInputQuota((int) (FileUtilities.folderSize(Paths.get(repository.root())) >> 20));
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
     * Returns the user login name
     * @param userId    The user account identifier
     */
    @RequestMapping(value = "/name/{userId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getUserName(@PathVariable("userId") String userId) {
        if (StringUtilities.isNullOrEmpty(userId)) {
            return prepareResult("The expected request params are empty!", ResponseStatus.FAILED);
        }
        try {
            final User user = userService.getUserInfo(userId);
            if (user != null) {
                return prepareResult(user.getUsername());
            } else {
                return prepareResult(null, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    /**
     * Updates the user profile
     * @param updatedUserInfo   The user profile structure
     */
    @RequestMapping(value = "/{userId}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> updateUserInfo(@RequestBody User updatedUserInfo) {
        if (updatedUserInfo == null) {
            return prepareResult("The expected request body is empty!", ResponseStatus.FAILED);
        }
        try {
            final User userInfo = userService.updateUserInfo(updatedUserInfo);
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
     * Saves the preferences of an account
     * @param userPreferences   The preferences list as the body of the request
     * @param authToken         The authentication token as header
     */
    @RequestMapping(value = "/prefs", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> saveOrUpdateUserPreferences(@RequestBody List<UserPreference> userPreferences,
                                                         @RequestHeader("X-Auth-Token") String authToken) {
        if (userPreferences == null || userPreferences.isEmpty() || StringUtilities.isNullOrEmpty(authToken)) {
            return prepareResult("The expected request body is empty!", ResponseStatus.FAILED);
        }
        try {
            final String userId = tokenService.retrieve(authToken).getPrincipal().toString();
            final List<UserPreference> userUpdatedPrefs = userService.saveOrUpdateUserPreferences(userId, userPreferences);
            if (userUpdatedPrefs != null && !userUpdatedPrefs.isEmpty()) {
                return prepareResult(userUpdatedPrefs);
            } else {
                return prepareResult(null, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    /**
     * Deletes the preferences of an account
     * @param userPrefsKeysToDelete The preferences list as the body of the request
     * @param authToken The authentication token as header
     */
    @RequestMapping(value = "/prefs", method = RequestMethod.DELETE, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> removeUserPreferences(@RequestBody List<String> userPrefsKeysToDelete,
                                                                    @RequestHeader("X-Auth-Token") String authToken) {
        if (StringUtilities.isNullOrEmpty(authToken)) {
            return prepareResult("Invalid token!", ResponseStatus.FAILED);
        }
        try {
            final String userId = tokenService.retrieve(authToken).getPrincipal().toString();
            return prepareResult(userService.removeUserPreferences(userId, userPrefsKeysToDelete));
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    private Repository getLocalRepository(String userId) {
        return repositoryProvider.getUserSystemRepositories(userId).stream().filter(w -> w.getType() == RepositoryType.LOCAL).findFirst().orElse(null);
    }
}
