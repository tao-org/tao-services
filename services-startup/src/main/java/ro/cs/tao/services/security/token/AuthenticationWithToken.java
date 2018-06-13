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

import org.springframework.security.authentication.jaas.JaasAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import javax.security.auth.login.LoginContext;
import java.util.List;

/**
 *
 * @author Oana H.
 */
public class AuthenticationWithToken extends JaasAuthenticationToken {

    public AuthenticationWithToken(Object principal, Object credentials, List<GrantedAuthority> authorities, LoginContext loginContext) {
        super(principal, credentials, authorities, loginContext);
    }

    public AuthenticationWithToken(Object principal, Object credentials, LoginContext loginContext) {
        super(principal, credentials, loginContext);
    }

    public void setToken(String token) {
        setDetails(token);
    }

    public String getToken() {
        return (String)getDetails();
    }
}
