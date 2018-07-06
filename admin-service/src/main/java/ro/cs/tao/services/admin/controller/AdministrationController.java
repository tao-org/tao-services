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
import ro.cs.tao.services.admin.mail.MailSenderTLS;
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
                //send email with activation link
                MailSenderTLS mailSenderTLS = new MailSenderTLS();
                String content = constructEmailContentForAccountActivation(userInfo.getFirstName() + " " + userInfo.getLastName(), "http://localhost:8080/user/activate/" + userInfo.getUsername());
                mailSenderTLS.sendMail(userInfo.getEmail(), "TAO - User activation required", content);

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

    private String constructEmailContentForAccountActivation(String userFullName, String activationLink){
        String result = "";
        result +="<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
          "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
          "<head>\n" +
          "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n" +
          "\t<title>Account activation</title>\n" +
          "<style type=\"text/css\">\n" +
          "body{width:100% !important;}\n" +
          "body{margin:0;padding:0;font-family: \"Helvetica Neue\", \"Helvetica\", Helvetica, Arial, sans-serif;font-size:12px;color:#333333;background-color: #FFFFFF;}\n" +
          "img{border:0;line-height:100%;outline:none;text-decoration:none;}\n" +
          "table td{border-collapse:collapse;}\n" +
          "a:link, a:visited {color:#336699;}\n" +
          "</style>\n" +
          "</head>\n" +
          "<body>\n" +
          "<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" height=\"100%\" width=\"100%\" id=\"bodyTable\" bgcolor=\"#38424B\">\n" +
          "    <tr>\n" +
          "        <td align=\"center\" valign=\"top\">\n" +
          "            <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"600\" id=\"emailContainer\">\n" +
          "                <tr>\n" +
          "                    <td align=\"center\" valign=\"top\">\n" +
          "                        <table border=\"0\" cellpadding=\"20\" cellspacing=\"0\" width=\"100%\" id=\"emailHeader\">\n" +
          "                            <tr>\n" +
          "\t\t\t\t\t\t\t\t<td align=\"center\" valign=\"top\" style=\"border-collapse:collapse;color:#ffffff;font-size:14px;line-height:150%;text-align:center;\">\n" +
          "TAO Platform - Account activation\n" +
          "                                </td>\n" +
          "                            </tr>\n" +
          "                        </table>\n" +
          "                    </td>\n" +
          "                </tr>\n" +
          "                <tr>\n" +
          "                    <td align=\"center\" valign=\"top\">\n" +
          "                        <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" id=\"emailBody\" bgcolor=\"#ffffff\">\n" +
          "                            <tr>\n" +
          "                                <td align=\"left\" valign=\"top\" style=\"padding:10px;\">\n" +
          "<p>\n" +
          "To: " + userFullName + "\n" +
          "<br><br>Hello! <br>You received this email as a consequence of your account creation. <br>For activating you account click on the following activation link: </br><strong><a href=\"" + activationLink + "\">Activate your account</a></strong>. \n" +
          "</p>\n" +
          "\n" +
          "                                </td>\n" +
          "                            </tr>\n" +
          "                        </table>\n" +
          "                    </td>\n" +
          "                </tr>\n" +
          "                <tr>\n" +
          "                    <td align=\"center\" valign=\"top\">\n" +
          "                        <table border=\"0\" cellpadding=\"20\" cellspacing=\"0\" width=\"100%\" id=\"emailFooter\">\n" +
          "                            <tr>\n" +
          "                                <td align=\"center\" valign=\"top\" style=\"border-collapse:collapse;color:#ffffff;font-size:12px;line-height:150%;text-align:center;\">\n" +
          "<em>TAO Administration</em>\n" +
          "                                </td>\n" +
          "                            </tr>\n" +
          "                        </table>\n" +
          "                    </td>\n" +
          "                </tr>\n" +
          "            </table>\n" +
          "        </td>\n" +
          "    </tr>\n" +
          "</table>\n" +
          "</body>\n" +
          "</html>";

        return result;
    }
}
