package ro.cs.tao.services.startup;

import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.services.commons.update.UpdateChecker;

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
