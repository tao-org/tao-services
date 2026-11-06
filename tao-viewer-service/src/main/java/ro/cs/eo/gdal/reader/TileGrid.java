package ro.cs.eo.gdal.reader;

import ro.cs.eo.gdal.dataio.drivers.CoordinateTransformation;
import ro.cs.eo.gdal.dataio.drivers.GDALConstConstants;
import ro.cs.eo.gdal.dataio.drivers.SpatialReference;

import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TileGrid {

    static final String WEB_MERCATOR_NAME = "web_mercator";
    static final String CUSTOM_GRID_NAME = "custom";

    final String name;
    final Extent extent;
    final String projectionCode;
    final String unit;

    private static final Map<String, TileGrid> customGridCache = new ConcurrentHashMap<>();
    private static final TileGrid WEB_MERCATOR_INSTANCE = webMercatorInternal();

    private TileGrid(String name, Extent extent, String projectionCode, String unit) {
        this.name = name;
        this.extent = extent;
        this.projectionCode = projectionCode;
        this.unit = unit;
    }

    static TileGrid webMercator() {
        return WEB_MERCATOR_INSTANCE;
    }

    private static TileGrid webMercatorInternal() {
        double originShift = 20037508.3427892480;
        Extent extent = new Extent();
        extent.xmin = -originShift;
        extent.ymin = -originShift;
        extent.xmax = originShift;
        extent.ymax = originShift;
        return new TileGrid(WEB_MERCATOR_NAME, extent, "3857", "m");
    }

    static TileGrid customGrid(String wkt) throws RuntimeException {
        return customGridCache.computeIfAbsent(wkt, key -> {
            SpatialReference mapSpatialReference = new SpatialReference();
            SpatialReference imageSpatialReference = new SpatialReference();
            CoordinateTransformation coordinateTransformation = null;
            try {
                int returnVal = mapSpatialReference.importFromEPSG(4326);
                if (returnVal != GDALConstConstants.ceNone()) {
                    throw new IllegalStateException("Unable to import Spatial Reference from EPSG.");
                }
                returnVal = imageSpatialReference.importFromWkt(wkt);
                if (returnVal != GDALConstConstants.ceNone()) {
                    throw new IllegalStateException("Unable to import Spatial Reference from WKT.");
                }
                String imageAuthorityCode = imageSpatialReference.getAuthorityCode(null);
                BBOX bbox = BBOX.getBBOXOfEPSG(imageAuthorityCode);
                coordinateTransformation = CoordinateTransformation.createCoordinateTransformation(mapSpatialReference, imageSpatialReference);
                
                double[] minXYCoordinates = coordinateTransformation.transformPoint(bbox.southLat, bbox.westLon);
                double[] maxXYCoordinates = coordinateTransformation.transformPoint(bbox.northLat, bbox.eastLon);
                
                Extent extent = new Extent();
                extent.xmin = minXYCoordinates[0];
                extent.ymin = minXYCoordinates[1];
                extent.xmax = maxXYCoordinates[0];
                extent.ymax = maxXYCoordinates[1];
                return new TileGrid(CUSTOM_GRID_NAME, extent, imageAuthorityCode, "m");
            } finally {
                // Critical: Explicitly delete JNI objects to prevent native memory bloat
                if (mapSpatialReference != null) mapSpatialReference.delete();
                if (imageSpatialReference != null) imageSpatialReference.delete();
                if (coordinateTransformation != null) coordinateTransformation.delete();
            }
        });
    }

    Extent tileExtent(int x, int y, byte z) {
        double tileW = (this.extent.xmax - this.extent.xmin) / (1 << z);
        double tileH = (this.extent.ymax - this.extent.ymin) / (1 << z);

        Extent tileExtent = new Extent();
        tileExtent.xmin = this.extent.xmin + tileW * x;
        tileExtent.ymin = this.extent.ymin + tileH * y;
        tileExtent.xmax = this.extent.xmin + tileW * (x + 1);
        tileExtent.ymax = this.extent.ymin + tileH * (y + 1);
        return tileExtent;
    }

    static class BBOX {

        private static String PROJ_DB_FILE = "";
        private static final Map<String, BBOX> bboxCache = new ConcurrentHashMap<>();
        private static Connection sharedConnection;

        static {
            PROJ_DB_FILE = TileGrid.class.getResource("proj.db").getPath();
        }

        public final double northLat;
        public final double westLon;
        public final double southLat;
        public final double eastLon;

        private BBOX(double northLat, double westLon, double southLat, double eastLon) {
            this.northLat = northLat;
            this.westLon = westLon;
            this.southLat = southLat;
            this.eastLon = eastLon;
        }

        private static BBOX getBBOXOfEPSG(String epsgCode) {
            // Optimization: Cache results to avoid DB hits
            return bboxCache.computeIfAbsent(epsgCode, code -> {
                    try {
                        if (sharedConnection == null || sharedConnection.isClosed()) {
                            sharedConnection = DriverManager.getConnection("jdbc:sqlite:" + PROJ_DB_FILE);
                        }
                        PreparedStatement preparedStatement = sharedConnection.prepareStatement(
                                "SELECT north_lat, west_lon, south_lat, east_lon FROM area where code=(SELECT area_of_use_code FROM projected_crs where code=?)"
                        );
                        preparedStatement.setQueryTimeout(30);
                        preparedStatement.setString(1, code);
                        ResultSet rs = preparedStatement.executeQuery();
                        if (rs.next()) {
                            return new BBOX(rs.getDouble(1), rs.getDouble(2), rs.getDouble(3), rs.getDouble(4));
                        } else {
                            throw new NullPointerException("BBOX not found for EPSG:" + code);
                        }
                    } catch (SQLException e) {
                        throw new IllegalStateException(e);
                    }
                });
            }
        }

    }
