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
package ro.cs.tao.services.security.token;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.jaas.JaasAuthenticationToken;
import org.springframework.security.authentication.jaas.JaasGrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.persistence.UserProvider;
import ro.cs.tao.security.SystemPrincipal;
import ro.cs.tao.security.Token;
import ro.cs.tao.security.UserPrincipal;
import ro.cs.tao.services.auth.token.TokenManagementService;
import ro.cs.tao.services.security.TokenCacheManager;
import ro.cs.tao.user.User;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Oana H.
 */
@Component
//@Scope(value = "singleton")
public class TokenAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    private TokenManagementService tokenManagementService;
    @Autowired
    private UserProvider userProvider;

    private List<GrantedAuthority> apiAuthority;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String token = (String) authentication.getPrincipal();
        if (token == null || token.isEmpty()) {
            throw new BadCredentialsException("Invalid token");
        }
        if (token.equals(ConfigurationManager.getInstance().getValue("api.token"))) {
            if (this.apiAuthority == null) {
                this.apiAuthority = new ArrayList<>();
                apiAuthority.add(new JaasGrantedAuthority("ADMIN", SystemPrincipal.instance()));
            }
            return new JaasAuthenticationToken(SystemPrincipal.instance(), token, this.apiAuthority, null);
        }
        if (!tokenManagementService.isValid(token)) {
            throw new BadCredentialsException("Invalid token or token expired");
        } else {
            Authentication auth = tokenManagementService.retrieve(token);
            if (auth == null) {
                final Token fullToken = TokenCacheManager.getCache().getFullToken(token);
                final String userId = TokenCacheManager.getCache().getUser(token);
                final User user = userProvider.get(userId);
                final UserPrincipal principal = new UserPrincipal(userId);
                GrantedAuthority authority = new JaasGrantedAuthority(user.getGroups().get(0).getName(), principal);
                auth = new JaasAuthenticationToken(principal, token, new ArrayList<>() {{ add(authority); }}, null);
                TokenCacheManager.getCache().put(fullToken, auth);
            }
        }
        return tokenManagementService.retrieve(token);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(PreAuthenticatedAuthenticationToken.class);
    }
}
