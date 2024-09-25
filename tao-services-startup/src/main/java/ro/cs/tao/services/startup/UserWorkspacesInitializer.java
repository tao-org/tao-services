package ro.cs.tao.services.startup;

import org.springframework.beans.factory.annotation.Autowired;
import ro.cs.tao.services.interfaces.UserService;
import ro.cs.tao.user.User;
import ro.cs.tao.user.UserStatus;

import java.util.List;

public class UserWorkspacesInitializer extends BaseLifeCycle {

    @Autowired
    private UserService userService;

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public void onStartUp() {
        final List<User> users = this.persistenceManager.users().list(UserStatus.ACTIVE);
        for (User user : users) {
            try {
                userService.createWorkspaces(user.getId());
            } catch (Throwable t) {
                logger.severe(t.getMessage());
            }
        }
    }

    @Override
    public void onShutdown() {
        // Do nothing
    }
}
