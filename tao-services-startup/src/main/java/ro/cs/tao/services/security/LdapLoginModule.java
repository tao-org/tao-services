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

import ro.cs.tao.ldap.TaoLdapClient;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.security.AuthenticationMode;
import ro.cs.tao.security.TaoLoginModule;
import ro.cs.tao.user.Group;
import ro.cs.tao.user.User;

import java.util.List;
import java.util.stream.Collectors;

public class LdapLoginModule extends TaoLoginModule {

    private final TaoLdapClient ldapClient;

    public LdapLoginModule() {
        super();
        ldapClient = new TaoLdapClient();
    }

    @Override
    protected AuthenticationMode intendedFor() {
        return AuthenticationMode.LDAP;
    }

    @Override
    protected boolean shouldEncryptPassword() { return false; }

    @Override
    protected User loginImpl(String user, String password) {
        if (userProvider == null) {
            throw new RuntimeException("[userProvider] not set");
        }
        User userEntity = ldapClient.checkLoginCredentials(username, password);
        if (userEntity != null) {
            final User existing = userProvider.getByName(user);
            if (existing == null) {
                try {
                    final List<Group> groups = userProvider.listGroups().stream().filter(g -> "USER".equalsIgnoreCase(g.getName())).collect(Collectors.toList());
                    userEntity.setGroups(groups);
                    userProvider.save(userEntity);
                } catch (PersistenceException e) {
                    logger.severe(String.format("Cannot persist user '%s'. Reason: %s", user, e.getMessage()));
                }
            } else {
                userEntity = existing;
            }
        }
        return userEntity;
    }
}
