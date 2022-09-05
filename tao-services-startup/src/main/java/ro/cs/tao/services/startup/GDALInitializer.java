package ro.cs.tao.services.startup;

import ro.cs.eo.gdal.dataio.GDALLoader;

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
