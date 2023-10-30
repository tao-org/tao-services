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
package ro.cs.tao.services.admin.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.UserProvider;
import ro.cs.tao.services.interfaces.AdministrationService;
import ro.cs.tao.services.model.user.DisableUserInfo;
import ro.cs.tao.services.model.user.UserMapping;
import ro.cs.tao.services.model.user.UserUnicityInfo;
import ro.cs.tao.user.Group;
import ro.cs.tao.user.User;
import ro.cs.tao.user.UserStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Oana H.
 */
@Service("adminService")
public class AdministrationServiceImpl implements AdministrationService {

    @Autowired
    private UserProvider userProvider;


    @Override
    public User addNewUser(User newUserInfo) throws PersistenceException {
        return userProvider.save(newUserInfo);
    }

    @Override
    public List<UserUnicityInfo> getAllUsersUnicityInfo() {
        final Map<String, String[]> unicityInfo = userProvider.listUnicityInfo();
        final List<UserUnicityInfo> results = new ArrayList<>();
        for (String username: unicityInfo.keySet()) {
            results.add(new UserUnicityInfo(username, unicityInfo.get(username)[0], unicityInfo.get(username)[1]));
        }
        return results;
    }

    @Override
    public List<UserMapping> getAllUserNames() {
        final Map<String, String> mappings = userProvider.listNames();
        return mappings.entrySet().stream().map(e -> new UserMapping(e.getKey(), e.getValue())).collect(Collectors.toList());
    }

    @Override
    public List<User> findUsersByStatus(UserStatus userStatus) {
        return userProvider.list(userStatus);
    }

    @Override
    public List<Group> getGroups() {
        return userProvider.listGroups();
    }

    @Override
    public List<User> getAdministrators() {
        return userProvider.listAdministrators();
    }

    @Override
    public List<User> getUsers(Set<String> userIds) {
        return userProvider.listUsers(userIds);
    }

    @Override
    public User getUserInfo(String userId) {
        return userProvider.get(userId);
    }

    @Override
    public User updateUserInfo(User updatedInfo) throws PersistenceException {
        return userProvider.update(updatedInfo, true);
    }

    @Override
    public void disableUser(String userId, DisableUserInfo additionalDisableActions) throws PersistenceException {
        userProvider.disable(userId);
        // TODO delete private resources or other additional actions
    }

    @Override
    public void deleteUser(String userId) throws PersistenceException {
        userProvider.delete(userId);
    }
}
