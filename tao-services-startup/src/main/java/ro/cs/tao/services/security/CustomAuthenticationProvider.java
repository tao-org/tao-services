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
import ro.cs.tao.security.*;
import ro.cs.tao.services.auth.token.TokenManagementService;
import ro.cs.tao.services.security.token.AuthenticationWithToken;
import ro.cs.tao.user.Group;
import ro.cs.tao.user.User;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Custom JAAS authentication provider
 *
 * @author  Oana H.
 */
@Service("customAuthenticationProvider")
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private static final Logger logger = Logger.getLogger(CustomAuthenticationProvider.class.getName());

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
            this.tokenService.setTokenProvider(new KeycloakClient());
        }
        this.localTokenProvider = new TokenProvider() { };
    }

    public static void setPersistenceManager(PersistenceManager manager) { persistenceManager = manager; }

    @Override
    public Authentication authenticate(Authentication authentication) {
        JaasAuthenticationToken jaasAuthenticationToken = (JaasAuthenticationToken) delegate.authenticate(authentication);

        if (jaasAuthenticationToken.isAuthenticated()) {
            final String userName = jaasAuthenticationToken.getPrincipal().toString();
            jaasAuthenticationToken.getLoginContext().getSubject().getPrincipals().add(new UserPrincipal(userName));
            // add user groups as roles
            final User user = persistenceManager.users().getByName(userName);
            if (user == null) {
                throw new RuntimeException(String.format("No such user '%s'", userName));
            }
            final List<Group> userGroups = user.getGroups();
            if (userGroups != null) {
                for (Group group : userGroups) {
                    jaasAuthenticationToken.getLoginContext().getSubject().getPrincipals().add(new GroupPrincipal(group.getName()));
                }
            }

            // generate and store a new authentication token
            final AuthenticationWithToken authenticationWithToken = new AuthenticationWithToken(jaasAuthenticationToken.getPrincipal(),
                                                                                                jaasAuthenticationToken.getCredentials(),
                                                                                                new ArrayList<>(jaasAuthenticationToken.getAuthorities()),
                                                                                                jaasAuthenticationToken.getLoginContext());
            Token userToken = tokenService.getUserToken(userName);
            if (userToken == null) {
                logger.finest(String.format("No previous token for user %s, generating a new one", userName));
                userToken = !"admin".equals(userName)
                            ? tokenService.generateNewToken(userName, user.getPassword())
                            : localTokenProvider.newToken(userName, user.getPassword());
                tokenService.store(userToken, authenticationWithToken);
                logger.finest(String.format("Stored token %s for user %s", userToken.getToken(), userName));
            } else if (tokenService.retrieve(userToken.getToken()) == null) {
                logger.finest(String.format("No previous authentication for user %s, storing the created one", userName));
                tokenService.store(new Token(userToken.getToken(), userToken.getRefreshToken(), TokenProvider.defaultExpiration()),
                                   authenticationWithToken);
                logger.finest(String.format("Stored token %s for user %s", userToken.getToken(), userName));
            }
            authenticationWithToken.setToken(userToken.getToken());
            return jaasAuthenticationToken;
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
