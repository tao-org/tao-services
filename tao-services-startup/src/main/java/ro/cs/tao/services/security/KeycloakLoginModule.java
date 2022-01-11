package ro.cs.tao.services.security;

import ro.cs.tao.keycloak.KeycloakClient;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.security.AuthenticationMode;
import ro.cs.tao.security.TaoLoginModule;
import ro.cs.tao.security.TokenCache;
import ro.cs.tao.security.UserPrincipal;
import ro.cs.tao.user.Group;
import ro.cs.tao.user.User;
import ro.cs.tao.user.UserPreference;

import java.util.ArrayList;
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
                    final List<Group> groups = userProvider.listGroups().stream().filter(g -> "USER".equalsIgnoreCase(g.getName())).collect(Collectors.toList());
                    userEntity.setGroups(groups);
                    userEntity.setPassword(password);
                    existing = userEntity;
                    userProvider.save(existing);
                } else {
                    List<UserPreference> preferences = existing.getPreferences();
                    if (preferences == null) {
                        preferences = new ArrayList<>();
                    }
                    final List<UserPreference> uPrefs = userEntity.getPreferences();
                    for (UserPreference uP : uPrefs) {
                        final UserPreference pref = preferences.stream().filter(p -> p.getKey().equals(uP.getKey())).findFirst().orElse(null);
                        if (pref != null) {
                            pref.setValue(uP.getValue());
                        } else {
                            preferences.add(pref);
                        }
                    }
                    userProvider.update(existing);
                }
                userEntity = existing;
            } catch (PersistenceException e) {
                logger.severe(String.format("Cannot persist user '%s'. Reason: %s", user, e.getMessage()));
            }
        }
        return userEntity;
    }
}
