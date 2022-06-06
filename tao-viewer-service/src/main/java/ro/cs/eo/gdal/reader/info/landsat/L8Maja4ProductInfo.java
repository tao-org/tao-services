package ro.cs.eo.gdal.reader.info.landsat;

import ro.cs.eo.gdal.reader.info.ProductInfo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class L8Maja4ProductInfo implements ProductInfo {

    private static final String[] PRODUCT_NAME_PREFIXES = {"LC08_L2A"};
    private static final String PRODUCT_DIR_PREFIX = "LANDSAT8";
    private static final String[] BANDS_SUFFIXES = {"FRE_B4", "FRE_B3", "FRE_B2"};
    private static final int[] BANDS_INDEXES = {1};
    private static final Integer PIXELS_MIN_SCALE = 0;
    private static final Integer PIXELS_MAX_SCALE = 7;
    private static final Integer BANDS_NO_DATA_VALUE = -10000;
    private final String productName;
    private final String productPath;
    private final String[] productBandsPaths = new String[BANDS_SUFFIXES.length];

    public L8Maja4ProductInfo() {
        this("","");
    }

    private L8Maja4ProductInfo(String productName, String productPath) {
        this.productName = productName;
        this.productPath = productPath;
    }

    @Override
    public String getProductName() {
        return this.productName;
    }

    @Override
    public String getProductPath() {
        return this.productPath;
    }

    @Override
    public String[] getProductBandsPaths() {
        if (productBandsPaths[0] != null) {
            return productBandsPaths;
        }
        Path productBandsFilesDirPath = Paths.get(this.productPath);
        try (Stream<Path> dirs = Files.list(productBandsFilesDirPath).filter(Files::isDirectory)) {
            dirs.forEach(dir -> {
                try (Stream<Path> files = Files.list(dir).filter(Files::isRegularFile)) {
                    files.forEach(file -> {
                        try {
                            String contentType = Files.probeContentType(file);
                            if (contentType != null && contentType.contains("image")) {
                                for (int suffixIndex = 0; suffixIndex < BANDS_SUFFIXES.length; suffixIndex++) {
                                    if (productBandsPaths[suffixIndex] == null && file.getFileName().toString().contains(BANDS_SUFFIXES[suffixIndex])) {
                                        productBandsPaths[suffixIndex] = file.toString();
                                    }
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return productBandsPaths;
    }

    @Override
    public int[] getProductBandsIndexes() {
        return BANDS_INDEXES;
    }

    @Override
    public Integer getProductBandsPixelsMinScale() {
        return PIXELS_MIN_SCALE;
    }

    @Override
    public Integer getProductBandsPixelsMaxScale() {
        return PIXELS_MAX_SCALE;
    }

    @Override
    public Integer getProductBandsNoDataValue() {
        return BANDS_NO_DATA_VALUE;
    }

    @Override
    public ProductInfo getInstance(String productName, String productPath) throws FileNotFoundException {
        Path productBandsFilesDirPath = Paths.get(productPath);
        if (!Files.exists(productBandsFilesDirPath)) {
            throw new FileNotFoundException("Product path not exists.");
        }
        if (Files.isDirectory(productBandsFilesDirPath)) {
            try (Stream<Path> allDirs = Files.list(productBandsFilesDirPath).filter(Files::isDirectory)) {
                Path[] dirs = allDirs.toArray(Path[]::new);
                if (dirs.length > 0 && dirs[0].getFileName().toString().contains(PRODUCT_DIR_PREFIX)) {
                    for (String productNamePrefix : PRODUCT_NAME_PREFIXES) {
                        if (productName.startsWith(productNamePrefix)) {
                            return new L8Maja4ProductInfo(productName, productPath);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
