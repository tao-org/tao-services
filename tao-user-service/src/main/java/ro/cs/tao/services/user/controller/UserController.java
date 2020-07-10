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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.configuration.TaoConfigurationProvider;
import ro.cs.tao.services.auth.token.TokenManagementService;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.UserService;
import ro.cs.tao.services.model.user.ResetPasswordInfo;
import ro.cs.tao.user.User;
import ro.cs.tao.user.UserPreference;
import ro.cs.tao.utils.StringUtilities;

import java.util.List;
import java.util.UUID;

/**
 * @author Oana H.
 */
@Controller
@RequestMapping("/user")
public class UserController extends BaseController {

    @Autowired
    private UserService userService;

    @Autowired
    private TokenManagementService tokenService;

    @RequestMapping(value = "/activate/{username}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> activate(@PathVariable("username") String username) {
        if (StringUtilities.isNullOrEmpty(username)) {
            return prepareResult("The expected request params are empty!", ResponseStatus.FAILED);
        }
        try {
            userService.activateUser(username);

            // we need to know if the user is TAO internal or external
            final User userInfo = userService.getUserInfo(username);

            if (!userInfo.isExternal()) {
                // internal TAO authenticated users need to set a password
                // we need a redirect to TAO reset password page from activation email within email that hits this endpoint
                final String passwordResetKey = UUID.randomUUID().toString();
                // save the reset key on user entity
                userInfo.setPasswordResetKey(passwordResetKey);
                userService.updateUserInfo(userInfo);

                final TaoConfigurationProvider configManager = TaoConfigurationProvider.getInstance();
                final String passwordResetUIUrl = configManager.getValue("tao.ui.base") + configManager.getValue("tao.ui.password.reset") + "?rk=" + passwordResetKey;
                HttpHeaders headers = new HttpHeaders();
                headers.add("Location", passwordResetUIUrl);
                return prepareResult(headers, HttpStatus.TEMPORARY_REDIRECT);
            }
            else {
                // external authenticated users have already a password in the external authentication mechanism
                // redirect to TAO login page from activation email within email that hits this endpoint
                final TaoConfigurationProvider configManager = TaoConfigurationProvider.getInstance();
                final String loginUIUrl = configManager.getValue("tao.ui.base") + configManager.getValue("tao.ui.login");
                HttpHeaders headers = new HttpHeaders();
                headers.add("Location", loginUIUrl);
                return prepareResult(headers, HttpStatus.TEMPORARY_REDIRECT);
            }

        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    @RequestMapping(value = "/{username}/reset", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> resetPassword(@PathVariable("username") String username, @RequestBody ResetPasswordInfo resetPasswordInfo) {
        if (StringUtilities.isNullOrEmpty(username) || resetPasswordInfo == null ||
                StringUtilities.isNullOrEmpty(resetPasswordInfo.getResetKey()) || StringUtilities.isNullOrEmpty(resetPasswordInfo.getNewPassword())) {
            return prepareResult("The expected request params are empty!", ResponseStatus.FAILED);
        }
        try {
            userService.resetPassword(username, resetPasswordInfo);
            return prepareResult("Password reset successfully!", ResponseStatus.SUCCEEDED);

        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    @RequestMapping(value = "/{username}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getUserInfo(@PathVariable("username") String username) {
        if (StringUtilities.isNullOrEmpty(username)) {
            return prepareResult("The expected request params are empty!", ResponseStatus.FAILED);
        }
        try {
            final User userInfo = userService.getUserInfo(username);
            if (userInfo != null) {
                return prepareResult(userInfo);
            } else {
                return prepareResult(null, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    @RequestMapping(value = "/{username}", method = RequestMethod.PUT, consumes = "application/json", produces = "application/json")
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

    @RequestMapping(value = "/prefs", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> saveOrUpdateUserPreferences(@RequestBody List<UserPreference> userPreferences, @RequestHeader("X-Auth-Token") String authToken) {
        if (userPreferences == null || userPreferences.isEmpty() || StringUtilities.isNullOrEmpty(authToken)) {
            return prepareResult("The expected request body is empty!", ResponseStatus.FAILED);
        }
        try {
            final String username = tokenService.retrieve(authToken).getPrincipal().toString();
            final List<UserPreference> userUpdatedPrefs = userService.saveOrUpdateUserPreferences(username, userPreferences);
            if (userUpdatedPrefs != null && !userUpdatedPrefs.isEmpty()) {
                return prepareResult(userUpdatedPrefs);
            } else {
                return prepareResult(null, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    @RequestMapping(value = "/prefs", method = RequestMethod.DELETE, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> removeUserPreferences(@RequestBody List<String> userPrefsKeysToDelete, @RequestHeader("X-Auth-Token") String authToken) {
        if (userPrefsKeysToDelete == null || userPrefsKeysToDelete.isEmpty() || StringUtilities.isNullOrEmpty(authToken)) {
            return prepareResult("The expected request body is empty!", ResponseStatus.FAILED);
        }
        try {
            final String username = tokenService.retrieve(authToken).getPrincipal().toString();
            final List<UserPreference> userUpdatedPrefs = userService.removeUserPreferences(username, userPrefsKeysToDelete);
            if (userUpdatedPrefs != null && !userUpdatedPrefs.isEmpty()) {
                return prepareResult(userUpdatedPrefs);
            } else {
                return prepareResult(null, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception ex) {
            return handleException(ex);
        }
    }
}
