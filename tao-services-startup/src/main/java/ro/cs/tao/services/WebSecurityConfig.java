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
package ro.cs.tao.services;

import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.firewall.HttpStatusRequestRejectedHandler;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.session.SessionInformationExpiredStrategy;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.GenericFilterBean;
import ro.cs.tao.EnumUtils;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.security.AuthenticationMode;
import ro.cs.tao.services.commons.Endpoints;
import ro.cs.tao.services.commons.TunnelFilter;
import ro.cs.tao.services.security.CustomAuthenticationProvider;
import ro.cs.tao.services.security.token.AuthenticationFilter;
import ro.cs.tao.services.security.token.TokenAuthenticationProvider;
import ro.cs.tao.spi.ServiceRegistry;
import ro.cs.tao.spi.ServiceRegistryManager;
import ro.cs.tao.utils.Tuple;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    private final int maxRejectedRequests = Integer.parseInt(ConfigurationManager.getInstance()
            .getValue("tao.max.rejected.requests", "10"));
    private final boolean enableBlockRequests = ConfigurationManager.getInstance().getBooleanValue("enable.block.bad.requests");
    private final int blockInterval = Integer.parseInt(ConfigurationManager.getInstance().getValue("tao.blocked.ip.interval", "10"));
    private Map<String, Tuple<Integer, LocalDateTime>> rejectedRequestCounter;

    private final Logger logger = Logger.getLogger(WebSecurityConfig.class.getName());
    private Timer timer;

    @Autowired
    private AuthenticationEntryPoint entryPoint;
    @Autowired
    private TokenAuthenticationProvider tokenProvider;
    @Autowired
    private CustomAuthenticationProvider customAuthenticationProvider;

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder auth = http.getSharedObject(AuthenticationManagerBuilder.class);
        auth.authenticationProvider(customAuthenticationProvider)
            .authenticationProvider(tokenProvider);
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
        return auth.build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {
        HttpSecurity httpSecurity = http
                .sessionManagement(sm -> sm
                        .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
                        .maximumSessions(2)
                        .maxSessionsPreventsLogin(false)
                        .expiredSessionStrategy(expiredSessionStrategy())
                        .sessionRegistry(sessionRegistry()))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(Endpoints.REFRESH_ENDPOINT, Endpoints.DOWNLOAD_ENDPOINT).permitAll()
                        .requestMatchers(Endpoints.LOGIN_ENDPOINT).authenticated()
                        .requestMatchers(apiEndpointsWithAuthToken()).authenticated()
                        .requestMatchers(Endpoints.GLOBAL_PATH_EXPRESSION).permitAll()
                        .anyRequest().permitAll())
                .httpBasic(Customizer.withDefaults())
                .authenticationProvider(customAuthenticationProvider)
                .authenticationProvider(tokenProvider)
                .addFilterBefore(new AuthenticationFilter(authenticationManager), BasicAuthenticationFilter.class)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint))
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults());

        if (enableBlockRequests) {
            httpSecurity.addFilterBefore(blockFilter(), BasicAuthenticationFilter.class);
        }

        if (tunnelFiltersEnabled()) {
            ServiceRegistry<TunnelFilter> registry = ServiceRegistryManager.getInstance().getServiceRegistry(TunnelFilter.class);
            Set<TunnelFilter> tunnelFilters = registry.getServices();
            if (tunnelFilters != null && !tunnelFilters.isEmpty()) {
                Class<? extends Filter> afterClass = AuthenticationFilter.class;
                for (TunnelFilter filter : tunnelFilters) {
                    http.addFilterAfter(filter, afterClass);
                    logger.fine("Registered tunnel filter: " + filter.getClass());
                    afterClass = filter.getClass();
                }
            }
        }

        return httpSecurity.build();
    }

    private boolean tunnelFiltersEnabled() {
        return ConfigurationManager.getInstance().getBooleanValue("enable.tunnel.filters") || componentEditorUsesTunnel();
    }

    private boolean componentEditorUsesTunnel() {
        final String componentEditor = ConfigurationManager.getInstance().getValue("component.editor", "");
        return "jupyter".equalsIgnoreCase(componentEditor) || "zeppelin".equalsIgnoreCase(componentEditor);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOriginPattern("*");
        configuration.setAllowedMethods(Collections.unmodifiableList(new ArrayList<String>() {{
            add("HEAD");
            add("GET");
            add("POST");
            add("PUT");
            add("DELETE");
            add("PATCH");
            add("OPTIONS");
        }}));
        configuration.setAllowCredentials(true);
        configuration.setAllowedHeaders(Collections.unmodifiableList(new ArrayList<String>() {{
            add("Authorization");
            add("Cache-Control");
            add("Content-Type");
            add("content-type");
            add("x-auth-token");
            add("X-Auth-Token");
            add("user");
        }}));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    /**
     * Same as the default strategy (writes a message to the response), except when the response was already written.
     * A session can expire while a long request is in progress (e.g. a streamed response of the assistant):
     * when the container dispatches the completion of that request, the response body was already sent.
     */
    private SessionInformationExpiredStrategy expiredSessionStrategy() {
        return event -> {
            final HttpServletRequest request = event.getRequest();
            final HttpServletResponse response = event.getResponse();
            if (request.getDispatcherType() == DispatcherType.ASYNC || response.isCommitted()) {
                logger.fine(() -> "Expired session detected for an already written response: " + request.getRequestURI());
                return;
            }
            try {
                response.getWriter().print("This session has been expired (possibly due to multiple concurrent " +
                                           "logins being attempted as the same user).");
                response.flushBuffer();
            } catch (IllegalStateException e) {
                // the response body is written as a stream
                logger.fine(() -> "Cannot report the expired session for " + request.getRequestURI() + ": " + e.getMessage());
            }
        };
    }

    @Bean
    public ServletListenerRegistrationBean<HttpSessionEventPublisher> httpSessionEventPublisher() {
        return new ServletListenerRegistrationBean<>(new HttpSessionEventPublisher());
    }

    @Bean
    public RequestContextListener requestContextListener() {
        return new RequestContextListener();
    }

    public Filter blockFilter() {
        return new GenericFilterBean() {
            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
                if (enableBlockRequests) {
                    HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
                    HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
                    String ip = getRequestIp(httpRequest);
                    Tuple<Integer, LocalDateTime> cnt;
                    if (rejectedRequestCounter == null) {
                        rejectedRequestCounter = Collections.synchronizedMap(new HashMap<>());
                    }
                    if ((cnt = rejectedRequestCounter.get(ip)) != null && cnt.getKeyOne() == maxRejectedRequests) {
                        if (!httpResponse.isCommitted()) {
                            httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST);
                            httpRequest.getSession().invalidate();
                        }
                        throw new BadRequestException(ip + " has been blocked for " + blockInterval + " minutes");
                    }
                }
                filterChain.doFilter(servletRequest, servletResponse);
            }
        };
    }

    @Bean
    public RequestRejectedHandler requestRejectedHandler() {
        return new HttpStatusRequestRejectedHandler() {
            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response, RequestRejectedException requestRejectedException) throws IOException {
                logger.warning(requestRejectedException.getMessage() + ". Request details:\n" + prettyPrint(request));
                if (enableBlockRequests) {
                    HttpSession session = request.getSession();
                    String ip = getRequestIp(request);
                    if (rejectedRequestCounter == null) {
                        rejectedRequestCounter = Collections.synchronizedMap(new HashMap<>());
                    }
                    if (!rejectedRequestCounter.containsKey(ip)) {
                        rejectedRequestCounter.put(ip, new Tuple<>(0, LocalDateTime.now()));
                    }
                    rejectedRequestCounter.put(ip, new Tuple<>(rejectedRequestCounter.get(ip).getKeyOne() + 1, LocalDateTime.now()));
                    if (timer == null) {
                        long interval = blockInterval * 60000L;
                        timer = new Timer();
                        timer.scheduleAtFixedRate(new TimerTask() {
                            @Override
                            public void run() {
                                Tuple<Integer, LocalDateTime> tuple = rejectedRequestCounter.get(ip);
                                if (tuple != null && tuple.getKeyTwo().isBefore(LocalDateTime.now().minusMinutes(10))) {
                                    rejectedRequestCounter.remove(ip);
                                }
                            }
                        }, interval, interval);
                    }
                }
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
        };
    }

    private String[] apiEndpointsWithAuthToken() {
        String[] expressions = Endpoints.endpoints("ro.cs.tao").toArray(new String[0]);
        logger.fine("Security active for the following endpoints: " + String.join(",", expressions));
        logger.fine("Authentication method: " +
                EnumUtils.getEnumConstantByName(AuthenticationMode.class,
                        ConfigurationManager.getInstance().getValue("authentication.mode", "local").toUpperCase()).friendlyName());
        return expressions;
    }

    private String prettyPrint(HttpServletRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append("URL=").append(request.getRequestURL()).append("\n");
        builder.append("SessionID=").append(request.getSession().getId()).append("\n");
        builder.append("Headers=[");
        Enumeration<String> names = request.getHeaderNames();
        if (names != null) {
            String name = null;
            while (names.hasMoreElements()) {
                name = names.nextElement();
                builder.append(name).append(":").append(request.getHeader(name)).append(",");
            }
            if (name != null) {
                builder.setLength(builder.length() - 1);
            }
        }
        builder.append("]\n").append("Cookies=[");
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                builder.append(cookie.getName()).append(":").append(cookie.getValue()).append(",");
            }
            if (cookies.length > 0) {
                builder.setLength(builder.length() - 1);
            }
        }
        builder.append("]\n");
        return builder.toString();
    }

    private String getRequestIp(HttpServletRequest httpRequest) {
        String xfHeader = httpRequest.getHeader("X-Forwarded-For");
        if (xfHeader != null) {
            return xfHeader.split(",")[0];
        }
        return httpRequest.getRemoteAddr();
    }
}
