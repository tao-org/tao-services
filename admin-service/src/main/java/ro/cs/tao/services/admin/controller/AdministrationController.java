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
package ro.cs.tao.services.admin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.services.auth.token.TokenManagementService;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.interfaces.AdministrationService;
import ro.cs.tao.services.model.user.UserUnicityInfo;
import ro.cs.tao.user.Group;
import ro.cs.tao.user.User;
import ro.cs.tao.user.UserStatus;
import ro.cs.tao.utils.StringUtils;

import java.util.List;

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

    @RequestMapping(value = "/users", method = RequestMethod.POST)
    public ResponseEntity<?> addNewUser(@RequestBody User newUserInfo) {
        if (newUserInfo == null) {
            return new ResponseEntity<>("The expected request body is empty!", HttpStatus.BAD_REQUEST);
        }
        try {
            final User userInfo = adminService.addNewUser(newUserInfo);
            if (userInfo != null) {
                // TODO send email with activation link

                return new ResponseEntity<>(userInfo, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
            }

        } catch (Exception ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/users/unicity", method = RequestMethod.GET)
    public ResponseEntity<?> getAllUsersUnicityInfo() {
        final List<UserUnicityInfo> userUnicityInfos = adminService.getAllUsersUnicityInfo();
        return new ResponseEntity<>(userUnicityInfos, HttpStatus.OK);
    }

    @RequestMapping(value = "/users/{status}", method = RequestMethod.GET)
    public ResponseEntity<?> findUsersByStatus(@PathVariable("status") UserStatus activationStatus) {
        if (activationStatus == null) {
            return new ResponseEntity<>("The expected request params are empty!", HttpStatus.BAD_REQUEST);
        }
        final List<User> users = adminService.findUsersByStatus(activationStatus);
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @RequestMapping(value = "/users/groups", method = RequestMethod.GET)
    public ResponseEntity<?> getGroups() {
        final List<Group> groups = adminService.getGroups();
        return new ResponseEntity<>(groups, HttpStatus.OK);
    }

    @RequestMapping(value = "/users/{username}", method = RequestMethod.GET)
    public ResponseEntity<?> getUserInfo(@PathVariable("username") String username) {
        if (StringUtils.isNullOrEmpty(username)) {
            return new ResponseEntity<>("The expected request params are empty!", HttpStatus.BAD_REQUEST);
        }
        try {
            final User userInfo = adminService.getUserInfo(username);
            if (userInfo != null) {
                return new ResponseEntity<>(userInfo, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/users/{username}", method = RequestMethod.POST)
    public ResponseEntity<?> updateUserInfo(@RequestBody User updatedUserInfo) {
        if (updatedUserInfo == null) {
            return new ResponseEntity<>("The expected request body is empty!", HttpStatus.BAD_REQUEST);
        }
        try {
            final User userInfo = adminService.updateUserInfo(updatedUserInfo);
            if (userInfo != null) {
                return new ResponseEntity<>(userInfo, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/users/{username}/disable", method = RequestMethod.POST)
    public ResponseEntity<?> disableUser(@PathVariable("username") String username, @RequestBody Boolean deletePrivateResources) {
        if (StringUtils.isNullOrEmpty(username) || deletePrivateResources == null) {
            return new ResponseEntity<>("The expected request params are empty!", HttpStatus.BAD_REQUEST);
        }
        try {
            // disable user
            adminService.disableUser(username);

            // TODO delete private resources
            /*if (deletePrivateResources){

            }*/

            return new ResponseEntity<>(null, HttpStatus.OK);

        } catch (Exception ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/users/{username}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteUser(@PathVariable("username") String username) {
        if (StringUtils.isNullOrEmpty(username)) {
            return new ResponseEntity<>("The expected request params are empty!", HttpStatus.BAD_REQUEST);
        }
        try {
            adminService.deleteUser(username);

            // TODO delete private resources
            /*if (deletePrivateResources){

            }*/

            return new ResponseEntity<>(null, HttpStatus.OK);

        } catch (Exception ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
