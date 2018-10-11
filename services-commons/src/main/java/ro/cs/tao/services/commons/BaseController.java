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
package ro.cs.tao.services.commons;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.user.User;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Cosmin Cara
 */
public class BaseController extends ControllerBase {
    private static Set<String> admins;
    private static Timer groupRefreshTimer;
    private static PersistenceManager persistenceManager;

    public static void setPersistenceManager(PersistenceManager persistenceManager) {
        BaseController.persistenceManager = persistenceManager;
        groupRefreshTimer = new Timer(true);
        admins = Collections.synchronizedSet(new HashSet<>());
        groupRefreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                admins = persistenceManager.getAdministrators()
                                            .stream().map(User::getUsername).collect(Collectors.toSet());
            }
        }, 0, 10000);
    }

    protected PersistenceManager getPersistenceManager() {
        return persistenceManager;
    }

    protected String currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    protected boolean isCurrentUserAdmin() {
        return admins.contains(SessionStore.currentContext().getPrincipal().getName());
    }
}
