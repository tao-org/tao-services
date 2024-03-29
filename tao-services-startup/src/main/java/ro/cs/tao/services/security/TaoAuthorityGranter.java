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

import org.springframework.security.authentication.jaas.AuthorityGranter;
import ro.cs.tao.security.UserPrincipal;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

public class TaoAuthorityGranter implements AuthorityGranter {

    @Override
    public Set<String> grant(Principal principal) {

        /*final List<SimpleGrantedAuthority> authorities = (List<SimpleGrantedAuthority>)((SecurityUser) principal).getAuthorities();
        return authorities.stream().map(auth -> auth.toString()).collect(Collectors.toSet());*/

        Set<String> roles = new HashSet<String>();
        if (principal instanceof UserPrincipal) {
            roles.add(principal.getName());
        } else {
            String user = principal.getName();
            if (user.indexOf('@') >= 0) {
                user = user.substring(0, user.indexOf('@'));
            }
            roles.add(user);
        }
        return roles;
    }
}
