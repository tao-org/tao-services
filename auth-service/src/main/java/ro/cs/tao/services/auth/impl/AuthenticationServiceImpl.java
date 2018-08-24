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
package ro.cs.tao.services.auth.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.services.auth.token.TokenManagementService;
import ro.cs.tao.services.interfaces.AuthenticationService;
import ro.cs.tao.services.model.auth.AuthInfo;
import ro.cs.tao.user.Group;
import ro.cs.tao.user.User;
import ro.cs.tao.user.UserStatus;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author Oana H.
 */
@Service("authenticationService")
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final Logger logger = Logger.getLogger(AuthenticationServiceImpl.class.getName());

    @Autowired
    private TokenManagementService tokenService;

    @Autowired
    private PersistenceManager persistenceManager;

    @Override
    public AuthInfo login(String username) {
        logger.info("Logged in (" + username + ")...");
        // if arrived here, this means that the JAAS login was successful

        String authenticationToken = tokenService.getUserToken(username);
        logger.info("Token " + authenticationToken);

        // check if user is still active in TAO
        final User user = persistenceManager.findUserByUsername(username);
        if (user != null && user.getStatus().value() == UserStatus.ACTIVE.value()) {
            // update user last login date
            persistenceManager.updateUserLastLoginDate(user.getId(), LocalDateTime.now(Clock.systemUTC()));
            // retrieve user groups and send them as profiles
            return new AuthInfo(true, authenticationToken, persistenceManager.getUserGroups(username).stream().map(Group::getName).collect(Collectors.toList()));
        }
        // unauthorized
        return new AuthInfo(false, null, null);
    }

    @Override
    public boolean logout(String authToken) {
        if (tokenService.contains(authToken)) {
            final String username = tokenService.retrieve(authToken).getPrincipal().toString();
            logger.info("Logging out (" + username + ")...");
            tokenService.removeUserTokens(username);
            return true;
        }
        else {
            logger.info("Invalid auth token received at logout: " + authToken);
            return false;
        }
    }
}
