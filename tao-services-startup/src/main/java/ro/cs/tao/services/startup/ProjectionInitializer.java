package ro.cs.tao.services.startup;

import ro.cs.tao.eodata.Projection;

import java.util.Map;

/**
 * The ProjectionInitializer class is responsible for initializing the Projection system
 * during the application startup lifecycle. It extends the BaseLifeCycle class,
 * inheriting the startup and shutdown mechanisms for managing component lifecycles.
 *
 * The class ensures that the supported coordinate reference system (CRS) projections
 * are loaded and reported when the application starts up. It leverages the static
 * Projection.getSupported() method to retrieve the mappings of projection codes and
 * descriptions.
 *
 * Functionality:
 * - Implements a priority level of 7 when determining the order of lifecycle component initialization.
 * - Logs the number of supported projections during the startup phase.
 * - Provides an empty implementation for the shutdown phase.
 *
 * Methods:
 * - priority(): Specifies the priority of this initializer in the application lifecycle.
 * - onStartUp(): Executes the logic for initializing supported projections and logs relevant information.
 * - onShutdown(): Defines the logic for component shutdown, currently unimplemented.
 *
 * This class is typically used as part of the services startup package to initialize and manage
 * required components during application runtime.
 */
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
