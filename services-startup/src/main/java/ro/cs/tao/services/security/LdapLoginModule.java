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
package ro.cs.tao.services.security;

import ro.cs.tao.ldap.TaoLdapClient;

import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

public class LdapLoginModule implements LoginModule {

    private static final Logger logger = Logger.getLogger(LdapLoginModule.class.getName());

    private UserPrincipal userPrincipal;
    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map sharedState;
    private Map options;
    private boolean succeeded = false;
    private boolean commitSucceeded = false;
    private String username;
    private String password;

    public TaoLdapClient ldapClient;

    public LdapLoginModule() {
        ldapClient = new TaoLdapClient();
    }

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
                           Map<String, ?> options) {

        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = sharedState;
        this.options = options;

        succeeded = false;
    }

    @Override
    public boolean login() throws LoginException {
        if (callbackHandler == null) {
            throw new LoginException("CallbackHandler null!");
        }

        Callback[] callbacks = new Callback[2];
        callbacks[0] = new NameCallback("name:");
        callbacks[1] = new PasswordCallback("password:", false);

        try {
            // call callback handler
            callbackHandler.handle(callbacks);
        } catch (IOException e) {
            throw new LoginException("IOException calling handle on callbackHandler : " + e.getMessage());
        } catch (UnsupportedCallbackException e) {
            throw new LoginException("UnsupportedCallbackException calling handle on callbackHandler : " + e.getMessage());
        }

        NameCallback nameCallback = (NameCallback) callbacks[0];
        PasswordCallback passwordCallback = (PasswordCallback) callbacks[1];

        username = nameCallback.getName();
        password = new String(passwordCallback.getPassword());

        // verify the username and password
        if (ldapClient.checkLoginCredentials(username, password)) {
            logger.fine("LDAP Login Module - Successful login!");
            succeeded = true;
            return succeeded;

        } else {
            logger.fine("LDAP Login Module - Invalid login credentials");
            succeeded = false;

            username = null;
            password = null;

            throw new FailedLoginException("Invalid login credentials.");
        }
    }

    /**
     * This method is called if the LoginContext's overall authentication succeeded
     * (the relevant REQUIRED, REQUISITE, SUFFICIENT and OPTIONAL LoginModules succeeded).
     * @return true if this LoginModule's own login and commit attempts succeeded, or false otherwise.
     * @throws LoginException
     */
    @Override
    public boolean commit() throws LoginException {
        if (succeeded == false) {
            return false;
        } else {
            // add a Principal (authenticated identity) to the Subject

            // assume the user we authenticated is the SamplePrincipal
            userPrincipal = new UserPrincipal(username);
            if (!subject.getPrincipals().contains(userPrincipal))
                subject.getPrincipals().add(userPrincipal);

            // erase username and password values
            username = null;
            password = null;

            commitSucceeded = true;
            return true;
        }
    }

    @Override
    public boolean logout() throws LoginException {
        subject.getPrincipals().remove(userPrincipal);
        succeeded = false;
        succeeded = commitSucceeded;
        username = null;
        password = null;
        userPrincipal = null;
        return true;
    }

    /**
     * This method is called if the LoginContext's overall authentication failed.
     * (the relevant REQUIRED, REQUISITE, SUFFICIENT and OPTIONAL LoginModules did not succeed).
     * @return false if this LoginModule's own login and/or commit attempts failed, and true otherwise.
     * @throws LoginException if the abort fails
     */
    @Override
    public boolean abort() throws LoginException {
        if (succeeded == false) {
            return false;
        } else if (commitSucceeded == false) {
            // login succeeded but overall authentication failed
            succeeded = false;
            username = null;
            password = null;
            userPrincipal = null;
        } else {
            // overall authentication succeeded and commit succeeded, but someone else's commit failed
            logout();
        }
        return true;
    }

}
