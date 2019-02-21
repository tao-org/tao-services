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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.configuration.ConfigurationManager;
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

/**
 * @author Oana H.
 */
@Controller
@RequestMapping("/admin")
public class AdministrationController extends BaseController {

    @Autowired
    private AdministrationService adminService;

    @Autowired
    private TokenManagementService tokenService;

    @RequestMapping(value = "/users", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> addNewUser(@RequestBody User newUserInfo) {
        if (newUserInfo == null) {
            return prepareResult("The expected request body is empty!", ResponseStatus.FAILED);
        }
        try {
            final User userInfo = adminService.addNewUser(newUserInfo);
            if (userInfo != null) {
                //send email with activation link
                final MailSender mailSender = new MailSender();
                final ConfigurationManager configManager = ConfigurationManager.getInstance();
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
        return prepareResult(adminService.getAllUsersUnicityInfo());
    }

    @RequestMapping(value = "/users", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> findUsersByStatus(@RequestParam("status") UserStatus activationStatus) {
        if (activationStatus == null) {
            return prepareResult("The expected request params are empty!", ResponseStatus.FAILED);
        }
        return prepareResult(adminService.findUsersByStatus(activationStatus));
    }

    @RequestMapping(value = "/users/groups", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getGroups() {
        return prepareResult(adminService.getGroups());
    }

    @RequestMapping(value = "/users/{username}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getUserInfo(@PathVariable("username") String username) {
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

    @RequestMapping(value = "/users/{username}", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> updateUserInfo(@RequestBody User updatedUserInfo) {
        if (updatedUserInfo == null) {
            return prepareResult("The expected request body is empty!", ResponseStatus.FAILED);
        }
        try {
            final User userInfo = adminService.updateUserInfo(updatedUserInfo);
            if (userInfo != null) {
                return prepareResult(userInfo);
            } else {
                return prepareResult(null, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    @RequestMapping(value = "/users/{username}/disable", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> disableUser(@PathVariable("username") String username, @RequestBody DisableUserInfo additionalDisableActions) {
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

    @RequestMapping(value = "/users/{username}", method = RequestMethod.DELETE, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> deleteUser(@PathVariable("username") String username) {
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
