/*
 * Copyright (C) 2017 CS ROMANIA
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

import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.jaas.*;
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
import ro.cs.tao.services.auth.token.TokenManagementService;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.security.CustomAuthenticationProvider;
import ro.cs.tao.services.security.TaoAuthorityGranter;
import ro.cs.tao.services.security.token.AuthenticationFilter;
import ro.cs.tao.services.security.token.TokenAuthenticationProvider;

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
        auth.authenticationProvider(customAuthProvider())
            .authenticationProvider(tokenAuthProvider());

        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        final PasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder;
    }

    @Bean
    public HttpSessionStrategy httpSessionStrategy() {
        return new HeaderHttpSessionStrategy();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        final CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(ImmutableList.of("*"));
        configuration.setAllowedMethods(ImmutableList.of("HEAD", "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        // setAllowCredentials(true) is important, otherwise:
        // The value of the 'Access-Control-Allow-Origin' header in the response must not be the wildcard '*' when the request's credentials mode is 'include'.
        configuration.setAllowCredentials(true);
        // setAllowedHeaders is important! Without it, OPTIONS preflight request
        // will fail with 403 Invalid CORS request
        configuration.setAllowedHeaders(ImmutableList.of("Authorization", "Cache-Control", "Content-Type", "x-auth-token"));
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }


    @Override
    protected void configure(HttpSecurity http) throws Exception {
        /*http
                .csrf().disable()
                .authorizeRequests()
                .antMatchers("/**").authenticated()
                .and()
                .httpBasic()
                .authenticationEntryPoint(entryPoint);
        http.sessionManagement().maximumSessions(1);
        http.cors();*/

        http
          .sessionManagement()
                .maximumSessions(1)
                .and()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
          .and()
          .authorizeRequests()
                .antMatchers(BaseController.LOGIN_ENDPOINT)
                .authenticated()
                .and()
                .httpBasic()
                .and()
                .authenticationProvider(customAuthProvider())
          /*.authorizeRequests()
                .antMatchers(BaseController.ADMIN_SERVICE_PATH_EXPRESSION).hasRole("ADMIN").and()*/
          .authorizeRequests()
                .antMatchers(apiEndpointsWithAuthToken())
                .authenticated()
                .and()
                .authenticationProvider(tokenAuthProvider())
                .addFilterBefore(new AuthenticationFilter(authenticationManager()), BasicAuthenticationFilter.class)
          .authorizeRequests()
                .antMatchers(BaseController.GLOBAL_PATH_EXPRESSION)

                // permit all after token management
                .permitAll()
                .and()

                // replace code above to depass token management
                /*.authenticated()
                .and()
                .httpBasic()
                .and()
                .authenticationProvider(customAuthProvider())*/

                .csrf()
                .disable();
        http.cors();
    }

    @Bean
    public AuthenticationProvider customAuthProvider() {
        return new CustomAuthenticationProvider(jaasAuthProvider(), tokenService);
    }

    @Bean
    public JaasAuthenticationProvider jaasAuthProvider() {
        JaasAuthenticationProvider authenticationProvider = new JaasAuthenticationProvider();
        authenticationProvider.setAuthorityGranters(new AuthorityGranter[] { new TaoAuthorityGranter() });
        authenticationProvider.setCallbackHandlers(new JaasAuthenticationCallbackHandler[] {
          new JaasNameCallbackHandler(), new JaasPasswordCallbackHandler() });
        authenticationProvider.setLoginContextName("taologin");
        authenticationProvider.setLoginConfig(new ClassPathResource("tao_jaas.config"));
        return authenticationProvider;
    }

    @Bean
    public AuthenticationProvider tokenAuthProvider() {
        return tokenProvider;
    }

    private String[] apiEndpointsWithAuthToken() {
        return new String[]{BaseController.ADMIN_SERVICE_PATH_EXPRESSION, BaseController.USER_SERVICE_PATH_EXPRESSION,
          BaseController.DATA_QUERY_SERVICE_PATH_EXPRESSION,
          //BaseController.MONITORING_SERVICE_PATH_EXPRESSION,
          BaseController.COMPONENT_SERVICE_PATH_EXPRESSION,
          //BaseController.CONFIGURATION_SERVICE_PATH_EXPRESSION,
          BaseController.CONTAINER_SERVICE_PATH_EXPRESSION,
          BaseController.DATA_SOURCE_COMPONENT_SERVICE_PATH_EXPRESSION,
          BaseController.FILES_SERVICE_PATH_EXPRESSION, BaseController.PRODUCT_SERVICE_PATH_EXPRESSION,
          BaseController.QUERY_SERVICE_PATH_EXPRESSION, BaseController.TOPOLOGY_SERVICE_PATH_EXPRESSION,
          BaseController.WORKFLOW_SERVICE_PATH_EXPRESSION, BaseController.ORCHESTRATOR_SERVICE_PATH_EXPRESSION,
          BaseController.LOGOUT_ENDPOINT};
    }
}