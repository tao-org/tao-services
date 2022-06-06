package ro.cs.eo.gdal.reader.info;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

public interface ProductInfo {

    ConcurrentHashMap<String, ProductInfo> PRODUCTS_INFO = new ConcurrentHashMap<>();

    static ProductInfo getProductInfoInstance(String productName, String productPath) throws FileNotFoundException {
        final ServiceLoader<ProductInfo> productInfoClasses = ServiceLoader.load(ProductInfo.class);
        final List<ProductInfo> matches = new ArrayList<>();
        for (ProductInfo productInfoClass : productInfoClasses) {
            final ProductInfo productInfoInstance = productInfoClass.getInstance(productName, productPath);
            if (productInfoInstance != null) {
                matches.add(productInfoInstance);
            }
        }
        if (matches.size() > 0) {
            return PRODUCTS_INFO.computeIfAbsent(productName,
                                                 k -> matches.stream().sorted((o1, o2) -> Integer.compare(o2.priority(), o1.priority()))
                                                             .findFirst().get());
        }
        throw new IllegalArgumentException("Unknown product");
    }

    default int priority() { return 1; }

    default boolean hasTiles() { return false; }

    String getProductName();

    String getProductPath();

    String[] getProductBandsPaths();

    int[] getProductBandsIndexes();

    Integer getProductBandsPixelsMinScale();

    Integer getProductBandsPixelsMaxScale();

    Integer getProductBandsNoDataValue();

    ProductInfo getInstance(String productName, String productPath) throws FileNotFoundException;

    default void setProductBandsPixelMinScale(int value) { }

    default void setProductBandsPixelMaxScale(int value) { }

    default boolean isMultiBandProduct() { return true; }
}
