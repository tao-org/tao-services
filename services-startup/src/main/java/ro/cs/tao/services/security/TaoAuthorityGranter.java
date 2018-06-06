package ro.cs.tao.services.security;

import org.springframework.security.authentication.jaas.AuthorityGranter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TaoAuthorityGranter implements AuthorityGranter {

    @Override
    public Set<String> grant(Principal principal) {

        /*final List<SimpleGrantedAuthority> authorities = (List<SimpleGrantedAuthority>)((SecurityUser) principal).getAuthorities();
        return authorities.stream().map(auth -> auth.toString()).collect(Collectors.toSet());*/

        Set<String> roles = new HashSet<String>();
        if (principal instanceof UserPrincipal) {
            roles.add(((UserPrincipal)principal).getName());
        } else {
            String user = principal.getName();
            if (user.indexOf('@') >= 0) {
                user = user.substring(0, user.indexOf('@'));
            }
            roles.add(user);
        }
        return roles;
    }
}
