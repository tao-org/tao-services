/*
 * Copyright (C) 2017 CS ROMANIA
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
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.services.auth.token.TokenManagementService;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.interfaces.UserService;
import ro.cs.tao.services.model.user.ResetPasswordInfo;
import ro.cs.tao.user.User;
import ro.cs.tao.user.UserPreference;
import ro.cs.tao.utils.StringUtils;

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

    @RequestMapping(value = "/activate/{username}", method = RequestMethod.GET)
    public ResponseEntity<?> activate(@PathVariable("username") String username) {
        if (StringUtils.isNullOrEmpty(username)) {
            return new ResponseEntity<>("The expected request params are empty!", HttpStatus.BAD_REQUEST);
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

                final ConfigurationManager configManager = ConfigurationManager.getInstance();
                final String passwordResetUIUrl = configManager.getValue("tao.ui.base") + configManager.getValue("tao.ui.password.reset") + "?rk=" + passwordResetKey;
                HttpHeaders headers = new HttpHeaders();
                headers.add("Location", passwordResetUIUrl);
                return new ResponseEntity<String>(headers, HttpStatus.TEMPORARY_REDIRECT);
            }
            else {
                // external authenticated users have already a password in the external authentication mechanism
                // redirect to TAO login page from activation email within email that hits this endpoint
                final ConfigurationManager configManager = ConfigurationManager.getInstance();
                final String loginUIUrl = configManager.getValue("tao.ui.base") + configManager.getValue("tao.ui.login");
                HttpHeaders headers = new HttpHeaders();
                headers.add("Location", loginUIUrl);
                return new ResponseEntity<String>(headers, HttpStatus.TEMPORARY_REDIRECT);
            }

        } catch (Exception ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/{username}/reset", method = RequestMethod.POST)
    public ResponseEntity<?> resetPassword(@PathVariable("username") String username, @RequestBody ResetPasswordInfo resetPasswordInfo) {
        if (StringUtils.isNullOrEmpty(username) || resetPasswordInfo == null ||
            StringUtils.isNullOrEmpty(resetPasswordInfo.getResetKey()) || StringUtils.isNullOrEmpty(resetPasswordInfo.getNewPassword())) {
            return new ResponseEntity<>("The expected request params are empty!", HttpStatus.BAD_REQUEST);
        }
        try {
            userService.resetPassword(username, resetPasswordInfo);
            return new ResponseEntity<>("Password reset successfully!", HttpStatus.OK);

        } catch (Exception ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/{username}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> getUserInfo(@PathVariable("username") String username) {
        if (StringUtils.isNullOrEmpty(username)) {
            return new ResponseEntity<>("The expected request params are empty!", HttpStatus.BAD_REQUEST);
        }
        try {
            final User userInfo = userService.getUserInfo(username);
            if (userInfo != null) {
                return new ResponseEntity<>(userInfo, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/{username}", method = RequestMethod.PUT, consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> updateUserInfo(@RequestBody User updatedUserInfo) {
        if (updatedUserInfo == null) {
            return new ResponseEntity<>("The expected request body is empty!", HttpStatus.BAD_REQUEST);
        }
        try {
            final User userInfo = userService.updateUserInfo(updatedUserInfo);
            if (userInfo != null) {
                return new ResponseEntity<>(userInfo, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/prefs", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> saveOrUpdateUserPreferences(@RequestBody List<UserPreference> userPreferences, @RequestHeader("X-Auth-Token") String authToken) {
        if (userPreferences == null || userPreferences.isEmpty() || StringUtils.isNullOrEmpty(authToken)) {
            return new ResponseEntity<>("The expected request body is empty!", HttpStatus.BAD_REQUEST);
        }
        try {
            final String username = tokenService.retrieve(authToken).getPrincipal().toString();
            final List<UserPreference> userUpdatedPrefs = userService.saveOrUpdateUserPreferences(username, userPreferences);
            if (userUpdatedPrefs != null && !userUpdatedPrefs.isEmpty()) {
                return new ResponseEntity<>(userUpdatedPrefs, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/prefs", method = RequestMethod.DELETE, consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> removeUserPreferences(@RequestBody List<String> userPrefsKeysToDelete, @RequestHeader("X-Auth-Token") String authToken) {
        if (userPrefsKeysToDelete == null || userPrefsKeysToDelete.isEmpty() || StringUtils.isNullOrEmpty(authToken)) {
            return new ResponseEntity<>("The expected request body is empty!", HttpStatus.BAD_REQUEST);
        }
        try {
            final String username = tokenService.retrieve(authToken).getPrincipal().toString();
            final List<UserPreference> userUpdatedPrefs = userService.removeUserPreferences(username, userPrefsKeysToDelete);
            if (userUpdatedPrefs != null && !userUpdatedPrefs.isEmpty()) {
                return new ResponseEntity<>(userUpdatedPrefs, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}