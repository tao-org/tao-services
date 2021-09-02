package ro.cs.tao.services.security;

import ro.cs.tao.keycloak.KeycloakClient;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.security.TaoLoginModule;
import ro.cs.tao.user.Group;
import ro.cs.tao.user.User;

import java.util.List;
import java.util.stream.Collectors;

public class KeycloakLoginModule extends TaoLoginModule {

    private final KeycloakClient client;

    public KeycloakLoginModule() {
        super();
        this.client = new KeycloakClient();
    }

    @Override
    protected boolean shouldEncryptPassword() {
        return false;
    }

    @Override
    protected User loginImpl(String user, String password) {
        if (userProvider == null) {
            throw new RuntimeException("[userProvider] not set");
        }
        User userEntity = this.client.checkLoginCredentials(username, password);
        if (userEntity != null) {
            final User existing = userProvider.getByName(user);
            if (existing == null) {
                try {
                    final List<Group> groups = userProvider.listGroups().stream().filter(g -> "USER".equalsIgnoreCase(g.getName())).collect(Collectors.toList());
                    userEntity.setGroups(groups);
                    userProvider.save(userEntity);
                    userEntity.setPassword(password);
                } catch (PersistenceException e) {
                    logger.severe(String.format("Cannot persist user '%s'. Reason: %s", user, e.getMessage()));
                }
            } else {
                userEntity = existing;
            }
        }
        return userEntity;
    }
}
