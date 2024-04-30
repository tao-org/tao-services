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
import ro.cs.tao.security.SystemPrincipal;
import ro.cs.tao.security.UserPrincipal;
import ro.cs.tao.user.Group;
import ro.cs.tao.user.LogEvent;
import ro.cs.tao.user.User;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Cosmin Cara
 */
public abstract class BaseController extends ControllerBase {

    private static AuditProvider auditProvider;
    private static UserProvider userProvider;
    private static final Map<String, String> userTokens = new HashMap<>();

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
    public final void setUserProvider(UserProvider provider) {
        if (userProvider == null) {
            userProvider = provider;
        }
    }

    @Autowired
    public final void setAuditProvider(AuditProvider provider) {
        if (auditProvider == null) {
            auditProvider = provider;
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
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof Principal)) {
            final User user = userProvider.get((String) principal);
            principal = new UserPrincipal(user.getId(),
                                          user.getGroups().stream().map(Group::getName).collect(Collectors.toSet()));
        }
        return (Principal) principal;

    }

    protected boolean isCurrentUserAdmin() {
        final Principal principal = currentPrincipal();
        return principal instanceof SystemPrincipal || ((UserPrincipal) principal).hasRole("admin");
    }

    protected boolean isAdmin(String userId) {
        return userProvider.get(userId).getGroups().stream().anyMatch(g -> "admin".equalsIgnoreCase(g.getName()));
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
