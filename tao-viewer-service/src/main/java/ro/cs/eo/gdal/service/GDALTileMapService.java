package ro.cs.eo.gdal.service;

import org.springframework.stereotype.Service;
import ro.cs.eo.gdal.dataio.GDALTileCache;
import ro.cs.eo.gdal.dataio.Keys;
import ro.cs.eo.gdal.reader.ImageInfo;
import ro.cs.eo.gdal.reader.TileReader;
import ro.cs.eo.gdal.reader.info.ProductDescriptor;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.configuration.ConfigurationProvider;
import ro.cs.tao.utils.executors.MemoryUnit;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service("TileMap")
public class GDALTileMapService {

    private static final String regEx = "^(.*?)\\..*$";
    private final TileReader tileReader;

    private final GDALTileCache gdalTileCache;

    public GDALTileMapService() {
        ConfigurationProvider config = ConfigurationManager.getInstance();
        final boolean cacheEnabled = Boolean.parseBoolean(config.getValue(Keys.CONFIG_CACHE_ENABLED_KEY, "false"));
        final Path path = Paths.get(System.getProperty("user.home")).resolve(config.getValue(Keys.CONFIG_CACHE_DIR_KEY, ".eds/cache/"));
        final int size = Integer.parseInt(config.getValue(Keys.CONFIG_CACHE_SIZE_KEY, String.valueOf(MemoryUnit.KB.value())));
        final boolean cacheClearAtStartup = Boolean.parseBoolean(config.getValue(Keys.CONFIG_CACHE_CLEAR_AT_STARTUP_KEY, "false"));
        this.gdalTileCache = new GDALTileCache(cacheEnabled, path, size, cacheClearAtStartup);
        this.tileReader = new TileReader(this.gdalTileCache);
    }

    public void setCacheDirPath(Path cacheDirPath) {
        this.gdalTileCache.setCacheDirPath(cacheDirPath);
    }

    public byte[] getGDALTile(ProductDescriptor productDescriptor, byte z, int x, int y) throws FileNotFoundException {
        try {
            final Path sourceLocalFilePath = productDescriptor.getProductPath();
            if (Files.exists(sourceLocalFilePath)) {
                final String imageFileName = sourceLocalFilePath.getFileName().toString().replaceAll(regEx, "$1");
                Path tileFilePath = this.gdalTileCache.getTilePath(imageFileName, x, y, z, productDescriptor.getPixelMinScale(), productDescriptor.getPixelMaxScale());
                if (Files.notExists(tileFilePath)) {
                    this.tileReader.computeTile(productDescriptor, tileFilePath, x, y, z);
                }
                return Files.readAllBytes(tileFilePath);
            } else {
                throw new FileNotFoundException("Product path not exists.");
            }
        } catch (FileNotFoundException | IndexOutOfBoundsException e) {
            throw e;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to compute the tile for level " + z + " and (X,Y) (" + x + "," + y + ").", ex);
        }
    }

    public synchronized ImageInfo getGDALImageInfo(ProductDescriptor productDescriptor) throws FileNotFoundException {
        try {
            final Path sourceLocalFilePath = productDescriptor.getProductPath();
            if (!Files.exists(sourceLocalFilePath)) {
                throw new FileNotFoundException("Product path not exists.");
            }
            return this.tileReader.getInfo(productDescriptor);
        } catch (FileNotFoundException e) {
            throw e;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read info for the image.", ex);
        }
    }


}
