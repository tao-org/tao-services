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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.session.web.http.HeaderHttpSessionStrategy;
import org.springframework.session.web.http.HttpSessionStrategy;
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

    /*@Autowired
    private TaoUserDetailsService userDetailsService;*/

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
    public HttpSessionStrategy httpSessionStrategy() {
        return new HeaderHttpSessionStrategy();
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


    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
          .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
          .and()
          .headers().frameOptions().sameOrigin()
          .and()
          .authorizeRequests()
                .antMatchers(Endpoints.LOGIN_ENDPOINT, Endpoints.REFRESH_ENDPOINT)
                    .permitAll()
                .antMatchers(Endpoints.LOGIN_ENDPOINT)
                    .authenticated()
                    .and()
                    .httpBasic()
                    .and()
                    .authenticationProvider(customAuthenticationProvider)
          /*.authorizeRequests()
                .antMatchers(BaseController.ADMIN_SERVICE_PATH_EXPRESSION).hasRole("ADMIN").and()*/
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
        String[] expressions = Endpoints.endpoints("ro.cs.tao");
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