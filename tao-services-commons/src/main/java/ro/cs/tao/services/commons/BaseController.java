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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ro.cs.tao.messaging.Messaging;
import ro.cs.tao.messaging.Topic;
import ro.cs.tao.persistence.AuditProvider;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.UserProvider;
import ro.cs.tao.security.UserPrincipal;
import ro.cs.tao.services.interfaces.AdministrationService;
import ro.cs.tao.user.LogEvent;
import ro.cs.tao.user.User;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Cosmin Cara
 */
public abstract class BaseController extends ControllerBase {
    private static final Set<String> admins;
    private static final Timer groupRefreshTimer;
    private static AdministrationService administrationService;
    private static AuditProvider auditProvider;
    private static UserProvider userProvider;
    private static final Map<String, String> userTokens = new HashMap<>();

    static {
        groupRefreshTimer = new Timer(true);
        admins = new HashSet<>();
        groupRefreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (administrationService != null) {
                    admins.clear();
                    admins.addAll(administrationService.getAdministrators().stream().map(User::getId).collect(Collectors.toSet()));
                }
            }
        }, 0, 10000);
    }

    public static ServletUriComponentsBuilder currentURL() {
        return ServletUriComponentsBuilder.fromCurrentRequestUri();
    }

    public static String tokenOf(String userId) {
        return userTokens.get(userId);
    }

    public static void storeToken(String userId, String token) {
        userTokens.put(userId, token);
    }

    public static void clearToken(String userId) {
        userTokens.remove(userId);
    }

    @Autowired
    public final void setAdministrationService(AdministrationService service) {
        if (administrationService == null) {
            synchronized (admins) {
                administrationService = service;
            }
        }
    }

    @Autowired
    public final void setUserProvider(UserProvider provider) {
        if (userProvider == null) {
            synchronized (admins) {
                userProvider = provider;
            }
        }
    }

    @Autowired
    public final void setAuditProvider(AuditProvider provider) {
        if (auditProvider == null) {
            synchronized (admins) {
                auditProvider = provider;
            }
        }
    }

    @ExceptionHandler(Exception.class)
    public void handleUncaughtException(Exception exception, WebRequest request) {
        error(exception.getMessage());
       final StringWriter writer = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(writer);
        exception.printStackTrace(printWriter);
        Messaging.send(request.getUserPrincipal(), Topic.ERROR.getCategory(),
                       request.getContextPath(), exception.getMessage(), writer.toString());
    }

    protected String currentUser() {
        return currentPrincipal().getName();
    }

    protected Principal currentPrincipal() {
        final Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal instanceof Principal
               ? (Principal) principal
               : new UserPrincipal(userProvider.getId((String) principal));

    }

    protected boolean isCurrentUserAdmin() {
        return admins.contains(currentUser());
    }

    protected boolean isAdmin(String userId) {
        return admins.contains(userId);
    }

    protected Set<String> getAdministrators() {
        return Collections.unmodifiableSet(admins);
    }

    protected void record(String event) {
        try {
            auditProvider.save(new LogEvent(LocalDateTime.now(), currentUser(), event));
        } catch (PersistenceException e) {
            Logger.getLogger(getClass().getName()).warning(e.getMessage());
        }
    }

    protected ResponseEntity<ServiceResponse<?>> unauthorizedResponse() {
        return prepareResult("Unauthorized", HttpStatus.UNAUTHORIZED);
    }
}
