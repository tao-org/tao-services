package ro.cs.tao.services.startup;

import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.services.bridge.spring.SpringContextBridge;
import ro.cs.tao.services.interfaces.UserService;
import ro.cs.tao.user.User;
import ro.cs.tao.user.UserStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Initializes user workspaces during application startup by creating necessary
 * workspace environments for all active users in the system.
 *
 * This class extends the {@code BaseLifeCycle} and overrides its lifecycle
 * methods to execute the initialization logic during the startup phase,
 * ensuring that every active user has their associated workspaces created.
 *
 * Key responsibilities include:
 * - Fetching a list of all users with an active status using the persistence manager.
 * - Invoking the {@code createWorkspaces} method of the {@code UserService}
 *   for each active user to initiate workspace creation.
 * - Logging any exceptions encountered during the workspace creation process
 *   without halting the initialization of other users.
 *
 * The priority of execution is set to ensure this initializer runs at a
 * predefined order relative to other lifecycle components.
 */
public class UserWorkspacesInitializer extends BaseLifeCycle {

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public void onStartUp() {
        final List<User> users = this.persistenceManager.users().list(UserStatus.ACTIVE);
        final UserService service = SpringContextBridge.services().getService(UserService.class);
        final Map<String, String> workspaceMap = new HashMap<>();
        String userWorkspaceId;
        for (User user : users) {
            try {
                userWorkspaceId = user.getWorkspaceId();
                if (userWorkspaceId == null) {
                    userWorkspaceId = user.getId();
                }
                service.createWorkspaces(userWorkspaceId);
                workspaceMap.put(user.getId(), userWorkspaceId);
            } catch (Throwable t) {
                logger.severe(t.getMessage());
            }
        }
        SystemVariable.setUserWorkspace(workspaceMap);
    }

    @Override
    public void onShutdown() {
        // Do nothing
    }
}
