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


public class CustomAuthenticationProvider implements AuthenticationProvider {

    private AuthenticationProvider delegate;

    public CustomAuthenticationProvider(AuthenticationProvider delegate) {
        this.delegate = delegate;
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
