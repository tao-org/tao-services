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

import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.firewall.HttpStatusRequestRejectedHandler;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.GenericFilterBean;
import ro.cs.tao.EnumUtils;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.security.AuthenticationMode;
import ro.cs.tao.services.auth.token.TokenManagementService;
import ro.cs.tao.services.commons.Endpoints;
import ro.cs.tao.services.commons.TunnelFilter;
import ro.cs.tao.services.security.CustomAuthenticationProvider;
import ro.cs.tao.services.security.token.AuthenticationFilter;
import ro.cs.tao.services.security.token.TokenAuthenticationProvider;
import ro.cs.tao.spi.ServiceRegistry;
import ro.cs.tao.spi.ServiceRegistryManager;
import ro.cs.tao.utils.Tuple;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author Cosmin Cara
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {//implements ApplicationContextAware {

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
    private TokenManagementService tokenService;

    @Autowired
    private CustomAuthenticationProvider customAuthenticationProvider;

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        /**
         * 1st way, in memory auth
         */
        /*auth
                .inMemoryAuthentication()
                .withUser("admin").password("admin").roles("ADMIN")
                .and()
                .withUser("user").password("user").roles("USER");*/

        /**
         * 2nd way, with UserDetailsService
         */
        //auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());

        /**
         * 3rd way, with JAAS custom auth provider
         */
        auth.authenticationProvider(customAuthenticationProvider)
            .authenticationProvider(tokenProvider);

        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        final CorsConfiguration configuration = new CorsConfiguration();
        //configuration.setAllowedOrigins(Collections.unmodifiableList(new ArrayList<String>() {{ add("*"); }}));
        configuration.addAllowedOriginPattern("*");
        configuration.setAllowedMethods(Collections.unmodifiableList(new ArrayList<String>() {{
            add("HEAD"); add("GET"); add("POST"); add("PUT"); add("DELETE"); add("PATCH"); add("OPTIONS"); }}));
        // setAllowCredentials(true) is important, otherwise:
        // The value of the 'Access-Control-Allow-Origin' header in the response must not be the wildcard '*' when the request's credentials mode is 'include'.
        configuration.setAllowCredentials(true);
        // setAllowedHeaders is important! Without it, OPTIONS preflight request
        // will fail with 403 Invalid CORS request
        configuration.setAllowedHeaders(Collections.unmodifiableList(new ArrayList<String>() {{
            add("Authorization"); add("Cache-Control"); add("Content-Type"); add("content-type");
            add("x-auth-token"); add("X-Auth-Token"); add("user"); }}));
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public ServletListenerRegistrationBean<HttpSessionEventPublisher> httpSessionEventPublisher() {
        return new ServletListenerRegistrationBean<HttpSessionEventPublisher>(new HttpSessionEventPublisher());
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
                    final HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
                    final HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
                    final String ip = getRequestIp(httpRequest);
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
                    final HttpSession session = request.getSession();
                    final String ip = getRequestIp(request);
                    if (rejectedRequestCounter == null) {
                        rejectedRequestCounter = Collections.synchronizedMap(new HashMap<>());
                    }
                    if (!rejectedRequestCounter.containsKey(ip)) {
                        rejectedRequestCounter.put(ip, new Tuple<>(0, LocalDateTime.now()));
                    }
                    rejectedRequestCounter.put(ip, new Tuple<>(rejectedRequestCounter.get(ip).getKeyOne() + 1, LocalDateTime.now()));
                    if (timer == null) {
                        long interval = blockInterval * 60000;
                        timer = new Timer();
                        timer.scheduleAtFixedRate(new TimerTask() {
                            @Override
                            public void run() {
                                final Tuple<Integer, LocalDateTime> tuple = rejectedRequestCounter.get(ip);
                                if (tuple.getKeyTwo().isBefore(LocalDateTime.now().minusMinutes(10))) {
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

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        final HttpSecurity httpSecurity = http
                .sessionManagement()
                    .maximumSessions(2).maxSessionsPreventsLogin(false).sessionRegistry(sessionRegistry())
                .and()
                    .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
                .and()
                    .headers().frameOptions().sameOrigin()
                .and()
                    .authorizeRequests()
                        .antMatchers(Endpoints.REFRESH_ENDPOINT, Endpoints.DOWNLOAD_ENDPOINT)
                            .permitAll()
                        .antMatchers(Endpoints.LOGIN_ENDPOINT)
                            .authenticated()
                .and()
                    .httpBasic()
                .and()
                    .authenticationProvider(customAuthenticationProvider)
                        .authorizeRequests()
                        .antMatchers(apiEndpointsWithAuthToken())
                        .authenticated()
                .and()
                    .authenticationProvider(tokenProvider)
                .addFilterBefore(new AuthenticationFilter(authenticationManager()), BasicAuthenticationFilter.class);
        if (enableBlockRequests) {
            httpSecurity.addFilterBefore(blockFilter(), BasicAuthenticationFilter.class);
        }
        if (ConfigurationManager.getInstance().getBooleanValue("enable.tunnel.filters")) {
            final ServiceRegistry<TunnelFilter> registry = ServiceRegistryManager.getInstance().getServiceRegistry(TunnelFilter.class);
            final Set<TunnelFilter> tunnelFilters = registry.getServices();
            if (tunnelFilters != null && !tunnelFilters.isEmpty()) {
                Class<? extends Filter> afterClass = AuthenticationFilter.class;
                for (TunnelFilter filter : tunnelFilters) {
                    http.addFilterAfter(filter, afterClass);
                    Logger.getLogger(WebSecurityConfig.class.getName())
                          .fine("Registered tunnel filter: " + filter.getClass());
                    afterClass = filter.getClass();
                }
            }
        }
        http.authorizeRequests()
                .antMatchers(Endpoints.GLOBAL_PATH_EXPRESSION)
                    // permit all after token management
                    .permitAll()
            .and()
                // replace code above to bypass token management
                /*.authenticated()
                .and()
                .httpBasic()
                .and()
                .authenticationProvider(customAuthProvider())*/
                .csrf()
                    .disable()
            .cors();
    }

    private String[] apiEndpointsWithAuthToken() {
        String[] expressions = Endpoints.endpoints("ro.cs.tao").toArray(new String[0]);;
        Logger.getLogger(WebSecurityConfig.class.getName())
                .fine("Security active for the following endpoints: " + String.join(",", expressions));
        Logger.getLogger(WebSecurityConfig.class.getName())
                .fine("Authentication method: " +
                        EnumUtils.getEnumConstantByName(AuthenticationMode.class,
                                                        ConfigurationManager.getInstance().getValue("authentication.mode", "local")
                                                                            .toUpperCase()).friendlyName());
        return expressions;
    }

    private String prettyPrint(HttpServletRequest request) {
        final StringBuilder builder = new StringBuilder();
        builder.append("URL=").append(request.getRequestURL()).append("\n");
        builder.append("SessionID=").append(request.getSession().getId()).append("\n");
        builder.append("Headers=[");
        final Enumeration<String> names = request.getHeaderNames();
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
        final Cookie[] cookies = request.getCookies();
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
        final String xfHeader = httpRequest.getHeader("X-Forwarded-For");
        final String ip;
        if (xfHeader != null) {
            ip = xfHeader.split(",")[0];
        } else {
            ip = httpRequest.getRemoteAddr();
        }
        return ip;
    }
}