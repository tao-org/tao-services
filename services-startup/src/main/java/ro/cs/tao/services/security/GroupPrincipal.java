package ro.cs.tao.services.security;

import javax.security.auth.Subject;
import java.security.Principal;

public class GroupPrincipal implements Principal {

    private final String groupName;

    public GroupPrincipal(String groupName) {
        this.groupName = groupName;
    }

    @Override
    public String getName() { return this.groupName; }

    @Override
    public boolean implies(Subject subject) { return true; }
}
