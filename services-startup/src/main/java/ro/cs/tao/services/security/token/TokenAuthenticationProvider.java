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
package ro.cs.tao.services.security.token;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import com.google.common.base.Optional;
import org.springframework.stereotype.Component;
import ro.cs.tao.services.auth.token.TokenManagementService;

/**
 *
 * @author Oana H.
 */
@Component
@Scope(value = "singleton")
public class TokenAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    private TokenManagementService tokenManagementService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Optional token = (Optional) authentication.getPrincipal();
        if (!token.isPresent() || token.get().toString().isEmpty()) {
            throw new BadCredentialsException("Invalid token");
        }
        if (!tokenManagementService.contains(token.get().toString())) {
            throw new BadCredentialsException("Invalid token or token expired");
        }
        return tokenManagementService.retrieve(token.get().toString());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(PreAuthenticatedAuthenticationToken.class);
    }
}
