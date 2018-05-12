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

import org.springframework.security.core.context.SecurityContextHolder;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.security.ExternalSessionContextProvider;
import ro.cs.tao.security.SessionContext;
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
            protected Principal setPrincipal() {
                return new UserPrincipal(((SecurityUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                                                 .getUsername());
            }

            @Override
            protected List<UserPreference> setPreferences() {
                try {
                    return persistenceManager != null ? persistenceManager.getUserPreferences(getPrincipal().getName()) :
                            null;
                } catch (PersistenceException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
    }
}
