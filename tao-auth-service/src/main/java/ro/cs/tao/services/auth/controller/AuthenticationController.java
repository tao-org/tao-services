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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.messaging.Messaging;
import ro.cs.tao.messaging.Topic;
import ro.cs.tao.persistence.AuditProvider;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.UserProvider;
import ro.cs.tao.security.Token;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.AuthenticationService;
import ro.cs.tao.services.model.auth.AuthInfo;
import ro.cs.tao.user.LogEvent;
import ro.cs.tao.user.User;
import ro.cs.tao.user.UserStatus;
import ro.cs.tao.utils.StringUtilities;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * @author Oana H.
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Operations related to user authentication")
public class AuthenticationController extends BaseController {

    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private AuditProvider auditProvider;
    @Autowired
    private UserProvider userProvider;

    /**
     * Logs in a user
     * @param user      The user account
     * @param password  The user password
     */
    @RequestMapping(value = "/login", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> login(@RequestParam(name = "username", required = false) String user,
                                                    @RequestParam(name = "password", required = false) String password,
                                                    @RequestParam(name = "code", required = false) String code,
                                                    HttpServletRequest request) {
        boolean success = true;
        try {
            final AuthInfo authInfo;
            if (!StringUtilities.isNullOrEmpty(code)) {
                authInfo = authenticationService.loginWithCode(currentUser());
            } else {
                authInfo = authenticationService.login(user, password);
            }
            final User userObj = userProvider.get(currentUser());
            if (authInfo != null && authInfo.isAuthenticated() && userObj.getStatus() == UserStatus.ACTIVE) {
                final LogEvent event = new LogEvent();
                event.setUserId(authInfo.getUserId());
                event.setTimestamp(LocalDateTime.now());
                event.setEvent("Login");
                try {
                    auditProvider.save(event);
                } catch (PersistenceException e) {
                    error(e.getMessage());
                }
                if (StringUtilities.isNullOrEmpty(user)) {
                    user = userObj.getUsername();
                }
                if (authInfo.getRefreshToken() != null) {
                    HttpHeaders headers = new HttpHeaders();
                    headers.add("Set-Cookie", "refreshToken=" + authInfo.getRefreshToken() + ";Max-Age=" + authInfo.getExpiration());
                    return ResponseEntity.status(HttpStatus.OK).headers(headers).body(new ServiceResponse<>(authInfo));
                } else {
                    Messaging.send(currentUser(), Topic.AUTHENTICATION.value(), "Login " + currentUser());
                    return prepareResult(authInfo);
                }
            } else {
                success = false;
                request.getSession().invalidate();
                HttpHeaders headers = new HttpHeaders();
                headers.add("Set-Cookie", "JSESSIONID=;Expires=" + LocalDateTime.now().minusMinutes(1));
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).headers(headers).body(new ServiceResponse<>());
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
    @RequestMapping(value = "/logout", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> logout(@RequestHeader("X-Auth-Token") String authToken,
                                                     HttpServletRequest request) {
        if (StringUtilities.isNullOrEmpty(authToken)) {
            return prepareResult("Expected request header absent!", ResponseStatus.FAILED);
        }
        try {
            if (authenticationService.logout(authToken)) {
                Messaging.send(currentUser(), Topic.AUTHENTICATION.value(), "Logout " + currentUser());
                request.getSession().invalidate();
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
     * @param userId      The user identifier
     * @param refreshToken  The refresh token to be used
     */
    @RequestMapping(value = "/refresh", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> refresh(@RequestParam(name = "user") String userId,
                                                      @RequestParam(name = "token") String refreshToken,
                                                      HttpServletRequest request) {
        boolean success = false;
        final String remoteAddr = request.getRemoteAddr();
        try {
            final Token newToken = authenticationService.getNewToken(userId, refreshToken);
            if (newToken != null) {
                HttpHeaders headers = new HttpHeaders();
                headers.add("Set-Cookie", "refreshToken=" + newToken.getRefreshToken() + ";Max-Age=" + newToken.getExpiresInSeconds());
                success = true;
                return ResponseEntity.status(HttpStatus.OK).headers(headers).body(new ServiceResponse<>(newToken));
            } else {
                return prepareResult(null, HttpStatus.BAD_REQUEST);
            }
        } catch (Exception ex) {
            return prepareResult(ex.getMessage(), ResponseStatus.FAILED);
        } finally {
            if (success) {
                trace("Token refreshed [user: %s, addr: %s]", userId, remoteAddr);
            } else {
                warn("Token refresh failed [user: %s, addr: %s]", userId, remoteAddr);
            }
        }
    }
}
