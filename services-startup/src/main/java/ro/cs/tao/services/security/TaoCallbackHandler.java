package ro.cs.tao.services.security;

import javax.security.auth.callback.*;
import java.io.IOException;

public class TaoCallbackHandler implements CallbackHandler {

    private String name;
    private String password;

    public TaoCallbackHandler(String name, String password) {
        this.name = name;
        this.password = password;
    }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof NameCallback) {
                NameCallback nameCallback = (NameCallback) callbacks[i];
                nameCallback.setName(name);
            } else if (callbacks[i] instanceof PasswordCallback) {
                PasswordCallback passwordCallback = (PasswordCallback) callbacks[i];
                passwordCallback.setPassword(password.toCharArray());
            } else {
                throw new UnsupportedCallbackException(callbacks[i], "Callback not supported");
            }
        }
    }
}
