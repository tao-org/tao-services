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
package ro.cs.tao.services.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.jaas.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.keycloak.KeycloakClient;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.UserProvider;
import ro.cs.tao.security.*;
import ro.cs.tao.services.auth.token.TokenManagementService;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.security.token.AuthenticationWithToken;
import ro.cs.tao.user.Group;
import ro.cs.tao.user.User;
import ro.cs.tao.user.UserType;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Custom JAAS authentication provider
 *
 * @author  Oana H.
 */
@Service("customAuthenticationProvider")
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private static final Logger logger = Logger.getLogger(CustomAuthenticationProvider.class.getName());
    private static String adminId;

    private final AuthenticationProvider delegate;

    @Autowired
    private final TokenManagementService tokenService;

    private final TokenProvider localTokenProvider;

    public static PersistenceManager persistenceManager;

    public CustomAuthenticationProvider(TokenManagementService tokenMngService) {
        this.delegate = jaasAuthProvider();
        this.tokenService = tokenMngService;
        this.tokenService.setTokenCache(TokenCacheManager.getCache());
        if (AuthenticationMode.KEYCLOAK.name().equalsIgnoreCase(ConfigurationManager.getInstance().getValue("authentication.mode"))) {
            // Keycloak settings are present
            final KeycloakClient client = new KeycloakClient();
            client.setTokenKeeper(TokenCacheManager.getCache());
            client.setUserProvider(new Supplier<UserProvider>() {
                @Override
                public UserProvider get() {
                    return persistenceManager.users();
                }
            });
            this.tokenService.setTokenProvider(client);
        }
        this.localTokenProvider = new TokenProvider() { };
    }

    public static void setPersistenceManager(PersistenceManager manager) { persistenceManager = manager; }

    UserProvider getUserProvider() {
        return persistenceManager.users();
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        if (adminId == null) {
            adminId = persistenceManager.users().getByName("admin").getId();
        }
        JaasAuthenticationToken jaasAuthenticationToken = (JaasAuthenticationToken) delegate.authenticate(authentication);

        if (jaasAuthenticationToken.isAuthenticated()) {
            final Principal principal = jaasAuthenticationToken.getLoginContext().getSubject().getPrincipals().stream().findFirst().get();
            final String userId = principal.getName();
            final User user = persistenceManager.users().get(userId);
            if (user == null) {
                throw new RuntimeException(String.format("No such user '%s'", userId));
            }
            final List<Group> userGroups = user.getGroups();
            final UserPrincipal userPrincipal = new UserPrincipal(user.getId(),
                                                                  userGroups.stream().map(Group::getName).collect(Collectors.toSet()));
            final Set<Principal> principals = jaasAuthenticationToken.getLoginContext().getSubject().getPrincipals();
            if (principals.stream().noneMatch(p -> p.getName().equals(userPrincipal.getName()))) {
                principals.add(userPrincipal);
            }
            // add user groups as roles
            if (userGroups != null) {
                for (Group group : userGroups) {
                    principals.add(new GroupPrincipal(group.getName()));
                }
            }

            // generate and store a new authentication token
            final AuthenticationWithToken authenticationWithToken = new AuthenticationWithToken(//jaasAuthenticationToken.getPrincipal(),
                                                                                                userPrincipal,
                                                                                                jaasAuthenticationToken.getCredentials(),
                                                                                                new ArrayList<>(jaasAuthenticationToken.getAuthorities()),
                                                                                                jaasAuthenticationToken.getLoginContext());
            Token userToken = tokenService.getUserToken(user.getId());
            if (userToken == null) {
                logger.finest(String.format("No previous token for user %s, generating a new one", userId));
                userToken = !adminId.equals(userId)
                            ? tokenService.generateNewToken(!UserType.KEYCLOAK.equals(user.getUserType())
                                                            ? user.getId()
                                                            : user.getUsername(),
                                                            user.getPassword())
                            : localTokenProvider.newToken(user.getId(), user.getPassword());
                tokenService.store(userToken, authenticationWithToken);
                logger.finest(String.format("Stored token %s for user %s", userToken.getToken(), userId));
            } else if (tokenService.retrieve(userToken.getToken()) == null) {
                logger.finest(String.format("No previous authentication for user %s, storing the created one", userId));
                tokenService.store(new Token(userToken.getToken(), userToken.getRefreshToken(), TokenProvider.defaultExpiration()),
                                   authenticationWithToken);
                logger.finest(String.format("Stored token %s for user %s", userToken.getToken(), userId));
            }
            tokenService.removeUserTokens(user.getUsername());
            authenticationWithToken.setToken(userToken.getToken());
            BaseController.storeToken(userId, userToken.getToken());
            return authenticationWithToken;
        } else {
            return null;
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return delegate.supports(authentication);
    }

    @Bean
    public JaasAuthenticationProvider jaasAuthProvider() {
        JaasAuthenticationProvider authenticationProvider = new JaasAuthenticationProvider();
        authenticationProvider.setAuthorityGranters(new AuthorityGranter[] { new TaoAuthorityGranter() });
        authenticationProvider.setCallbackHandlers(new JaasAuthenticationCallbackHandler[] {
                new JaasNameCallbackHandler(), new JaasPasswordCallbackHandler()
        });
        authenticationProvider.setLoginContextName("taologin");
        authenticationProvider.setLoginConfig(new ClassPathResource("tao_jaas.config"));
        return authenticationProvider;
    }
}
