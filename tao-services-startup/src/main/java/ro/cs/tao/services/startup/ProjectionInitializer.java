package ro.cs.tao.services.startup;

import ro.cs.tao.eodata.Projection;

import java.util.Map;

public class ProjectionInitializer extends BaseLifeCycle {
    @Override
    public int priority() {
        return 7;
    }

    @Override
    public void onStartUp() {
        final Map<String, String> map = Projection.getSupported();
        logger.fine(String.format("Projection database initialized [%d projections]", map.size()));
    }

    @Override
    public void onShutdown() {

    }
}
