package ro.cs.eo.gdal.reader;

import ro.cs.eo.gdal.dataio.GDALTileCache;
import ro.cs.eo.gdal.dataio.drivers.*;
import ro.cs.eo.gdal.reader.info.ProductInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class TileReader {
    private static SpatialReference destReference;
    private static final Config config;
    private final GDALTileCache gdalTileCache;

    static {
        config = Config.getWebMercatorConfig();
    }

    public TileReader(GDALTileCache gdalTileCache) {
        this.gdalTileCache = gdalTileCache;
    }

    public void computeTile(ProductInfo productInfo, Path tileFilePath, int x, int y, byte z) throws IOException {
        GDAL.useExceptions();
        Dataset[] gdalDatasets = null;
        Dataset vrtDataset = null;
        Dataset warpDataset = null;
        Dataset translateDataset = null;
        try {
            if (!Files.exists(tileFilePath)) {
                Files.createDirectories(tileFilePath.getParent());
                if (this.config.reverseY) {
                    y = (1 << z) - 1 - y;
                }
                final Extent tileExtent = this.config.tileGrid.tileExtent(x, y, z);
                gdalDatasets = prepareDatasets(productInfo, tileExtent);
                vrtDataset = buildVRTDataset(productInfo, gdalDatasets);
                final String tmpFileName = "/vsimem/" + System.currentTimeMillis() + ".tif";
                if (productInfo.hasTiles()) {
                    warpDataset = buildWarpedDataset(productInfo, this.config, tileExtent, vrtDataset, tmpFileName);
                    translateDataset = buildTranslatedDataset(productInfo, warpDataset, tileFilePath.toString());
                } else {
                    /*translateDataset = buildTranslatedDataset(productInfo, this.config, tileExtent, vrtDataset, tmpFileName);
                    warpDataset = buildWarpedDataset(productInfo, this.config, tileExtent, translateDataset, tileFilePath.toString());*/
                    translateDataset = buildTranslatedDataset(productInfo, this.config, tileExtent, vrtDataset, tileFilePath.toString());
                }
                if (this.gdalTileCache.isCacheEnabled()){
                    final long tileSize = Files.size(tileFilePath);
                    this.gdalTileCache.putTile(String.valueOf(tileFilePath), tileSize);
                }
            }
        } finally {
            if (gdalDatasets != null) {
                for (Dataset gdalDataset : gdalDatasets) {
                    deleteDataset(gdalDataset, false);
                }
            }
            if (vrtDataset != null) {
                deleteDataset(vrtDataset, false);
            }
            if (warpDataset != null) {
                deleteDataset(warpDataset, false);
            }
            if (translateDataset != null) {
                deleteDataset(translateDataset, false);
            }
        }
    }

    public ImageInfo getInfo(ProductInfo productInfo) {
        GDAL.useExceptions();
        final Path productFilePath = Paths.get(productInfo.getProductPath());
        final String name = productFilePath.getFileName().toString();
        final String projectionCode = "EPSG:" + this.config.tileGrid.projectionCode;
        final String projectionUnit = this.config.tileGrid.unit;
        final String tileGrid = this.config.tileGrid.name;
        final String extent = "[" + this.config.tileGrid.extent.xmin + ", " + this.config.tileGrid.extent.ymin + ", " + this.config.tileGrid.extent.xmax + ", " + this.config.tileGrid.extent.ymax + "]";
        final StringBuilder resolutions = new StringBuilder("[");
        double resolution = 11;
        byte z;
        for (z = 0; z < 28 && resolution > 10; z++) {
            resolution = (this.config.tileGrid.extent.ymax - this.config.tileGrid.extent.ymin) / this.config.tileHeight / (1 << z);
            if (!resolutions.toString().equals("[")) {
                resolutions.append(", ");
            }
            resolutions.append(resolution);
        }
        resolutions.append("]");
        String path = Paths.get(productInfo.getProductBandsPaths()[0]).toAbsolutePath().toString();
        final Dataset gdalDataset = GDAL.open(path, GDALConst.gaReadonly());
        if (gdalDataset == null) {
            throw new IllegalStateException("Fail to open Dataset for file: " + productFilePath);
        }
        final Extent imageExtent = computeImageExtent(gdalDataset);
        deleteDataset(gdalDataset, false);
        final String projectionExtent = "[" + imageExtent.xmin + ", " + imageExtent.ymin + ", " + imageExtent.xmax + ", " + imageExtent.ymax + "]";
        final String center = "[" + (imageExtent.xmin + (imageExtent.xmax - imageExtent.xmin) / 2) + "," + (imageExtent.ymin + (imageExtent.ymax - imageExtent.ymin) / 2) + "]";
        return new ImageInfo(name, projectionCode, projectionUnit, projectionExtent, tileGrid, extent, resolutions.toString(), "0", String.valueOf(z), center);
    }

    private static double[] translateImagePointToMercatorPoint(Dataset imageDataset, double x, double y) {
        final SpatialReference sourceSpatialReference = new SpatialReference();
        String projectionRef = imageDataset.getProjectionRef();
        if (projectionRef.isEmpty()) {
            throw new IllegalArgumentException("Missing Geo-localisation data from dataset.");
        }
        /*if (projectionRef.contains("PROJCS")) {
            WKTReader reader = new WKTReader();
            Geometry geometry = null;
            try {
                geometry = reader.read(projectionRef);
                WKTWriter2 writer = new WKTWriter2();
                projectionRef = writer.write(geometry);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }*/
        if (!GDALConstConstants.ceNone().equals(sourceSpatialReference.importFromWkt(projectionRef))) {
            throw new IllegalStateException("Unable to import Spatial Reference " + projectionRef + " from WKT.");
        }
        if (destReference == null) {
            destReference = new SpatialReference();
            if (!GDALConstConstants.ceNone().equals(destReference.importFromEPSG(3857))) {
                throw new IllegalStateException("Unable to import Spatial Reference from EPSG:3857!");
            }
        }
        final CoordinateTransformation coordinateTransformation = CoordinateTransformation.createCoordinateTransformation(sourceSpatialReference, destReference);
        if (coordinateTransformation == null) {
            throw new RuntimeException("Cannot obtain coordinateTransformation");
        }
        final double[] mercatorPoint;
        if (!projectionRef.contains("WGS 84")) {
            mercatorPoint = coordinateTransformation.transformPoint(y, x);
        } else {
            mercatorPoint = coordinateTransformation.transformPoint(x, y);
        }
        return mercatorPoint;
    }

    private static Extent computeImageExtent(Dataset gdalDataset) {
        final Extent imageExtent = new Extent();
        final double[] geoTransform = new double[6];
        gdalDataset.getGeoTransform(geoTransform);
        // some products have the Y step incorrectly placed as z
        final double stepY = geoTransform[4] != 0 ? geoTransform[4] : geoTransform[5];
        final double[] minPoint = translateImagePointToMercatorPoint(gdalDataset, geoTransform[0],
                                                                                  geoTransform[3] + stepY * gdalDataset.getRasterYSize());
        final double[] maxPoint = translateImagePointToMercatorPoint(gdalDataset, geoTransform[0] + geoTransform[1] * gdalDataset.getRasterXSize(),
                                                                                  geoTransform[3]);
        imageExtent.xmin = minPoint[0];
        imageExtent.ymin = minPoint[1];
        imageExtent.xmax = maxPoint[0];
        imageExtent.ymax = maxPoint[1];
        return imageExtent;
    }

    private static boolean tileInsideImage(Extent imageExtent, Extent tileExtent) {
        return Math.max(tileExtent.xmin, imageExtent.xmin) < Math.min(tileExtent.xmax, imageExtent.xmax) &&
               Math.max(tileExtent.ymin, imageExtent.ymin) < Math.min(tileExtent.ymax, imageExtent.ymax);
    }

    private static void deleteDataset(Dataset dataset, boolean deleteFromDisk) {
        dataset.delete();
        final int size = dataset.getFileList().size();
        for (int i = 0; i < size; i++) {
            String datasetFile = (String) dataset.getFileList().get(i);
            if (datasetFile.startsWith("/vsimem/") || deleteFromDisk) {
                dataset.getDriver().delete(datasetFile);
            }
        }
    }

    private static Dataset[] prepareDatasets(ProductInfo productInfo, Extent tileExtent) {
        final List<Dataset> gdalDatasets = new ArrayList<>();
        final String[] productBandsPaths = productInfo.getProductBandsPaths();
        for (String productBandsPath : productBandsPaths) {
            final Dataset gdalDataset = GDAL.open(Paths.get(productBandsPath).toAbsolutePath().toString(), GDALConst.gaReadonly());
            if (gdalDataset == null) {
                throw new IllegalStateException("Fail to open Dataset for file: " + productBandsPath);
            }
            final Extent imageExtent = computeImageExtent(gdalDataset);
            if (tileInsideImage(imageExtent, tileExtent)) {
                gdalDatasets.add(gdalDataset);
            } else {
                deleteDataset(gdalDataset, false);
            }
        }
        if (gdalDatasets.size() < 1) {
            throw new IndexOutOfBoundsException("Tile out of bounds.");
        }

        return gdalDatasets.toArray(new Dataset[0]);
    }

    private static Dataset buildVRTDataset(ProductInfo productInfo, Dataset[] gdalDatasets) {
        if (gdalDatasets.length == 1) {
            return gdalDatasets[0];
        }
        final Vector<String> vrtOptions = new Vector<>();
        for (int productBandIndex : productInfo.getProductBandsIndexes()) {
            vrtOptions.add("-b");
            vrtOptions.add(String.valueOf(productBandIndex));
        }
        if (gdalDatasets.length > 1 && productInfo.isMultiBandProduct()) {
            vrtOptions.add("-separate");
        }
        final String tempVrtFileName = "/vsimem/" + System.currentTimeMillis() + ".vrt";
        Dataset vrtDataset = GDAL.buildVRT(tempVrtFileName, gdalDatasets, new BuildVRTOptions(vrtOptions));
        if (vrtDataset == null) {
            throw new IllegalStateException("Fail to build VRT dataset. " + GDAL.getLastErrorMsg());
        }
        return vrtDataset;
    }

    private static Dataset buildWarpedDataset(ProductInfo productInfo, Config config, Extent tileExtent, Dataset gdalDataset, String tileFilePath) {
        final Vector<String> warpOptions = new Vector<>();
        warpOptions.add("-t_srs");
        warpOptions.add("EPSG:" + config.tileGrid.projectionCode);
        warpOptions.add("-te");
        warpOptions.add(String.valueOf(tileExtent.xmin));
        warpOptions.add(String.valueOf(tileExtent.ymin));
        warpOptions.add(String.valueOf(tileExtent.xmax));
        warpOptions.add(String.valueOf(tileExtent.ymax));
        warpOptions.add("-ts");
        warpOptions.add(String.valueOf(config.tileWidth));
        warpOptions.add(String.valueOf(config.tileHeight));
        //warpOptions.add("-tap");
        if (productInfo.getProductBandsNoDataValue() != null) {
            warpOptions.add("-srcnodata");
            warpOptions.add(String.valueOf(productInfo.getProductBandsNoDataValue()));
            warpOptions.add("-dstnodata");
            warpOptions.add("0");
        }
        warpOptions.add("-multi");
        warpOptions.add("-wo");
        warpOptions.add("NUM_THREADS=ALL_CPUS");
        if (gdalDataset.getRasterCount() == 1 || gdalDataset.getRasterCount() == 3) {
            warpOptions.add("-dstalpha");
        }
        final Dataset warpDataset = GDAL.warp(tileFilePath, new Dataset[]{gdalDataset}, new WarpOptions(warpOptions));
        if (warpDataset == null) {
            throw new IllegalStateException("Fail to warp dataset. " + GDAL.getLastErrorMsg());
        }
        return warpDataset;
    }

    private static Dataset buildTranslatedDataset(ProductInfo productInfo, Dataset warpDataset, String tileFilePath) {
        Vector<String> translateOptions = new Vector<>();
        if (productInfo.getProductBandsPixelsMinScale() != null && productInfo.getProductBandsPixelsMaxScale() != null) {
            translateOptions.add("-scale");
            translateOptions.add(String.valueOf(productInfo.getProductBandsPixelsMinScale()));
            translateOptions.add(String.valueOf(productInfo.getProductBandsPixelsMaxScale()));
        }
        if (!GDALConstConstants.gdtByte().equals(warpDataset.getRasterBand(1).getDataType())) {
            translateOptions.add("-ot");
            translateOptions.add("Byte");
        }
        final Dataset translateDataset = GDAL.translate(tileFilePath, warpDataset, new TranslateOptions(translateOptions));
        if (translateDataset == null) {
            throw new IllegalStateException("Fail to translate dataset. " + GDAL.getLastErrorMsg());
        }
        return translateDataset;
    }

    private static Dataset buildTranslatedDataset(ProductInfo productInfo, Config config, Extent tileExtent, Dataset gdalDataset, String tileFilePath) {
        final Vector<String> translateOptions = new Vector<>();
        translateOptions.add("-strict");
        translateOptions.add("-a_nodata");
        //translateOptions.add("none");
        translateOptions.add(String.valueOf(productInfo.getProductBandsNoDataValue()));
        translateOptions.add("-projwin_srs");
        translateOptions.add("EPSG:" + config.tileGrid.projectionCode);
        translateOptions.add("-projwin");
        // Warning: map axes are not the same as image axes: (ulx uly) -> (xmin, ymax)
        translateOptions.add(String.valueOf(tileExtent.xmin));
        translateOptions.add(String.valueOf(tileExtent.ymax));
        translateOptions.add(String.valueOf(tileExtent.xmax));
        translateOptions.add(String.valueOf(tileExtent.ymin));
        translateOptions.add("-outsize");
        translateOptions.add(String.valueOf(config.tileWidth));
        translateOptions.add(String.valueOf(config.tileHeight));
        translateOptions.add("-a_srs");
        translateOptions.add("EPSG:" + config.tileGrid.projectionCode);
        Double[] val = new Double[1];
        gdalDataset.getRasterBand(1).getMinimum(val);
        Double minVal = val[0] != null ? val[0] : productInfo.getProductBandsPixelsMinScale();
        gdalDataset.getRasterBand(1).getMaximum(val);
        Double maxVal = val[0] != null ? val[0] : productInfo.getProductBandsPixelsMaxScale();
        if (minVal != null && maxVal != null) {
            translateOptions.add("-scale");
            translateOptions.add(String.valueOf(minVal));
            translateOptions.add(String.valueOf(maxVal));
        }
        if (!gdalDataset.getRasterBand(1).getDataType().equals(GDALConstConstants.gdtByte())) {
            translateOptions.add("-ot");
            translateOptions.add("Byte");
        }

        final Dataset translateDataset = GDAL.translate(tileFilePath, gdalDataset, new TranslateOptions(translateOptions));
        if (translateDataset == null) {
            throw new IllegalStateException("Fail to translate dataset. " + GDAL.getLastErrorMsg());
        }
        return translateDataset;
    }
}
