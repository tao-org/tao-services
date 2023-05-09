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
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import ro.cs.tao.EnumUtils;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.security.AuthenticationMode;
import ro.cs.tao.services.auth.token.TokenManagementService;
import ro.cs.tao.services.commons.Endpoints;
import ro.cs.tao.services.security.CustomAuthenticationProvider;
import ro.cs.tao.services.security.token.AuthenticationFilter;
import ro.cs.tao.services.security.token.TokenAuthenticationProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * @author Cosmin Cara
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {//implements ApplicationContextAware {

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
            add("Authorization"); add("Cache-Control"); add("Content-Type"); add("x-auth-token"); add("user"); }}));
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

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
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
                .addFilterBefore(new AuthenticationFilter(authenticationManager()), BasicAuthenticationFilter.class)
          .authorizeRequests()
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
                .disable();
        http.cors();
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
}