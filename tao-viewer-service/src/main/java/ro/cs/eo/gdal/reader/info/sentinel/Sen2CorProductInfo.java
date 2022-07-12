package ro.cs.eo.gdal.reader.info.sentinel;

import ro.cs.eo.gdal.reader.info.ProductInfo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Sen2CorProductInfo implements ProductInfo {

    private static final Pattern namePattern = Pattern.compile("S2[ABCD]_MSIL2A_\\d{8}T\\d{6}_N\\d{4}_R\\d{3}_T\\d{2}\\w{3}_\\d{8}T\\d{6}.SAFE");
    private static final String[] BANDS_SUFFIXES = {"B02_10m", "B03_10m", "B04_10m"};
    private static final int[] BANDS_INDEXES = {1};
    private static final Integer PIXELS_MIN_SCALE = 0;
    private static final Integer PIXELS_MAX_SCALE = 32767;
    private static final Integer BANDS_NO_DATA_VALUE = 0;

    private final String productName;
    private final String productPath;
    private final String[] productBandsPaths = new String[BANDS_SUFFIXES.length];

    public Sen2CorProductInfo() {
        this("","");
    }

    private Sen2CorProductInfo(String productName, String productPath) {
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
        Path productBandsFilesDirPath = Paths.get(this.productPath).resolve("GRANULE");
        try (Stream<Path> dirs = Files.list(productBandsFilesDirPath).filter(Files::isDirectory)) {
            dirs.forEach(dir -> {
                try (Stream<Path> files = Files.list(dir.resolve("IMG_DATA").resolve("R10m")).filter(Files::isRegularFile)) {
                    files.forEach(file -> {
                        for (int suffixIndex = 0; suffixIndex < BANDS_SUFFIXES.length; suffixIndex++) {
                            if (productBandsPaths[suffixIndex] == null && file.getFileName().toString().contains(BANDS_SUFFIXES[suffixIndex])) {
                                productBandsPaths[suffixIndex] = file.toString();
                            }
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
        if(namePattern.matcher(productName).find()) {
            Path productBandsFilesDirPath = Paths.get(productPath);
            if (!Files.exists(productBandsFilesDirPath)) {
                throw new FileNotFoundException("Product path not exists.");
            }
            if (Files.exists(productBandsFilesDirPath.resolve("MTD_MSIL2A.xml"))) {
                return new Sen2CorProductInfo(productName, productPath);
            }
        }
        return null;
    }
}
