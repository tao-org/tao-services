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

import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.security.Token;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.AuthenticationService;
import ro.cs.tao.services.model.auth.AuthInfo;
import ro.cs.tao.utils.StringUtilities;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.StringTokenizer;

/**
 * @author Oana H.
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Operations related to user authentication")
public class AuthenticationController extends BaseController {

    @Autowired
    private AuthenticationService authenticationService;

    /**
     * Logs in a user
     * @param user      The user account
     * @param password  The user password
     */
    @RequestMapping(value = "/login", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> login(@RequestParam("username") String user,
                                                    @RequestParam("password") String password) {
        boolean success = true;
        try {
            final AuthInfo authInfo = authenticationService.login(user, password);
            if (authInfo != null && authInfo.isAuthenticated()) {
                if (authInfo.getRefreshToken() != null) {
                    HttpHeaders headers = new HttpHeaders();
                    headers.add("Set-Cookie", "refreshToken=" + authInfo.getRefreshToken() + ";Max-Age=" + authInfo.getExpiration());
                    return ResponseEntity.status(HttpStatus.OK).headers(headers).body(new ServiceResponse<>(authInfo));
                } else {
                    return prepareResult(authInfo);
                }
            } else {
                success = false;
                return prepareResult(null, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception ex) {
            success = false;
            return handleException(ex);
        } finally {
            if (success) {
                debug("Successful login: %s", user);
            } else {
                warn("Login failed: %s", user);
            }
        }
    }

    /**
     * Logs out a user
     * @param authToken The authentication token
     */
    @RequestMapping(value = "/logout", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> logout(@RequestHeader("X-Auth-Token") String authToken) {
        if (StringUtilities.isNullOrEmpty(authToken)) {
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

    /**
     * Refreshes the authentication token for a user
     * @param userName      The user login
     * @param refreshToken  The refresh token to be used
     */
    @RequestMapping(value = "/refresh", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> refresh(@RequestParam(name = "user") String userName,
                                                      @RequestParam(name = "token") String refreshToken,
                                                      HttpServletRequest request) {
        boolean success = false;
        final String remoteAddr = request.getRemoteAddr();
        try {
            final Token newToken = authenticationService.getNewToken(userName, refreshToken);
            if (newToken != null) {
                HttpHeaders headers = new HttpHeaders();
                headers.add("Set-Cookie", "refreshToken=" + newToken.getRefreshToken() + ";Max-Age=" + newToken.getExpiresInSeconds());
                success = true;
                return ResponseEntity.status(HttpStatus.OK).headers(headers).body(new ServiceResponse<>(newToken));
            } else {
                return prepareResult(null, HttpStatus.BAD_REQUEST);
            }
        } catch (Exception ex) {
            //return handleException(ex);
            return prepareResult(ex.getMessage(), ResponseStatus.FAILED);
        } finally {
            if (success) {
                trace("Token refreshed [user: %s, addr: %s]", userName, remoteAddr);
            } else {
                warn("Token refresh failed [user: %s, addr: %s]", userName);
            }
        }
    }

    private String getUsernameFromBasicAuthHeader(String authHeader) throws UnsupportedEncodingException {
        String username = null;
        if (authHeader != null) {
            StringTokenizer st = new StringTokenizer(authHeader);
            if (st.hasMoreTokens()) {
                String basic = st.nextToken();
                if (basic.equalsIgnoreCase("Basic")) {
                    String credentials = new String(Base64.decodeBase64(st.nextToken()), StandardCharsets.UTF_8);
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
