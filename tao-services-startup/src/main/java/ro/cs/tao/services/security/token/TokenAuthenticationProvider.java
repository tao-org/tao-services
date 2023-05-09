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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.security.SystemPrincipal;
import ro.cs.tao.services.auth.token.TokenManagementService;

/**
 *
 * @author Oana H.
 */
@Component
//@Scope(value = "singleton")
public class TokenAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    private TokenManagementService tokenManagementService;
    private String apiDevToken;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (this.apiDevToken == null) {
            this.apiDevToken = ConfigurationManager.getInstance().getValue("api.token");
        }
        String token = (String) authentication.getPrincipal();
        if (token == null || token.length() == 0) {
            throw new BadCredentialsException("Invalid token");
        }
        if (token.equals(this.apiDevToken)) {
            return new JaasAuthenticationToken(SystemPrincipal.instance(), token, null, null);
        }
        if (!tokenManagementService.isValid(token)) {
            throw new BadCredentialsException("Invalid token or token expired");
        }
        return tokenManagementService.retrieve(token);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(PreAuthenticatedAuthenticationToken.class);
    }
}
