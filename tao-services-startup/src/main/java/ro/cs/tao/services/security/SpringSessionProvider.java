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

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.security.ExternalSessionContextProvider;
import ro.cs.tao.security.SessionContext;
import ro.cs.tao.security.SystemPrincipal;
import ro.cs.tao.security.UserPrincipal;
import ro.cs.tao.user.User;
import ro.cs.tao.user.UserPreference;

import java.security.Principal;
import java.util.List;

public class SpringSessionProvider implements ExternalSessionContextProvider {
    public static PersistenceManager persistenceManager;

    public static void setPersistenceManager(PersistenceManager manager) { persistenceManager = manager; }

    @Override
    public SessionContext currentContext() {
        return new SessionContext() {

            @Override
            public Principal setPrincipal(Principal value) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                Object principal = auth != null ? auth.getPrincipal() : SystemPrincipal.instance();
                if (principal instanceof String) {
                    final User user = persistenceManager.users().getByName(principal.toString());
                    if (user != null) {
                        principal =  new UserPrincipal(user.getId());
                    }
                }
                return principal instanceof UserPrincipal
                       ? (UserPrincipal) principal
                       : new UserPrincipal(principal instanceof Principal ? ((Principal) principal).getName() : principal.toString());

            }

            @Override
            protected List<UserPreference> setPreferences() {
                return persistenceManager != null ? persistenceManager.users().listPreferences(getPrincipal().getName()) :
                        null;
            }

            @Override
            public int hashCode() {
                return getPrincipal() != null ? getPrincipal().getName().hashCode() : super.hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                if (!(obj instanceof SessionContext)) {
                    return false;
                }
                SessionContext other = (SessionContext) obj;
                return (this.getPrincipal() == null && other.getPrincipal() == null) ||
                        (this.getPrincipal().getName().equals(other.getPrincipal().getName()));
            }
        };
    }
}
