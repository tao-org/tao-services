package ro.cs.tao.services.startup;

import ro.cs.eo.gdal.dataio.GDALLoader;

/**
 * The GDALInitializer class is responsible for initializing the GDAL (Geospatial Data Abstraction Library)
 * binaries during the application startup lifecycle.
 *
 * This class extends the BaseLifeCycle and overrides its lifecycle methods to ensure that the
 * GDAL binaries are properly initialized and ready for use.
 */
public class GDALInitializer extends BaseLifeCycle {
    @Override
    public int priority() {
        return 12;
    }

    @Override
    public void onStartUp() {
        try {
            GDALLoader.getInstance().initGDAL();
            logger.fine("GDAL binaries initialized");
        } catch (Throwable error) {
            logger.warning("GDAL binaries cannot be initialized [cause: " + error.getMessage() + "]");
        }
    }

    @Override
    public void onShutdown() {

    }
}
