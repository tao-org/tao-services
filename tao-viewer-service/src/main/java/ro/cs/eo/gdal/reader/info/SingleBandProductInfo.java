package ro.cs.eo.gdal.reader.info;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SingleBandProductInfo implements ProductInfo {

    private static final int[] BANDS_INDEXES = {1};
    private static final Integer PIXELS_MIN_SCALE = 0;
    private static final Integer PIXELS_MAX_SCALE = 5000;
    private static final Integer BANDS_NO_DATA_VALUE = -10000;

    private final String productName;
    private final String productPath;
    private final String[] productBandsPaths;

    public SingleBandProductInfo() {
        this("", "");
    }

    private SingleBandProductInfo(String productName, String productPath) {
        this.productName = productName;
        this.productPath = productPath;
        this.productBandsPaths = new String[]{productPath};
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
        return this.productBandsPaths;
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
        Path productBandFile = Paths.get(productPath);
        if (!Files.exists(productBandFile)) {
            throw new FileNotFoundException("Product path not exists.");
        }
        if (Files.isRegularFile(productBandFile)) {
            return new SingleBandProductInfo(productName, productPath);
        }
        return null;
    }
}
