package ro.cs.tao.services.startup;

import ro.cs.tao.services.bridge.spring.SpringContextBridge;
import ro.cs.tao.services.interfaces.UserService;
import ro.cs.tao.user.User;
import ro.cs.tao.user.UserStatus;

import java.util.List;

public class UserWorkspacesInitializer extends BaseLifeCycle {

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public void onStartUp() {
        final List<User> users = this.persistenceManager.users().list(UserStatus.ACTIVE);
        final UserService service = SpringContextBridge.services().getService(UserService.class);
        for (User user : users) {
            try {
                service.createWorkspaces(user.getId());
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
