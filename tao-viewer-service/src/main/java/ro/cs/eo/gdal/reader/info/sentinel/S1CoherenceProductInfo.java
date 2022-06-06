package ro.cs.eo.gdal.reader.info.sentinel;

import ro.cs.eo.gdal.reader.info.ProductInfo;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public class S1CoherenceProductInfo implements ProductInfo {

    private static final Pattern namePattern = Pattern.compile("SEN4CAP_L2A_S\\d{1,3}_V\\d{8}T\\d{6}_\\d{8}T\\d{6}_V[HV]_\\d{3}_(S1_L2A_)?(\\d{2}\\w{3}_)?COHE|S1A_L2_COH_\\d{8}T\\d{6}_(\\d{8}T\\d{6}_)?V[HV]_\\d{3}_\\d{2}\\w{3}");
    private static final int[] BANDS_INDEXES = {0};
    private static final Integer PIXELS_MIN_SCALE = 0;
    private static final Integer PIXELS_MAX_SCALE = 1;
    private static final Integer BANDS_NO_DATA_VALUE = 0;

    private final String productName;
    private final String productPath;
    private final String[] productBandsPaths;

    public S1CoherenceProductInfo() {
        this("", "");
    }

    private S1CoherenceProductInfo(String productName, String productPath) {
        this.productName = productName;
        this.productPath = productPath;
        this.productBandsPaths = new String[]{productPath};
    }

    @Override
    public int priority() { return 2; }

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
        if (namePattern.matcher(productName).find()) {
            final Path productBandFile = Paths.get(productPath);
            if (!Files.exists(productBandFile)) {
                throw new FileNotFoundException("Product path not exists.");
            }
            if (Files.isRegularFile(productBandFile)) {
                return new S1CoherenceProductInfo(productName, productPath);
            }
        }
        return null;
    }
}

