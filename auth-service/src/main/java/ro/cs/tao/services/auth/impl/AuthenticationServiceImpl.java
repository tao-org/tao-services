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

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.springframework.stereotype.Service;
import ro.cs.tao.services.interfaces.AuthenticationService;
import ro.cs.tao.services.model.auth.AuthInfo;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.logging.Logger;

@Service("authenticationService")
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final Logger logger = Logger.getLogger(AuthenticationServiceImpl.class.getName());

    @Override
    public AuthInfo login(String username, String password) {
        logger.info("Login called (" + username + ")...");
        // if arrived here, this means that the JAAS login was successful

        String key = UUID.randomUUID().toString().toUpperCase() +
          "|" + username +
          "|" + LocalDateTime.now();

        StandardPBEStringEncryptor jasypt = new StandardPBEStringEncryptor();
        jasypt.setPassword("ARwkYz");
        // this is the authentication token user will send in order to use the web service
        String authenticationToken = jasypt.encrypt(key);

        // TODO retrieve user group and send it as profile
        return new AuthInfo(true, authenticationToken, null);
    }

    @Override
    public void logout(String username) {

    }
}
