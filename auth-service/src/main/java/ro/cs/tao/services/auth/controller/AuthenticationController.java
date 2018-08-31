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
package ro.cs.tao.services.auth.controller;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.AuthenticationService;
import ro.cs.tao.services.model.auth.AuthInfo;
import ro.cs.tao.utils.StringUtils;

import java.io.UnsupportedEncodingException;
import java.util.StringTokenizer;

/**
 * @author Oana H.
 */
@Controller
@RequestMapping("/auth")
public class AuthenticationController extends BaseController {

    @Autowired
    private AuthenticationService authenticationService;

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public ResponseEntity<ServiceResponse<?>> login(@RequestHeader("Authorization") String authHeader) {
        if (StringUtils.isNullOrEmpty(authHeader)) {
            return prepareResult("Expected request header empty!", ResponseStatus.FAILED);
        }

        try {
            final String username = getUsernameFromBasicAuthHeader(authHeader);
            if (StringUtils.isNullOrEmpty(username)) {
                return prepareResult("Empty credentials in request header!", ResponseStatus.FAILED);
            }
            final AuthInfo authInfo = authenticationService.login(username);
            if (authInfo != null && authInfo.isAuthenticated()) {
                return prepareResult(authInfo);
            } else {
                return prepareResult(null, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    @RequestMapping(value = "/logout", method = RequestMethod.POST)
    public ResponseEntity<ServiceResponse<?>> logout(@RequestHeader("X-Auth-Token") String authToken) {
        if (StringUtils.isNullOrEmpty(authToken)) {
            return prepareResult("Expected request header absent!", ResponseStatus.FAILED);
        }

        try {
            if (authenticationService.logout(authToken)) {
                return prepareResult("Logout successful", HttpStatus.OK);
            } else {
                return prepareResult(null, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    private String getUsernameFromBasicAuthHeader(String authHeader) throws UnsupportedEncodingException {
        String username = null;
        if (authHeader != null) {
            StringTokenizer st = new StringTokenizer(authHeader);
            if (st.hasMoreTokens()) {
                String basic = st.nextToken();
                if (basic.equalsIgnoreCase("Basic")) {
                    String credentials = new String(Base64.decodeBase64(st.nextToken()), "UTF-8");
                    int separatorPosition = credentials.indexOf(":");
                    if (separatorPosition != -1) {
                        username = credentials.substring(0, separatorPosition).trim();
                    }
                }
            }
        }
        return username;
    }

}
