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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ro.cs.tao.component.TaoComponent;
import ro.cs.tao.messaging.Messaging;
import ro.cs.tao.messaging.Topic;
import ro.cs.tao.persistence.AuditProvider;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.UserProvider;
import ro.cs.tao.security.SystemPrincipal;
import ro.cs.tao.security.UserPrincipal;
import ro.cs.tao.user.Group;
import ro.cs.tao.user.LogEvent;
import ro.cs.tao.user.Role;
import ro.cs.tao.user.User;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * BaseController is an abstract class extending ControllerBase and provides common functionality for managing
 * user sessions, access control, and auditing across various controller implementations.
 *
 * @author Cosmin Cara
 */
public abstract class BaseController extends ControllerBase {

    private static AuditProvider auditProvider;
    protected static UserProvider userProvider;
    private static final Map<String, String> userTokens = new HashMap<>();

    /**
     * Builds a {@link ServletUriComponentsBuilder} instance initialized with the current request URI.
     *
     * @return a {@link ServletUriComponentsBuilder} instance pointing to the current request URI
     */
    public static ServletUriComponentsBuilder currentURL() {
        return ServletUriComponentsBuilder.fromCurrentRequestUri();
    }

    /**
     * Retrieves the token associated with the specified user ID.
     *
     * @param userId the identifier of the user whose token is being requested
     * @return the token associated with the specified user ID, or null if no token is found
     */
    public static String tokenOf(String userId) {
        return userTokens.get(userId);
    }

    /**
     * Stores a token for the specified user ID.
     *
     * @param userId the identifier of the user for whom the token is being stored
     * @param token the token to be associated with the specified user ID
     */
    public static void storeToken(String userId, String token) {
        userTokens.put(userId, token);
    }

    /**
     * Removes the token associated with the specified user ID from the user token storage.
     *
     * @param userId the identifier of the user whose token is to be cleared
     */
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

    /**
     * Retrieves the username of the currently authenticated user.
     *
     * @return the username of the currently authenticated user
     */
    protected String currentUser() {
        return currentPrincipal().getName();
    }

    protected String currentOwner() {
        Principal principal = currentPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getMasterId() != null
                    ? userPrincipal.getMasterId()
                    : userPrincipal.getName();
        } else {
            return principal.getName();
        }
    }

    /**
     * Retrieves the currently authenticated principal.
     * If the principal is not an instance of {@code Principal}, it is assumed to be a username,
     * and a {@code UserPrincipal} is created using the user's details retrieved via the {@code userProvider}.
     *
     * @return the authenticated principal, either as a {@code Principal} or as a {@code UserPrincipal}
     */
    protected Principal currentPrincipal() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof Principal)) {
            final User user = userProvider.get((String) principal);
            principal = new UserPrincipal(user.getId(),
                                          user.getWorkspaceId(),
                                          user.getGroups().stream().map(Group::getName).collect(Collectors.toSet()));
        }
        return (Principal) principal;

    }

    /**
     * Determines whether the currently authenticated user has administrative privileges.
     * The check is performed based on the type and roles of the principal returned by {@code currentPrincipal()}:
     * - If the principal is an instance of {@code SystemPrincipal}, it is considered an admin.
     * - If the principal is an instance of {@code UserPrincipal}, the method verifies if it has the "admin" role.
     *
     * @return {@code true} if the current user is an administrator, otherwise {@code false}.
     */
    protected boolean isCurrentUserAdmin() {
        final Principal principal = currentPrincipal();
        return principal instanceof SystemPrincipal || ((UserPrincipal) principal).hasRole(Role.ADMIN);
    }

    /**
     * Checks if the user with the specified user ID has administrator privileges.
     * The method determines this by retrieving the user's groups and checking if
     * any of the groups has the name "admin" (case-insensitive).
     *
     * @param userId the identifier of the user whose administrative status is to be checked
     * @return {@code true} if the user is an administrator, otherwise {@code false}
     */
    protected boolean isAdmin(String userId) {
        return userProvider.get(userId).getGroups().stream().anyMatch(g -> Role.ADMIN.equalsIgnoreCase(g.getName()));
    }

    protected <T extends TaoComponent> boolean ownsEntity(T entity) {
        try {
            final Method getOwner = entity.getClass().getMethod("getOwner");
            Object value = getOwner.invoke(entity);
            return currentUser().equals(value) || currentOwner().equals(value);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return false;
        }
    }

    /**
     * Records an audit log for a specific event. The event is saved with the current timestamp
     * and the username of the currently authenticated user.
     *
     * @param event the description of the event to be recorded in the audit log
     */
    protected void record(String event) {
        try {
            auditProvider.save(new LogEvent(LocalDateTime.now(), currentUser(), RequestContextHolder.currentRequestAttributes().getSessionId(), event));
        } catch (PersistenceException e) {
            Logger.getLogger(getClass().getName()).warning(e.getMessage());
        }
    }

    /**
     * Returns a standardized response entity indicating that the request is unauthorized.
     * The response includes a message set to "Unauthorized" and an HTTP status of 401 (Unauthorized).
     *
     * @return a {@link ResponseEntity} wrapping a {@link ServiceResponse} with the message "Unauthorized"
     *         and an HTTP status of {@code HttpStatus.UNAUTHORIZED}
     */
    protected ResponseEntity<ServiceResponse<?>> unauthorizedResponse() {
        return prepareResult("Unauthorized", HttpStatus.UNAUTHORIZED);
    }
}
