package ro.cs.eo.gdal.reader.info.sentinel;

import ro.cs.eo.gdal.reader.info.ProductInfo;
import ro.cs.tao.utils.Triple;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Sen2AgriL3BProductInfo implements ProductInfo {
    private static final Pattern[] namePatterns = {Pattern.compile("S2AGRI_L3B_PRD_S\\d{1,3}_\\d{8}T\\d{6}_A\\d{8}T\\d{6}"),
                                Pattern.compile("S2AGRI_L3A_PRD_S\\d{1,3}_\\d{8}T\\d{6}_V\\d{8}_\\d{8}"),
                                Pattern.compile("S2AGRI_(L3S1COMP|L3BICOMP)_PRD_S\\d{1,3}_\\d{8}T\\d{6}_V\\d{8}T\\d{6}_\\d{8}T\\d{6}_(AMP|NDVI|LAI|FAPAR|FCOVER)(_(VV|VH))?")};
    private static final Pattern[] rasterPatterns = {Pattern.compile("S2AGRI_L3B_S(.*)(?:MONO)?_A\\d{8}T\\d{6}_T(.*)\\.TIF"),
            Pattern.compile("S2AGRI_L3A_S(RFL)_V\\d{8}_\\d{8}_T.*_10M\\.TIF"),
            Pattern.compile("S2AGRI_(?:L3S1COMP|L3BICOMP)_V\\d{8}T\\d{6}_\\d{8}T\\d{6}_T(?:.*)_(AMP|NDVI|LAI|FAPAR|FCOVER)(?:_(?:VV|VH))?\\.TIF")};
    private static final String[] RASTER_PRIORITY_ORDER = {"NDVI", "LAI", "FAPAR", "FCOVER", "NDWI", "BRIGHT", "AMP", "RFL"};
    private static final Map<String, Triple<Integer, Integer, Integer>> mapRasterLimitsInfos = new HashMap<String, Triple<Integer, Integer, Integer>>()
    {{
        put("NDVI", new Triple<>(0, 1000, -10000));
        put("LAI", new Triple<>(0, 8000, -10000));
        put("FAPAR", new Triple<>(0, 1000, -10000));
        put("FCOVER", new Triple<>(0, 1000, -10000));
        put("NDWI", new Triple<>(-1000, 1000, -10000));
        put("BRIGHT", new Triple<>(0, 5000, -10000));
        put("AMP", new Triple<>(0, 100, 0));
        put("RFL", new Triple<>(0, 10000, -10000));
    }};
    private static final Map<String, int[]> mapRasterBandsInfos = new HashMap<String, int[]>()
    {{
        put("NDVI", new int[]{1});
        put("LAI", new int[]{1});
        put("FAPAR", new int[]{1});
        put("FCOVER", new int[]{1});
        put("NDWI", new int[]{1});
        put("BRIGHT", new int[]{1});
        put("AMP", new int[]{1});
        put("RFL", new int[]{3,2,1});
    }};

    private boolean isInitialized;
    private int[] bandsIndexes = {1};
    private Integer pixelsMinScale = 0;
    private Integer pixelsMaxScale = 1000;
    private Integer bandsNoDataValue = -10000;

    private final String productName;
    private final String productPath;
    private String[] productBandsPaths;

    public Sen2AgriL3BProductInfo() {
        this("","");
    }

    private Sen2AgriL3BProductInfo(String productName, String productPath) {
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
        updateProductBandInfos();
        return productBandsPaths;
    }

    @Override
    public int[] getProductBandsIndexes() {
        updateProductBandInfos();
        return bandsIndexes;
    }

    @Override
    public Integer getProductBandsPixelsMinScale() {
        updateProductBandInfos();
        return pixelsMinScale;
    }

    @Override
    public Integer getProductBandsPixelsMaxScale() {
        updateProductBandInfos();
        return pixelsMaxScale;
    }

    @Override
    public Integer getProductBandsNoDataValue() {
        updateProductBandInfos();
        return bandsNoDataValue;
    }

    @Override
    public ProductInfo getInstance(String productName, String productPath) throws FileNotFoundException {
        Path productBandsFilesDirPath = Paths.get(productPath);
        boolean exists = Files.exists(productBandsFilesDirPath);
        boolean isDir = Files.isDirectory(productBandsFilesDirPath);
        for (Pattern pattern : namePatterns) {
            if (pattern.matcher(productName).find()) {
                if (!exists) {
                    throw new FileNotFoundException("Product path not exists.");
                }
                if (isDir) {
                    return new Sen2AgriL3BProductInfo(productName, productPath);
                }
            }
        }
        return null;
    }

    private void updateProductBandInfos() {
        if (isInitialized) {
            return;
        }
        Path productBandsFilesDirPath = Paths.get(this.productPath).resolve("TILES");
        try (Stream<Path> dirs = Files.list(productBandsFilesDirPath).filter(Files::isDirectory)) {
            // use a tree map for having as much as possible a predictable biophysical indicator order
            final Map<String, List<String>> rasters = new HashMap<>();
            dirs.forEach(dir -> {
                try (Stream<Path> files = Files.list(dir.resolve("IMG_DATA")).filter(Files::isRegularFile)) {
                    files.forEach(file -> {
                        for (Pattern pattern : rasterPatterns) {
                            Matcher matcher = pattern.matcher(file.getFileName().toString());
                            if (matcher.find()) {
                                rasters.computeIfAbsent(matcher.group(1), k -> new ArrayList<>()).add(file.toString());
                                break;
                            }
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            if (!rasters.isEmpty()) {
                for (String raster : RASTER_PRIORITY_ORDER) {
                    if (rasters.containsKey(raster)) {
                        productBandsPaths = rasters.get(raster).stream().toArray(String[]::new);
                        Triple<Integer, Integer, Integer> rasterInfos = mapRasterLimitsInfos.get(raster);
                        pixelsMinScale = rasterInfos.getKeyOne();
                        pixelsMaxScale = rasterInfos.getKeyTwo();
                        bandsNoDataValue = rasterInfos.getKeyThree();
                        bandsIndexes = mapRasterBandsInfos.get(raster);
                        isInitialized = true;
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isMultiBandProduct() {
        return false;
    }
}
