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
package ro.cs.tao.services.auth.impl;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.persistence.RepositoryProvider;
import ro.cs.tao.persistence.UserProvider;
import ro.cs.tao.security.Token;
import ro.cs.tao.services.auth.token.TokenManagementService;
import ro.cs.tao.services.commons.WorkspaceCreator;
import ro.cs.tao.services.interfaces.AuthenticationService;
import ro.cs.tao.services.model.auth.AuthInfo;
import ro.cs.tao.user.Group;
import ro.cs.tao.user.User;
import ro.cs.tao.user.UserStatus;
import ro.cs.tao.utils.FileUtilities;
import ro.cs.tao.workspaces.Repository;
import ro.cs.tao.workspaces.RepositoryFactory;
import ro.cs.tao.workspaces.RepositoryType;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
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
    private UserProvider userProvider;

    @Autowired
    private RepositoryProvider workspaceProvider;

    @Override
    public AuthInfo login(String userName, String password) {

        final String userId = userProvider.getId(userName);
        Token authenticationToken = tokenService.getUserToken(userId);
        if (authenticationToken != null) {
            logger.finest("Token " + authenticationToken.getToken());

            // check if user is still active in TAO
            final User user = userProvider.getByName(userName);
            if (user != null && user.getStatus() == UserStatus.ACTIVE) {
                try {
                    // update user last login date
                    userProvider.updateLastLoginDate(user.getId(), LocalDateTime.now(Clock.systemUTC()));
                    final List<Repository> list = workspaceProvider.getByUser(user.getId());
                    final Set<RepositoryType> types = RepositoryFactory.getDefaultRepositoryTypes();
                    if (list == null || list.isEmpty()) {
                        userProvider.createWorkspaces(user, new WorkspaceCreator());
                    } else {
                        for (Repository repository : list) {
                            types.remove(repository.getType());
                        }
                        // Some of the default repository types were not created for user
                        if (!types.isEmpty()) {
                            userProvider.createWorkspaces(user, new WorkspaceCreator());
                        }
                    }
                    Path path = Paths.get(SystemVariable.ROOT.value()).resolve(user.getId());
                    FileUtilities.ensureExists(path);
                    FileUtilities.ensureExists(path.resolve("files"));
                } catch (Exception e) {
                    logger.severe(String.format("Cannot create workspace for user %s. Reason: %s",
                                                user.getId(), ExceptionUtils.getStackTrace(e)));
                }
                // retrieve user groups and send them as profiles
                return new AuthInfo(true, userId, userName, authenticationToken.getToken(),
                                    authenticationToken.getRefreshToken(), authenticationToken.getExpiresInSeconds(),
                                    user.getGroups().stream().map(Group::getName).collect(Collectors.toList()));
            }
        }
        // unauthorized
        return new AuthInfo(false, null, null, null, null, -1, null);
    }

    @Override
    public AuthInfo loginWithCode(String userId) {
        Token authenticationToken = tokenService.getUserToken(userId);
        if (authenticationToken != null) {
            logger.finest("Token " + authenticationToken.getToken());

            // check if user is still active in TAO
            final Authentication authentication = tokenService.retrieve(authenticationToken.getToken());
            if (authentication != null) {
                //final String userId = userProvider.getId(authentication.getName());
                final User user = userProvider.get(userId);
                if (user != null && user.getStatus() == UserStatus.ACTIVE) {
                    try {
                        // update user last login date
                        userProvider.updateLastLoginDate(user.getId(), LocalDateTime.now(Clock.systemUTC()));
                        final List<Repository> list = workspaceProvider.getByUser(user.getId());
                        if (list == null || list.isEmpty()) {
                            userProvider.createWorkspaces(user, new WorkspaceCreator());
                        }
                        Path path = Paths.get(SystemVariable.ROOT.value()).resolve(user.getId());
                        FileUtilities.ensureExists(path);
                        FileUtilities.ensureExists(path.resolve("files"));
                    } catch (Exception e) {
                        logger.severe(String.format("Cannot create workspace for user %s. Reason: %s",
                                                    user.getId(), ExceptionUtils.getStackTrace(e)));
                    }
                    // retrieve user groups and send them as profiles
                    return new AuthInfo(true, userId, user.getUsername(), authenticationToken.getToken(),
                                        authenticationToken.getRefreshToken(), authenticationToken.getExpiresInSeconds(),
                                        user.getGroups().stream().map(Group::getName).collect(Collectors.toList()));
                }
            }
        }
        // unauthorized
        return new AuthInfo(false, null, null, null, null, -1, null);
    }

    @Override
    public boolean logout(String authToken) {
        if (tokenService.isValid(authToken)) {
            final String userId = tokenService.retrieve(authToken).getPrincipal().toString();
            logger.finest("Logging out (" + userId + ")...");
            tokenService.removeUserTokens(userId);
            return true;
        }
        else {
            logger.finest("Invalid auth token received at logout: " + authToken);
            return false;
        }
    }

    @Override
    public Token getNewToken(String userId, String refreshToken) {
        final User user = userProvider.get(userId);
        if (user != null && user.getStatus() == UserStatus.ACTIVE) {
            Token authenticationToken = tokenService.getUserToken(userId);
            if (authenticationToken == null) {
                authenticationToken = tokenService.getFromRefreshToken(refreshToken);
            }
            if (authenticationToken != null) {
                final String refreshTokenOriginal = authenticationToken.getRefreshToken();
                if (refreshToken.equals(refreshTokenOriginal)) {
                    final Token token = tokenService.generateNewToken(refreshToken);
                    if (token != null) {
                        final Authentication authentication = tokenService.retrieveFromRefreshToken(refreshToken);
                        tokenService.removeUserTokens(userId);
                        tokenService.store(token, authentication);
                        return token;
                    }
                }
            }
        }
        return null;
    }

}
