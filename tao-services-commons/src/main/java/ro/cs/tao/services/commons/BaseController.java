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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.interfaces.AdministrationService;
import ro.cs.tao.user.User;

import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

/**
 * @author Cosmin Cara
 */
public abstract class BaseController extends ControllerBase {
    private static final Set<String> admins;
    private static final Timer groupRefreshTimer;
    private static AdministrationService administrationService;

    static {
        groupRefreshTimer = new Timer(true);
        admins = new HashSet<>();
        groupRefreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (administrationService != null) {
                    admins.clear();
                    admins.addAll(administrationService.getAdministrators().stream().map(User::getUsername).collect(Collectors.toSet()));
                }
            }
        }, 10000, 10000);
    }

    public static ServletUriComponentsBuilder currentURL() {
        return ServletUriComponentsBuilder.fromCurrentRequestUri();
    }

    @Autowired
    public final void setAdministrationService(AdministrationService service) {
        if (administrationService == null) {
            synchronized (admins) {
                administrationService = service;
            }
        }
    }

    protected String currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    protected boolean isCurrentUserAdmin() {
        return admins.contains(SessionStore.currentContext().getPrincipal().getName());
    }
}
