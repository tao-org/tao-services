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
package ro.cs.tao.services.security.token;

import org.apache.commons.codec.binary.Base64;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.UrlPathHelper;
import ro.cs.tao.messaging.Message;
import ro.cs.tao.messaging.Notifiable;
import ro.cs.tao.messaging.Topic;
import ro.cs.tao.messaging.system.StartupCompletedMessage;
import ro.cs.tao.security.SystemPrincipal;
import ro.cs.tao.services.commons.Endpoints;
import ro.cs.tao.utils.ExceptionUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Oana H.
 */
public class AuthenticationFilter extends GenericFilterBean {
    private static final Logger logger = Logger.getLogger(AuthenticationFilter.class.getName());
    private static volatile boolean systemInitialised = false;
    private final AuthenticationManager authenticationManager;
    private final SystemMessageReceiver receiver;

    public AuthenticationFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
        this.receiver = new SystemMessageReceiver();
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        final HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        final HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
        if (!systemInitialised && !httpResponse.isCommitted()) {
            httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "The system is not able yet to serve http requests");
        }
        final Optional<String> token = Optional.ofNullable(httpRequest.getHeader("X-Auth-Token"));
        final String resourcePath = new UrlPathHelper().getPathWithinApplication(httpRequest);

        try {
            ServletRequest request = servletRequest;
            if (postToAuthenticate(httpRequest, resourcePath)) {
                final String username = httpRequest.getParameter("username");
                final String password = httpRequest.getParameter("password");
                MutableHttpServletRequest wrapper = new MutableHttpServletRequest(httpRequest);
                wrapper.putHeader("Authorization", "Basic " + new String(Base64.encodeBase64((username + ":" + password).getBytes(StandardCharsets.US_ASCII))));
                request = wrapper;
            } else if (token.isPresent()) {
                processTokenAuthentication(token.get(), request);
            } else {
                if (isExcludedResource(httpRequest, resourcePath)) {
                    SecurityContextHolder.getContext().setAuthentication(new SystemAuthentication());
                } else {
                    if (requiresToken(resourcePath)) {
                        throw new AuthenticationCredentialsNotFoundException(resourcePath);
                    }
                }
            }
            filterChain.doFilter(request, servletResponse);
        } catch (InternalAuthenticationServiceException internalAuthenticationServiceException) {
            SecurityContextHolder.clearContext();
            logger.log(Level.SEVERE, "Internal authentication service exception", internalAuthenticationServiceException);
            if (!httpResponse.isCommitted()) {
                httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (AuthenticationException authenticationException) {
            SecurityContextHolder.clearContext();
            if (!httpResponse.isCommitted()) {
                httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, authenticationException.getMessage());
            }
        } catch (Throwable t) {
            ExceptionUtils.getStackTrace(logger, t);
        }

    }

    private void processTokenAuthentication(String token, ServletRequest request) {
        final PreAuthenticatedAuthenticationToken requestAuthentication = new PreAuthenticatedAuthenticationToken(token, null);
        final Authentication responseAuthentication = authenticationManager.authenticate(requestAuthentication);
        if (responseAuthentication == null || !responseAuthentication.isAuthenticated()) {
            logger.warning(String.format("Authentication request [ip: %s, token: %s] failed",
                                         request.getRemoteAddr(), token));
            throw new InternalAuthenticationServiceException("Unable to authenticate user");
        }
        SecurityContextHolder.getContext().setAuthentication(responseAuthentication);
    }

    private boolean postToAuthenticate(HttpServletRequest httpRequest, String resourcePath) {
        return Endpoints.LOGIN_ENDPOINT.equalsIgnoreCase(resourcePath)
                && httpRequest.getMethod().equals(Endpoints.LOGIN_ENDPOINT_METHOD);
    }

    private boolean requiresToken(String resourcePath) {
        return Endpoints.endpoints("ro.cs.tao").stream().anyMatch(e -> e.indexOf('*') > 0
                ? resourcePath.startsWith(e.substring(0, e.indexOf('*')))
                : resourcePath.startsWith(e));
    }

    private boolean isExcludedResource(HttpServletRequest httpRequest, String resourcePath){
        return  (Endpoints.REFRESH_ENDPOINT.equalsIgnoreCase(resourcePath)
                    && httpRequest.getMethod().equals(Endpoints.LOGIN_ENDPOINT_METHOD)) ||
                (resourcePath.startsWith(Endpoints.DOWNLOAD_ENDPOINT)
                    && httpRequest.getMethod().equals("GET"));
    }

    private static class SystemMessageReceiver extends Notifiable {

        SystemMessageReceiver() {
            subscribe(Topic.SYSTEM.value());
        }

        @Override
        protected void onMessageReceived(Message message) {
            if (message instanceof StartupCompletedMessage) {
                systemInitialised = true;
            }
        }
    }

    private static class SystemAuthentication implements Authentication {

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return null;
        }

        @Override
        public Object getCredentials() {
            return SystemPrincipal.instance();
        }

        @Override
        public Object getDetails() {
            return SystemPrincipal.instance();
        }

        @Override
        public Object getPrincipal() {
            return SystemPrincipal.instance();
        }

        @Override
        public boolean isAuthenticated() {
            return true;
        }

        @Override
        public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {

        }

        @Override
        public String getName() {
            return SystemPrincipal.instance().getName();
        }
    }
}
