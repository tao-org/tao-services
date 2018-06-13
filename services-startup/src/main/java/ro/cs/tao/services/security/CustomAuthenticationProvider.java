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
package ro.cs.tao.services.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.jaas.JaasAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import ro.cs.tao.services.auth.token.TokenManagementService;
import ro.cs.tao.services.security.token.AuthenticationWithToken;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Custom JAAS authentication provider
 *
 * @author  Oana H.
 */

public class CustomAuthenticationProvider implements AuthenticationProvider {

    private static final Logger logger = Logger.getLogger(CustomAuthenticationProvider.class.getName());

    private AuthenticationProvider delegate;

    private TokenManagementService tokenService;

    public CustomAuthenticationProvider(AuthenticationProvider delegate, TokenManagementService tokenMngService) {
        this.delegate = delegate;
        this.tokenService = tokenMngService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        JaasAuthenticationToken jaasAuthenticationToken = (JaasAuthenticationToken) delegate
          .authenticate(authentication);

        if (jaasAuthenticationToken.isAuthenticated()) {
            String userName = jaasAuthenticationToken.getPrincipal().toString();
            jaasAuthenticationToken.getLoginContext().getSubject().getPrincipals().add(new UserPrincipal(userName));
            // TODO: why user group is needed here?
            //jaasAuthenticationToken.getLoginContext().getSubject().getPrincipals().add(new GroupPrincipal("admin"));

            // generate and store a new authentication token
            AuthenticationWithToken authenticationWithToken = new AuthenticationWithToken(jaasAuthenticationToken.getPrincipal(), jaasAuthenticationToken.getCredentials(), jaasAuthenticationToken.getAuthorities().stream().collect(Collectors.toList()), jaasAuthenticationToken.getLoginContext());
            final String newToken = tokenService.generateNewToken();
            authenticationWithToken.setToken(newToken);
            tokenService.store(newToken, authenticationWithToken);

            logger.info("Stored token: " + newToken);

            return jaasAuthenticationToken;
        } else {
            return null;
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return delegate.supports(authentication);
    }
}
