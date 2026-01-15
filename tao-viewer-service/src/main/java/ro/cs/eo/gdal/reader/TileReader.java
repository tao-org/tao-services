package ro.cs.eo.gdal.reader;

import org.geotools.geojson.geom.GeometryJSON;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import ro.cs.eo.gdal.dataio.GDALTileCache;
import ro.cs.eo.gdal.dataio.drivers.*;
import ro.cs.eo.gdal.reader.info.ExpressionEvaluator;
import ro.cs.eo.gdal.reader.info.FormatDescriptor;
import ro.cs.eo.gdal.reader.info.ProductDescriptor;
import ro.cs.tao.utils.FileUtilities;
import ro.cs.tao.utils.StringUtilities;
import ro.cs.tao.utils.Triple;
import ro.cs.tao.utils.executors.Executor;
import ro.cs.tao.utils.executors.ExecutorType;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

public class TileReader {
    private static SpatialReference destReference;
    private static final Config config;
    private static final Map<Path, Triple<Extent, double[], LocalDateTime>> scaleCache;
    private static final Timer evictor;
    private final GDALTileCache gdalTileCache;

    static {
        config = Config.getWebMercatorConfig();
        scaleCache = Collections.synchronizedMap(new HashMap<>());
        evictor = new Timer();
        evictor.schedule(new TimerTask() {
            @Override
            public void run() {
                final LocalDateTime reference = LocalDateTime.now().minusHours(1);
                scaleCache.entrySet().removeIf(entry -> entry.getValue().getKeyThree().isBefore(reference));
            }
        }, 3600000, 3600000);
    }

    public static void setScale(Path file, double[] minMax) {
        Triple<Extent, double[], LocalDateTime> triple = scaleCache.get(file);
        if (triple != null) {
            scaleCache.replace(file, new Triple<>(triple.getKeyOne(), minMax, LocalDateTime.now()));
        }
    }

    public static double[] getScale(Path file) {
        Triple<Extent, double[], LocalDateTime> triple = scaleCache.get(file);
        return triple != null ? triple.getKeyTwo() : null;
    }

    public TileReader(GDALTileCache gdalTileCache) {
        this.gdalTileCache = gdalTileCache;
    }

