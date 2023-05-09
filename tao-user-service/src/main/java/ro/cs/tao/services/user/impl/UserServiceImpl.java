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
package ro.cs.tao.services.user.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.UserProvider;
import ro.cs.tao.services.interfaces.UserService;
import ro.cs.tao.services.model.user.ResetPasswordInfo;
import ro.cs.tao.user.Group;
import ro.cs.tao.user.User;
import ro.cs.tao.user.UserPreference;

import java.util.List;

/**
 * @author Oana H.
 */
@Service("userService")
public class UserServiceImpl implements UserService {

    @Autowired
    private UserProvider userProvider;

    @Override
    public List<Group> getGroups() {
        return userProvider.listGroups();
    }

    @Override
    public void activateUser(String username) throws PersistenceException {
        userProvider.activate(username);
    }

    @Override
    public void resetPassword(String username, ResetPasswordInfo resetInfo) throws PersistenceException {
        userProvider.resetPassword(username, resetInfo.getResetKey(), resetInfo.getNewPassword());
    }

    @Override
    public User getUserInfo(String username) {
        return userProvider.getByName(username);
    }

    @Override
    public User updateUserInfo(User updatedInfo) throws PersistenceException {
        return userProvider.update(updatedInfo, false);
    }

    @Override
    public List<UserPreference> saveOrUpdateUserPreferences(String username, List<UserPreference> userPreferences) throws PersistenceException {
        return userProvider.save(username, userPreferences);
    }

    @Override
    public List<UserPreference> removeUserPreferences(String username, List<String> userPrefsKeysToDelete) throws PersistenceException {
        return userProvider.remove(username, userPrefsKeysToDelete);
    }
}
