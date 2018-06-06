package ro.cs.tao.services.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.jaas.JaasAuthenticationToken;
import org.springframework.security.core.Authentication;


public class CustomAuthenticationProvider implements AuthenticationProvider {

    private AuthenticationProvider delegate;

    public CustomAuthenticationProvider(AuthenticationProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        JaasAuthenticationToken jaasAuthenticationToken = (JaasAuthenticationToken) delegate
          .authenticate(authentication);

        if (jaasAuthenticationToken.isAuthenticated()) {
            String userName = jaasAuthenticationToken.getPrincipal().toString();
            jaasAuthenticationToken.getLoginContext().getSubject().getPrincipals().add(new UserPrincipal(userName));
            // TODO: why user group is needed here?
            //jaasAuthenticationToken.getLoginContext().getSubject().getPrincipals().add(new GroupPrincipal("admin"));

            return jaasAuthenticationToken;
        } else {
            return null;
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return delegate.supports(authentication);
    }
}