    public void computeTile(ProductDescriptor productDescriptor, Path tileFilePath, int x, int y, byte z) throws IOException {
        Dataset[] gdalDatasets = null;
        Dataset vrtDataset = null;
        Dataset warpDataset = null;
        Dataset translateDataset = null;
        try {
            GDAL.useExceptions();
            //if (!Files.exists(tileFilePath)) {
            FileUtilities.createDirectories(tileFilePath.getParent());
            if (config.reverseY) {
                y = (1 << z) - 1 - y;
            }
            final Extent tileExtent = config.tileGrid.tileExtent(x, y, z);
            gdalDatasets = prepareDatasets(productDescriptor, tileExtent);
            vrtDataset = buildVRTDataset(productDescriptor, gdalDatasets);
            final String tmpFileName = "/vsimem/" + System.currentTimeMillis() + ".tif";
            warpDataset = buildWarpedDataset(productDescriptor, tileExtent, vrtDataset, tmpFileName);
            translateDataset = buildTranslatedDataset(productDescriptor, warpDataset, tileFilePath.toString());
            if (this.gdalTileCache.isCacheEnabled()){
                final long tileSize = Files.size(tileFilePath);
                this.gdalTileCache.putTile(tileFilePath, tileSize);
            }
            //}
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

    private void writeGeoJSON(String wkt, Path outFile) throws IOException, ParseException {
        try (OutputStream out = Files.newOutputStream(outFile)) {
            WKTReader reader = new WKTReader();
            Geometry geom = reader.read(wkt);
            GeometryJSON gJson = new GeometryJSON();
            if (geom.getNumGeometries() > 1) {
                geom = geom.getEnvelope();
            }
            gJson.write(geom, out);
        }
    }


    public void extractSubset(ProductDescriptor productDescriptor, Path filePath, double x, double y, double width, double height, String wkt) throws Exception {
        Dataset[] gdalDatasets = null;
        Dataset vrtDataset = null;
        Dataset warpDataset = null;
        Dataset translateDataset = null;
        try {
            GDAL.useExceptions();
            FileUtilities.createDirectories(filePath.getParent());
            final Path shapeFile = filePath.getParent().resolve("wkt.shp");
            writeGeoJSON(wkt, shapeFile);
            if (config.reverseY) {
                y = - y;
            }
            final Extent extent = new Extent(x, y, x + width, y + height);
            gdalDatasets = prepareDatasets2(productDescriptor);
            vrtDataset = buildVRTDataset(productDescriptor, gdalDatasets);
            //final String tmpFileName = "/vsimem/" + System.currentTimeMillis() + ".tif";
            final String warpFileName = filePath.getParent().resolve("warped.tif").toString();
            warpDataset = buildWarpedDataset2(productDescriptor, extent, vrtDataset, warpFileName);
            buildRasterizedDataset(shapeFile, warpFileName, warpDataset.getRasterCount());
            warpDataset = GDAL.open(warpFileName, GDALConst.gaReadonly());
            translateDataset = buildTranslatedDataset(productDescriptor, warpDataset, filePath.toString().replace(".tif", ".png"));
            Files.deleteIfExists(shapeFile);
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

    public ImageInfo getInfo(ProductDescriptor productDescriptor) {
        final Path productFilePath = productDescriptor.getProductPath();
        final String name = productFilePath.getFileName().toString();
        final String projectionCode = "EPSG:" + config.tileGrid.projectionCode;
        final String projectionUnit = config.tileGrid.unit;
        final String tileGrid = config.tileGrid.name;
        final String extent = "[" + config.tileGrid.extent.xmin + ", " + config.tileGrid.extent.ymin + ", " + config.tileGrid.extent.xmax + ", " + config.tileGrid.extent.ymax + "]";
        final StringBuilder resolutions = new StringBuilder("[");
        double resolution = 11;
        byte z;
        for (z = 0; z < 28 && resolution > 10; z++) {
            resolution = (config.tileGrid.extent.ymax - config.tileGrid.extent.ymin) / config.tileHeight / (1 << z);
            if (!resolutions.toString().equals("[")) {
                resolutions.append(", ");
            }
            resolutions.append(resolution);
        }
        resolutions.append("]");
        String path = productDescriptor.getBandPaths()[0].toAbsolutePath().toString();
        final Dataset gdalDataset = GDAL.open(path, GDALConst.gaReadonly());
        if (gdalDataset == null) {
            throw new IllegalStateException("Fail to open Dataset for file: " + productFilePath + ". " + GDAL.getLastErrorMsg());
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
        if (!GDALConstConstants.ceNone().equals(sourceSpatialReference.importFromWkt(projectionRef))) {
            throw new IllegalStateException("Unable to import Spatial Reference " + projectionRef + " from WKT. " + GDAL.getLastErrorMsg());
        }
        sourceSpatialReference.setAxisMappingStrategy(0);
        if (destReference == null) {
            synchronized (TileReader.class) {
                destReference = new SpatialReference();
                final int statusCode = destReference.importFromEPSG(3857);
                if (!GDALConstConstants.ceNone().equals(statusCode) || destReference.getJniInstance().toString().isEmpty()) {
                    throw new IllegalStateException("Unable to import Spatial Reference from EPSG:3857! " + GDAL.getLastErrorMsg());
                }
            }
        }
        final CoordinateTransformation coordinateTransformation = CoordinateTransformation.createCoordinateTransformation(sourceSpatialReference, destReference);
        if (coordinateTransformation == null) {
            throw new RuntimeException("Cannot obtain coordinateTransformation");
        }
        return coordinateTransformation.transformPoint(x, y);
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

    private static Extent getImageExtent(Dataset gdalDataset) {
        final Extent imageExtent = new Extent();
        final double[] geoTransform = new double[6];
        gdalDataset.getGeoTransform(geoTransform);
        // some products have the Y step incorrectly placed as z
        final double stepY = geoTransform[4] != 0 ? geoTransform[4] : geoTransform[5];
        final double[] minPoint = new double[] { geoTransform[0], geoTransform[3] + stepY * gdalDataset.getRasterYSize() };
        final double[] maxPoint = new double[] { geoTransform[0] + geoTransform[1] * gdalDataset.getRasterXSize(), geoTransform[3] };
        imageExtent.xmin = minPoint[0];
        imageExtent.ymin = minPoint[1];
        imageExtent.xmax = maxPoint[0];
        imageExtent.ymax = maxPoint[1];
        return imageExtent;
    }

    private static boolean tileInsideImage(Extent imageExtent, Extent tileExtent) {
        return Math.max(tileExtent.xmin, imageExtent.xmin) < Math.min(tileExtent.xmax, imageExtent.xmax) ||
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

    private static Dataset[] prepareDatasets(ProductDescriptor productDescriptor, Extent tileExtent) {
        final List<Dataset> gdalDatasets = new ArrayList<>();
        final Path[] productBandsPaths = productDescriptor.getBandPaths();
        final Path productPath = productDescriptor.getProductPath();
        
        // Optimization: Retrieve from cache once
        Triple<Extent, double[], LocalDateTime> triple = scaleCache.get(productPath);
        
        for (Path productBandsPath : productBandsPaths) {
            if (triple != null && !tileInsideImage(triple.getKeyOne(), tileExtent)) {
                continue; // Skip opening if we know it's out of bounds
            }

            final Dataset gdalDataset = GDAL.open(productBandsPath.toAbsolutePath().toString(), GDALConst.gaReadonly());
            if (gdalDataset == null) {
                throw new IllegalStateException("Fail to open Dataset for file: " + productBandsPath + ". " + GDAL.getLastErrorMsg());
            }
            if (!scaleCache.containsKey(productPath)) {
                scaleCache.put(productPath, new Triple<>(computeImageExtent(gdalDataset), new double[0], LocalDateTime.now()));
            }
            triple = scaleCache.get(productPath);
            if (tileInsideImage(triple.getKeyOne(), tileExtent)) {
                gdalDatasets.add(gdalDataset);
            } else {
                deleteDataset(gdalDataset, false);
            }
        }
        if (gdalDatasets.isEmpty()) {
            throw new IndexOutOfBoundsException("Tile out of bounds.");
        }

        return gdalDatasets.toArray(new Dataset[0]);
    }

    private static Dataset[] prepareDatasets2(ProductDescriptor productDescriptor) {
        final List<Dataset> gdalDatasets = new ArrayList<>();
        final Path[] productBandsPaths = productDescriptor.getBandPaths();
        final Path productPath = productDescriptor.getProductPath();
        for (Path productBandsPath : productBandsPaths) {
            final Dataset gdalDataset = GDAL.open(productBandsPath.toAbsolutePath().toString(), GDALConst.gaReadonly());
            if (gdalDataset == null) {
                throw new IllegalStateException("Fail to open Dataset for file: " + productBandsPath + ". " + GDAL.getLastErrorMsg());
            }
            gdalDatasets.add(gdalDataset);
        }
        if (gdalDatasets.isEmpty()) {
            throw new IndexOutOfBoundsException("Tile out of bounds.");
        }

        return gdalDatasets.toArray(new Dataset[0]);
    }

    private static Dataset buildVRTDataset(ProductDescriptor productDescriptor, Dataset[] gdalDatasets) {
        /*if (gdalDatasets.length == 1) {
            return gdalDatasets[0];
        }*/
        final Vector<String> vrtOptions = new Vector<>();
        FormatDescriptor formatDescriptor = productDescriptor.getFormatDescriptor();
        for (FormatDescriptor.Band band : formatDescriptor.getBands()) {
            vrtOptions.add("-b");
            vrtOptions.add(String.valueOf(band.getIndexInRaster()));
        }
        if (gdalDatasets.length > 1 && formatDescriptor.multiRaster()) {
            vrtOptions.add("-separate");
        }
        final String tempVrtFileName = "/vsimem/" + System.currentTimeMillis() + ".vrt";
        Dataset vrtDataset = GDAL.buildVRT(tempVrtFileName, gdalDatasets, new BuildVRTOptions(vrtOptions));
        if (vrtDataset == null) {
            throw new IllegalStateException("Fail to build VRT dataset. " + GDAL.getLastErrorMsg());
        }
        return vrtDataset;
    }

    private static Dataset buildWarpedDataset(ProductDescriptor productDescriptor, Extent tileExtent, Dataset gdalDataset, String tileFilePath) {
        final Vector<String> warpOptions = new Vector<>();
        warpOptions.add("-t_srs");
        warpOptions.add("EPSG:" + TileReader.config.tileGrid.projectionCode);
        warpOptions.add("-te");
        warpOptions.add(String.valueOf(tileExtent.xmin));
        warpOptions.add(String.valueOf(tileExtent.ymin));
        warpOptions.add(String.valueOf(tileExtent.xmax));
        warpOptions.add(String.valueOf(tileExtent.ymax));
        warpOptions.add("-ts");
        warpOptions.add(String.valueOf(TileReader.config.tileWidth));
        warpOptions.add(String.valueOf(TileReader.config.tileHeight));
        //warpOptions.add("-tap");
        if (productDescriptor.getNoDataValue() != null) {
            warpOptions.add("-srcnodata");
            warpOptions.add(String.valueOf(productDescriptor.getNoDataValue()));
            warpOptions.add("-dstnodata");
            warpOptions.add("0");
        }
        warpOptions.add("-multi");
        warpOptions.add("-wo");
        warpOptions.add("NUM_THREADS=ALL_CPUS");
        // Optimization: Increase warp memory and cache
        warpOptions.add("-wm");
        warpOptions.add("512"); // 512MB for warping
        if (gdalDataset.getRasterCount() == 1 || gdalDataset.getRasterCount() == 3) {
            warpOptions.add("-dstalpha");
        }
        final Dataset warpDataset = GDAL.warp(tileFilePath, new Dataset[]{gdalDataset}, new WarpOptions(warpOptions));
        if (warpDataset == null) {
            throw new IllegalStateException("Fail to warp dataset. " + GDAL.getLastErrorMsg());
        }
        return warpDataset;
    }

    private static Dataset buildWarpedDataset2(ProductDescriptor productDescriptor, Extent tileExtent, Dataset gdalDataset, String tileFilePath) {
        final Vector<String> warpOptions = new Vector<>();
        warpOptions.add("-t_srs");
        warpOptions.add("EPSG:4326");
        warpOptions.add("-te");
        warpOptions.add(String.valueOf(tileExtent.xmin));
        warpOptions.add(String.valueOf(tileExtent.ymin));
        warpOptions.add(String.valueOf(tileExtent.xmax));
        warpOptions.add(String.valueOf(tileExtent.ymax));
        //warpOptions.add("-tap");
        if (productDescriptor.getNoDataValue() != null) {
            warpOptions.add("-srcnodata");
            warpOptions.add(String.valueOf(productDescriptor.getNoDataValue()));
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

    private static Dataset buildRasterizedDataset(Path vectorFile, String raster, int bands) throws Exception {
        final List<String> args = new ArrayList<>();
        args.add("gdal_rasterize");
        args.add("-burn");
        for (int i = 1; i <= bands; i++) {
            args.add("32767");
        }
        args.add(vectorFile.toString());
        args.add(raster);
        Executor executor = Executor.create(ExecutorType.PROCESS, null, args);
        executor.execute(false);
        return GDAL.open(raster, GDALConst.gaReadonly());
    }

    private static Dataset buildTranslatedDataset(ProductDescriptor productDescriptor, Dataset warpDataset, String tileFilePath) {
        final Vector<String> translateOptions = new Vector<>();
        double min, max;
        translateOptions.add("-scale");
        final Band rasterBand = warpDataset.getRasterBand(1);
        final Path productPath = productDescriptor.getProductPath();
        final FormatDescriptor formatDescriptor = productDescriptor.getFormatDescriptor();
        if (formatDescriptor.getPriority() == 1 &&
                productDescriptor.getPixelMinScale() != null && productDescriptor.getPixelMaxScale() != null &&
                productDescriptor.getPixelMinScale() >= 0 && productDescriptor.getPixelMaxScale() > 0) {
            min = productDescriptor.getPixelMinScale();
            max = productDescriptor.getPixelMaxScale();
        } else {
            Triple<Extent, double[], LocalDateTime> triple = scaleCache.get(productPath);
            if (triple == null || triple.getKeyTwo().length == 0) {
                double[] vals = new double[2];
                double[] mins = new double[2], maxs = new double[2], means = new double[2], stddevs = new double[2];
                // Optimization: Use GetStatistics with approx_ok = true for much faster execution
                // If that's not available in your JNI wrapper, computeMinMax is used.
                int res = rasterBand.getStatistics(true, true, mins, maxs,  means, stddevs);
                if (res != 0) {
                    rasterBand.computeMinMax(vals);
                    min = vals[0];
                    max = vals[1];
                } else {
                    min = mins[0];
                    max = maxs[0];
                }
                triple = new Triple<>(triple == null ? computeImageExtent(warpDataset) : triple.getKeyOne(), new double[] { min, max }, LocalDateTime.now());
                scaleCache.put(productPath, triple);
            }
            min = triple.getKeyTwo()[0];
            max = triple.getKeyTwo()[1];
        }
        translateOptions.add(String.valueOf(min));
        translateOptions.add(String.valueOf(max));
        /*translateOptions.add("-a_scale");
        translateOptions.add("none");*/
        if (!GDALConstConstants.gdtByte().equals(rasterBand.getDataType())) {
            translateOptions.add("-ot");
            translateOptions.add("Byte");
        }
        // Apply any band transformation if expression is defined
        final List<FormatDescriptor.Band> bands = formatDescriptor.getBands();
        for (FormatDescriptor.Band band : bands) {
            String expression;
            if (!StringUtilities.isNullOrEmpty(expression = band.getExpression())) {
                try {
                    final Band gdalBand = warpDataset.getRasterBand(band.getIndexInRaster());
                    final int xSize = gdalBand.getXSize();
                    final int ySize = gdalBand.getYSize();
                    final int type = gdalBand.getDataType();
                    ByteBuffer buffer = null;
                    int size;
                    if (type == GDALConstConstants.gdtByte()) {
                        ExpressionEvaluator<Byte, Byte> evaluator = new ExpressionEvaluator<>(byte.class, byte.class);
                        buffer = gdalBand.readRasterByte(0, 0, xSize, ySize);
                        byte[] array;
                        // The underlying DirectByteBuffer is implementation-dependent and may not be backed by a Java array
                        if (buffer.hasArray()) {
                            array = buffer.array(); // Use array access if not direct for speed
                            for (int i = 0; i < array.length; i++) {
                                buffer.put(i, evaluator.evaluate(array[i], expression));
                            }
                        } else {
                            size = buffer.limit();
                            for (int i = 0; i < size; i++) {
                                buffer.put(i, evaluator.evaluate(buffer.get(i), expression));
                            }
                        }
                    } else if (type == GDALConstConstants.gdtUint16() || type == GDALConstConstants.gdtInt16()) {
                        ExpressionEvaluator<Short, Short> evaluator = new ExpressionEvaluator<>(short.class, short.class);
                        buffer = gdalBand.readRasterShort(0, 0, xSize, ySize);
                        final ShortBuffer sBuffer = buffer.asShortBuffer();
                        size = sBuffer.limit();
                        for (int i = 0; i < size; i++) {
                            sBuffer.put(i, evaluator.evaluate(sBuffer.get(i), expression));
                        }
                    } else if (type == GDALConstConstants.gdtUint32() || type == GDALConstConstants.gdtInt32()) {
                        ExpressionEvaluator<Integer, Integer> evaluator = new ExpressionEvaluator<>(int.class, int.class);
                        buffer = gdalBand.readRasterInt(0, 0, xSize, ySize);
                        final IntBuffer iBuffer = buffer.asIntBuffer();
                        size = iBuffer.limit();
                        for (int i = 0; i < size; i++) {
                            iBuffer.put(i, evaluator.evaluate(iBuffer.get(i), expression));
                        }
                    } else if (type == GDALConstConstants.gdtFloat32()) {
                        ExpressionEvaluator<Float, Float> evaluator = new ExpressionEvaluator<>(float.class, float.class);
                        buffer = gdalBand.readRasterFloat(0, 0, xSize, ySize);
                        final FloatBuffer fBuffer = buffer.asFloatBuffer();
                        size = fBuffer.limit();
                        for (int i = 0; i < size; i++) {
                            fBuffer.put(i, evaluator.evaluate(fBuffer.get(i), expression));
                        }
                    } else if (type == GDALConstConstants.gdtFloat64()) {
                        ExpressionEvaluator<Double, Double> evaluator = new ExpressionEvaluator<>(double.class, double.class);
                        buffer = gdalBand.readRasterDouble(0, 0, xSize, ySize);
                        final DoubleBuffer dBuffer = buffer.asDoubleBuffer();
                        size = dBuffer.limit();
                        for (int i = 0; i < size; i++) {
                            dBuffer.put(i, evaluator.evaluate(dBuffer.get(i), expression));
                        }
                    }
                    if (buffer != null) {
                        gdalBand.writeRasterDirect(0, 0, xSize, ySize, type, buffer);
                    }
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        }
        Dataset translateDataset = null;
        try {
            translateDataset = GDAL.translate(tileFilePath, warpDataset, new TranslateOptions(translateOptions));
        } catch (Throwable e) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            translateDataset = GDAL.translate(tileFilePath, warpDataset, new TranslateOptions(translateOptions));
        }
        if (translateDataset == null) {
            throw new IllegalStateException("Fail to translate dataset. " + GDAL.getLastErrorMsg());
        }
        return translateDataset;
    }
}
