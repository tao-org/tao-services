package ro.cs.tao.services.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.stereotype.Component;
import ro.cs.tao.persistence.PersistenceManager;

import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.io.IOException;
import java.util.Map;

@Component
//@ImportResource({"classpath*:tao-persistence-context.xml"})
public class TaoLocalLoginModule implements LoginModule {
    private UserPrincipal userPrincipal;
    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map sharedState;
    private Map options;
    private boolean succeeded = false;
    private boolean commitSucceeded = false;
    private String username;
    private String password;

    @Autowired
    private PersistenceManager persistenceMng;

    /*public TaoLocalLoginModule() {
        System.out.println("Login Module - constructor called");
    }*/

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
                           Map<String, ?> options) {

        System.out.println("Login Module - initialize called");
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = sharedState;
        this.options = options;

        succeeded = false;
    }

    @Override
    public boolean login() throws LoginException {
        System.out.println("Login Module - login called");
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

        System.out.println("username=" + username + " password=" + username);
        System.out.println("check credentials = " + (persistenceMng.checkLoginCredentials(username, password) ? "true" : "false"));

        // verify the username and password TODO use persistence
        if (persistenceMng.checkLoginCredentials(username, password)) {
            System.out.println("Successful login!");
            succeeded = true;
            return succeeded;

        } else {
            System.out.println("Invalid login credentials");
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
        System.out.println("Login Module - commit called");
        //return succeeded;

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
        System.out.println("Login Module - logout called");
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
        System.out.println("Login Module - abort called");
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
