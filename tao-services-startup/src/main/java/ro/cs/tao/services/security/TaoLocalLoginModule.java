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

import ro.cs.tao.security.AuthenticationMode;
import ro.cs.tao.security.TaoLoginModule;
import ro.cs.tao.user.User;

public class TaoLocalLoginModule extends TaoLoginModule {

    public TaoLocalLoginModule() {
        super();
    }

    @Override
    protected AuthenticationMode intendedFor() {
        return AuthenticationMode.LOCAL;
    }

    @Override
    protected boolean shouldEncryptPassword() { return true; }

    @Override
    protected User loginImpl(String user, String password) {
        if (userProvider == null) {
            throw new RuntimeException("[userProvider] not set");
        }
        final User userEntity;
        if (userProvider.login(user, password)) {
            userEntity = userProvider.getByName(user);
        } else {
            userEntity = null;
        }
        return userEntity;
    }

}
