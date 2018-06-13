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

import ro.cs.tao.services.auth.token.TokenManagementService;
import ro.cs.tao.services.interfaces.AuthenticationService;
import ro.cs.tao.services.model.auth.AuthInfo;

import java.util.logging.Logger;

/**
 *
 * @author Oana H.
 */
@Service("authenticationService")
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final Logger logger = Logger.getLogger(AuthenticationServiceImpl.class.getName());

    @Autowired
    private TokenManagementService tokenService;

    @Override
    public AuthInfo login(String username, String password) {
        logger.info("Login (" + username + ")...");
        // if arrived here, this means that the JAAS login was successful

        String authenticationToken = tokenService.getUserToken(username);
        logger.info("Token " + authenticationToken);

        // TODO retrieve user group and send it as profile
        return new AuthInfo(true, authenticationToken, null);
    }

    @Override
    public void logout(String username) {

    }
}
