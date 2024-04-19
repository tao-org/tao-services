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
import ro.cs.tao.execution.persistence.ExecutionTaskProvider;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.UserProvider;
import ro.cs.tao.services.commons.WorkspaceCreator;
import ro.cs.tao.services.interfaces.UserService;
import ro.cs.tao.services.model.user.ResetPasswordInfo;
import ro.cs.tao.user.Group;
import ro.cs.tao.user.User;
import ro.cs.tao.user.UserPreference;
import ro.cs.tao.utils.StringUtilities;

import java.util.List;

/**
 * @author Oana H.
 */
@Service("userService")
public class UserServiceImpl implements UserService {

    @Autowired
    private UserProvider userProvider;

    @Autowired
    private ExecutionTaskProvider taskProvider;

    @Override
    public List<Group> getGroups() {
        return userProvider.listGroups();
    }

    @Override
    public void activateUser(String userId) throws PersistenceException {
        userProvider.activate(userId);
    }

    @Override
    public void createWorkspaces(String userId) {
        final User user = userProvider.get(userId);
        if (user != null) {
            userProvider.createWorkspaces(user, new WorkspaceCreator());
        } else {
            userProvider.createWorkspaces(userId, new WorkspaceCreator());
        }
    }

    @Override
    public void resetPassword(String userId, ResetPasswordInfo resetInfo) throws PersistenceException {
        userProvider.resetPassword(userId, resetInfo.getResetKey(), resetInfo.getNewPassword());
    }

    @Override
    public User getUserInfo(String userId) {
        User user = null;
        if (!StringUtilities.isNullOrEmpty(userId)) {
            user = userProvider.get(userId);
            if (user == null) {
                user = userProvider.getByName(userId);
            }
            user.setActualProcessingQuota(taskProvider.getCPUsForUser(user.getId()));
        }
        return user;
    }

    @Override
    public User updateUserInfo(User updatedInfo) throws PersistenceException {
        return userProvider.update(updatedInfo, false);
    }

    @Override
    public List<UserPreference> saveOrUpdateUserPreferences(String userId, List<UserPreference> userPreferences) throws PersistenceException {
        return userProvider.save(userId, userPreferences);
    }

    @Override
    public List<UserPreference> removeUserPreferences(String userId, List<String> userPrefsKeysToDelete) throws PersistenceException {
        return userProvider.remove(userId, userPrefsKeysToDelete);
    }
}
