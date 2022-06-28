package ro.cs.tao.services.startup;

import ro.cs.eo.gdal.dataio.GDALLoader;

public class ViewerInitializer extends BaseLifeCycle {
    @Override
    public int priority() {
        return 12;
    }

    @Override
    public void onStartUp() {
        try {
            GDALLoader.getInstance().initGDAL();
            logger.fine("GDAL viewer initialized");
        } catch (Throwable error) {
            logger.warning("GDAL viewer cannot be initialized [cause: " + error.getMessage() + "]");
        }
    }

    @Override
    public void onShutdown() {

    }
}
