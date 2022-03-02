package ro.cs.tao.services.security;

import ro.cs.tao.keycloak.KeycloakClient;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.security.AuthenticationMode;
import ro.cs.tao.security.TaoLoginModule;
import ro.cs.tao.security.TokenCache;
import ro.cs.tao.security.UserPrincipal;
import ro.cs.tao.user.Group;
import ro.cs.tao.user.User;

import java.util.List;
import java.util.stream.Collectors;

public class KeycloakLoginModule extends TaoLoginModule {

    private final KeycloakClient client;

    public KeycloakLoginModule() {
        super();
        this.client = new KeycloakClient();
        final TokenCache cache = TokenCacheManager.getCache();
        this.client.setTokenKeeper(cache::put);
    }

    @Override
    protected AuthenticationMode intendedFor() {
        return AuthenticationMode.KEYCLOAK;
    }

    @Override
    protected boolean shouldEncryptPassword() {
        return false;
    }

    @Override
    protected User loginImpl(String user, String password) {
        if (subject.getPrincipals().contains(new UserPrincipal(user))) {
            return new User();
        }
        if (userProvider == null) {
            throw new RuntimeException("[userProvider] not set");
        }
        User userEntity = this.client.checkLoginCredentials(username, password);
        if (userEntity != null) {
            User existing = userProvider.getByName(user);
            try {
                if (existing == null) {
                    final boolean isAdmin = username.equals(this.client.getAdminUser());
                    final List<Group> groups = userProvider.listGroups().stream()
                            .filter(g -> (isAdmin ? "ADMIN" : "USER").equalsIgnoreCase(g.getName())).collect(Collectors.toList());
                    userEntity.setGroups(groups);
                    userEntity.setPassword(password);
                    if (!isAdmin) {
                        userEntity.setProcessingQuota(userProvider.defaultCPUQuota());
                        userEntity.setMemoryQuota(userProvider.defaultMemoryQuota());
                    }
                    existing = userEntity;
                    userProvider.save(existing);
                }
                userEntity = existing;
                logger.finest("Keycloak login: " + user);
            } catch (PersistenceException e) {
                logger.severe(String.format("Cannot persist user '%s'. Reason: %s", user, e.getMessage()));
            }
        }
        return userEntity;
    }
}
