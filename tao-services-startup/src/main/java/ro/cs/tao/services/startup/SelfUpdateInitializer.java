package ro.cs.tao.services.startup;

import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.services.commons.update.UpdateChecker;

/**
 * The SelfUpdateInitializer class is responsible for initializing the self-update mechanism
 * during application startup if the configuration indicates that update checks are required.
 *
 * This class extends the BaseLifeCycle and overrides its lifecycle methods to define
 * specific startup and shutdown behavior related to the update mechanism.
 *
 * On startup, it checks the configuration property "check.for.updates". If this property
 * is set to true, it initializes the UpdateChecker, which handles periodic update checks
 * and performs necessary updates based on available update metadata.
 *
 * The priority for this initializer is set to 11, which determines its position in
 * the startup lifecycle.
 *
 * Methods:
 * - priority(): Specifies the execution priority of this initializer. Returns 11.
 * - onStartUp(): Activates the self-update mechanism by initializing the UpdateChecker if necessary.
 * - onShutdown(): Defines the behavior on lifecycle shutdown, currently left empty.
 */
public class SelfUpdateInitializer extends BaseLifeCycle {
    @Override
    public int priority() { return 11; }

    @Override
    public void onStartUp() {
        if (Boolean.parseBoolean(ConfigurationManager.getInstance().getValue("check.for.updates", "false"))) {
            UpdateChecker.initialize();
        }
    }

    @Override
    public void onShutdown() {

    }
}
