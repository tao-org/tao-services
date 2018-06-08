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
package ro.cs.tao.services.auth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ro.cs.tao.services.auth.beans.LoginDetails;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.interfaces.AuthenticationService;
import ro.cs.tao.services.model.auth.AuthInfo;
import ro.cs.tao.utils.StringUtils;

@Controller
@RequestMapping("/auth")
public class AuthenticationController extends BaseController {

    @Autowired
    private AuthenticationService authenticationService;

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public ResponseEntity<?> login(@RequestBody LoginDetails request) {
        if (request == null) {
            return new ResponseEntity<>("No body request present!", HttpStatus.BAD_REQUEST);
        }

        if (StringUtils.isNullOrEmpty(request.getUsername()) || StringUtils.isNullOrEmpty(request.getPassword())) {
            return new ResponseEntity<>("Empty credentials in body request!", HttpStatus.BAD_REQUEST);
        }

        try {
            final AuthInfo authInfo = authenticationService.login(request.getUsername(), request.getPassword());
            if (authInfo != null && authInfo.isAuthenticated()) {
                return new ResponseEntity<>(authInfo, HttpStatus.OK);
            }
            else {
                return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
