package ro.cs.eo.gdal.service;

import org.springframework.stereotype.Service;
import ro.cs.eo.gdal.dataio.Keys;
import ro.cs.eo.gdal.dataio.drivers.Dataset;
import ro.cs.eo.gdal.dataio.drivers.GDAL;
import ro.cs.eo.gdal.dataio.drivers.VectorTranslateOptions;
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.configuration.ConfigurationProvider;
import ro.cs.tao.utils.FileUtilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Vector;

/** Produces a GeoJSON tile by reading only features intersecting the requested area. */
@Service
public class VectorTileService {
    private static final int MAX_TILE_BYTES = 8 * 1024 * 1024;
    private static final double MAX_LATITUDE = 85.0511287798066;
    private final boolean cacheEnabled;
    private final Path cachePath;

    public VectorTileService() {
        ConfigurationProvider config = ConfigurationManager.getInstance();
        this.cacheEnabled = Boolean.parseBoolean(config.getValue(Keys.CONFIG_CACHE_ENABLED_KEY, "false"));
        this.cachePath = Path.of(SystemVariable.ROOT.value(), "cache");
        if (this.cacheEnabled) {
            try {
                FileUtilities.ensureExists(this.cachePath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public byte[] getTile(Path file, int z, int x, int y) throws IOException {
        if (z < 0 || z > 22 || x < 0 || y < 0 || x >= (1L << z) || y >= (1L << z)) {
            throw new IllegalArgumentException("Invalid tile coordinates");
        }
        String name = file.getFileName().toString().toLowerCase();
        if (!(name.endsWith(".shp") || name.endsWith(".kml") || name.endsWith(".geojson") || name.endsWith(".json"))) {
            throw new IllegalArgumentException("Unsupported vector format");
        }
        if (!Files.isRegularFile(file)) {
            throw new java.io.FileNotFoundException(file.toString());
        }
        byte[] bytes;
        Path outputFile = this.cachePath.resolve(name + ".v2." + x + "." + y + "." + z);
        if (this.cacheEnabled) {
            if (Files.exists(outputFile)) {
                return Files.readAllBytes(outputFile);
            }
        }
        final double west = longitude(x, z);
        final double east = longitude(x + 1L, z);
        final double north = latitude(y, z);
        final double south = latitude(y + 1L, z);
        final Vector<String> options = new Vector<>();
        options.add("-f"); options.add("GeoJSON");
        options.add("-t_srs"); options.add("EPSG:4326");
        options.add("-spat_srs"); options.add("EPSG:4326");
        options.add("-spat");
        options.add(Double.toString(west)); options.add(Double.toString(south));
        options.add(Double.toString(east)); options.add(Double.toString(north));
        options.add("-nln"); options.add("tile");
        options.add("-append");

        Dataset source = null;
        Dataset result = null;
        try {
            GDAL.useExceptions();
            source = GDAL.openVector(file.toAbsolutePath().toString());
            if (source == null) {
                throw new IOException("GDAL could not read vector file: " + GDAL.getLastErrorMsg());
            }
            result = GDAL.vectorTranslate(outputFile.toString(), source, new VectorTranslateOptions(options));
            if (result == null) {
                throw new IOException("GDAL could not create vector tile: " + GDAL.getLastErrorMsg());
            }
            result.delete();
            result = null;
            if (Files.size(outputFile) > MAX_TILE_BYTES) {
                throw new IOException("Vector tile exceeds 8 MB; request a higher zoom level");
            }
            return Files.readAllBytes(outputFile);
        } finally {
            if (result != null) result.delete();
            if (source != null) source.delete();
            if (!this.cacheEnabled) {
                Files.deleteIfExists(outputFile);
            }
        }
    }

    private static double longitude(long x, int z) {
        return x * 360.0 / (1L << z) - 180.0;
    }

    private static double latitude(long y, int z) {
        final double radians = Math.atan(Math.sinh(Math.PI * (1 - 2.0 * y / (1L << z))));
        return Math.clamp(Math.toDegrees(radians), -MAX_LATITUDE, MAX_LATITUDE);
    }
}
