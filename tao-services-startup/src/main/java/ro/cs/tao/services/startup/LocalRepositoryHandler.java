package ro.cs.tao.services.startup;

import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.security.SystemPrincipal;
import ro.cs.tao.user.User;
import ro.cs.tao.user.UserStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Ensures the existence of local user folders.
 */
public class LocalRepositoryHandler extends BaseLifeCycle {

    @Override
    public int priority() {
        return 3;
    }

    @Override
    public void onStartUp() {
        List<User> activeUsers = persistenceManager.users().list(UserStatus.ACTIVE);
        if (activeUsers != null) {
            final Path rootPath = Paths.get(SystemVariable.ROOT.value());
            for (User user : activeUsers) {
                if (!SystemPrincipal.instance().getName().equals(user.getUsername())) {
                    try {
                        Path userPath = rootPath.resolve(user.getUsername());
                        Files.createDirectories(userPath);
                        Files.createDirectories(userPath.resolve("files"));
                    } catch (IOException e) {
                        logger.severe(String.format("Failed to create user workspace [user=%s, reason=%s]",
                                                    user.getUsername(), e.getMessage()));
                    }
                }
            }
        }
    }

    @Override
    public void onShutdown() {

    }
}
