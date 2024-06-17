package ro.cs.eo.gdal.web;

import org.apache.commons.compress.utils.IOUtils;
import org.geotools.geometry.jts.WKTReader2;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.cs.eo.gdal.reader.Extent;
import ro.cs.eo.gdal.reader.ImageInfo;
import ro.cs.eo.gdal.reader.TileReader;
import ro.cs.eo.gdal.reader.info.FormatDescriptor;
import ro.cs.eo.gdal.reader.info.ProductDescriptor;
import ro.cs.eo.gdal.reader.info.ProductDescriptorFactory;
import ro.cs.eo.gdal.service.FileGeometryFilter;
import ro.cs.eo.gdal.service.GDALTileMapService;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.datasource.ProductHelperFactory;
import ro.cs.tao.eodata.util.ProductHelper;
import ro.cs.tao.persistence.RepositoryProvider;
import ro.cs.tao.services.commons.ControllerBase;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.spi.ServiceRegistry;
import ro.cs.tao.spi.ServiceRegistryManager;
import ro.cs.tao.utils.StringUtilities;
import ro.cs.tao.workspaces.Repository;
import ro.cs.tao.workspaces.RepositoryType;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/view")
public class ViewerController extends ControllerBase {

    private final AnnotationConfigApplicationContext context;
    private final Logger logger = Logger.getLogger(ViewerController.class.getName());

    @Autowired
    private RepositoryProvider repositoryProvider;

    public ViewerController() {
        this.context = new AnnotationConfigApplicationContext();
        this.context.scan("ro.cs.eo.gdal.service");
        this.context.refresh();
    }

    @RequestMapping(value = "/scale", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getScale(@RequestParam("file") String relativePath) {
        final Repository localRepository = getLocalRepository();
        final String path = localRepository.resolve(relativePath);
        return prepareResult(TileReader.getScale(Paths.get(path)));
    }

    @RequestMapping(value = "/scale", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> changeScale(@RequestParam("file") String relativePath,
                                                          @RequestParam("min") double min,
                                                          @RequestParam("max") double max) {
        final Repository localRepository = getLocalRepository();
        final String path = localRepository.resolve(relativePath);
        TileReader.setScale(Paths.get(path), new double[] { min, max });
        return prepareResult("New scale set", ResponseStatus.SUCCEEDED);
    }

    @RequestMapping(value = "/tile", method = RequestMethod.GET)
    public ResponseEntity<byte[]> viewTileData(@RequestParam("file") String relativePath,
                                               @RequestParam("z") byte z,
                                               @RequestParam("x") int x,
                                               @RequestParam("y") int y) {
        GDALTileMapService gdalTileMapService = (GDALTileMapService) context.getBean("TileMap");
        HttpHeaders headers = new HttpHeaders();
        try {
            final Repository localRepository = getLocalRepository();
            final String path = localRepository.resolve(relativePath);
            ProductDescriptor productDescriptor = ProductDescriptorFactory.getDescriptor(Paths.get(path));
            double[] scale = TileReader.getScale(Paths.get(path));
            if (scale != null && scale.length == 2) {
                FormatDescriptor formatDescriptor = productDescriptor.getFormatDescriptor();
                formatDescriptor.setPixelMinScale((int) scale[0]);
                formatDescriptor.setPixelMaxScale((int) scale[1]);
                //formatDescriptor.setNoDataValue(override.getKeyThree());
                formatDescriptor.setPriority(1);
            }
            byte[] data = gdalTileMapService.getGDALTile(productDescriptor, z, x, y);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE);
            headers.add(HttpHeaders.CACHE_CONTROL, "max-age=60");
            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (IndexOutOfBoundsException e) {
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE);
            return new ResponseEntity<>(("<html><body><h1>" + e.getMessage() + "</h1><br/><h3>Try again.</h3></body></html>").getBytes(), headers, HttpStatus.NOT_FOUND);
        } catch (FileNotFoundException e) {
            logger.warning(e.getMessage());
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE);
            return new ResponseEntity<>(("<html><body><h1>" + e.getMessage() + "</h1></body></html>").getBytes(), headers, HttpStatus.NOT_FOUND);
        } catch(IllegalArgumentException e){
            logger.warning(e.getMessage());
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE);
            return new ResponseEntity<>(("<html><body><h1>" + e.getMessage() + "</h1><br/><h3>Try another product.</h3></body></html>").getBytes(), headers, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception e) {
            logger.severe(e.getMessage());
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE);
            return new ResponseEntity<>(("<html><body><h1>500</h1><br/><h3>Internal server error.</h3></body></html>").getBytes(), headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "info", method = RequestMethod.GET)
    public ResponseEntity<String> viewTileInfo(@RequestParam("relativePath") String relativePath) {
        GDALTileMapService gdalTileMapService = (GDALTileMapService) context.getBean("TileMap");
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE);

        try {
            /*List<EOProduct> products = persistenceManager.rasterData().getProductsByNames(name);
            if (products == null || products.size() == 0) {
                throw new FileNotFoundException("Product not found.");
            }
            String path = products.get(0).getLocation();*/
            final Repository localRepository = getLocalRepository();
            final String path = localRepository.resolve(relativePath);
            final String name = relativePath.substring(relativePath.lastIndexOf('/') + 1);
            ProductDescriptor productDescriptor = ProductDescriptorFactory.getDescriptor(Paths.get(path));
            double[] scale = TileReader.getScale(Paths.get(path));
            if (scale != null && scale.length == 2) {
                FormatDescriptor formatDescriptor = productDescriptor.getFormatDescriptor();
                formatDescriptor.setPixelMinScale((int) scale[0]);
                formatDescriptor.setPixelMaxScale((int) scale[1]);
                //formatDescriptor.setNoDataValue(override.getKeyThree());
                formatDescriptor.setPriority(1);
            }
            ImageInfo imageInfo = gdalTileMapService.getGDALImageInfo(productDescriptor);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            return new ResponseEntity<>(imageInfo.toJson(), headers, HttpStatus.OK);
        } catch (FileNotFoundException e) {
            logger.warning(e.getMessage());
            return new ResponseEntity<>("<html><body><h1>" + e.getMessage() + "</h1></body></html>", headers, HttpStatus.NOT_FOUND);
        } catch (IndexOutOfBoundsException e) {
            logger.warning(e.getMessage());
            return new ResponseEntity<>("<html><body><h1>" + e.getMessage() + "</h1><br/><h3>Try again.</h3></body></html>", headers, HttpStatus.NOT_FOUND);
        } catch(IllegalArgumentException e){
            logger.warning(e.getMessage());
            return new ResponseEntity<>("<html><body><h1>" + e.getMessage() + "</h1><br/><h3>Try another product.</h3></body></html>", headers, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception e) {
            logger.severe(e.getMessage());
            return new ResponseEntity<>("<html><body><h1>500</h1><br/><h3>Internal server error.</h3></body></html>", headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "map", method = RequestMethod.GET)
    public ResponseEntity<byte[]> viewMap() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE);
        try (InputStream s = ViewerController.class.getResourceAsStream("view1.html")) {
            final byte[] data;
            data = s != null ? IOUtils.toByteArray(s) : new byte[0];
            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.severe(e.getMessage());
            return new ResponseEntity<>("<html><body><h1>500</h1><br/><h3>Internal server error.</h3></body></html>".getBytes(), headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "config", method = RequestMethod.POST)
    public ResponseEntity<String> setCacheDir(@RequestParam("cache-dir") String newCacheDir) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE);
        try {
            GDALTileMapService gdalTileMapService = (GDALTileMapService) context.getBean("TileMap");
            Path cacheDirpath = Paths.get(newCacheDir);
            if (Files.exists(cacheDirpath)) {
                gdalTileMapService.setCacheDirPath(cacheDirpath);
                return new ResponseEntity<>("<html><body><h1>OK</h1><br/><h3>Cache directory path updated successfully.</h3></body></html>", headers, HttpStatus.OK);
            }
            return new ResponseEntity<>("<html><body><h1>BAD</h1><br/><h3>Cache directory path not updated. Invalid path.</h3></body></html>", headers, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.severe(e.getMessage());
            return new ResponseEntity<>("<html><body><h1>500</h1><br/><h3>Internal server error.</h3></body></html>", headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/series", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> extractSeriesInfo(@RequestParam("id") String id,
                                                                @RequestParam("geometry") String geometryWKT) {
        GDALTileMapService gdalTileMapService = (GDALTileMapService) context.getBean("TileMap");
        try {
            final ServiceRegistry<FileGeometryFilter> registry = ServiceRegistryManager.getInstance().getServiceRegistry(FileGeometryFilter.class);
            final Set<FileGeometryFilter> services = registry.getServices();
            if (services.isEmpty()) {
                throw new Exception("Not implemented");
            }
            final String sitePath = ConfigurationManager.getInstance().getValue("site.path");
            if (sitePath == null || sitePath.isEmpty()) {
                throw new IOException("Cannot determine site path");
            }
            Path cachePath = Paths.get(sitePath).resolve("series");
            if (!Files.exists(cachePath)) {
                Files.createDirectories(cachePath);
            }
            final FileGeometryFilter filter = services.iterator().next();
            final Extent extent = filter.computeEnvelope(geometryWKT);
            final List<Path> files = filter.filter(geometryWKT);
            files.sort(new Comparator<Path>() {
                @Override
                public int compare(Path o1, Path o2) {
                    final ProductHelper h1 = ProductHelperFactory.getHelper(o1.getFileName().toString());
                    final ProductHelper h2 = ProductHelperFactory.getHelper(o2.getFileName().toString());
                    if (h1 == null || h1.getSensingDate() == null) return 1;
                    if (h2 == null || h2.getSensingDate() == null) return -1;
                    return h1.getSensingDate().compareTo(h2.getSensingDate());
                }
            });
            final Map<String, String> innerMap = new LinkedHashMap<>();
            for (Path file : files) {
                final ProductHelper helper = ProductHelperFactory.getHelper(file.getFileName().toString());
                innerMap.put("date", helper != null ? helper.getSensingDate() : null);
                innerMap.put("image", file.toString());
            }
            return prepareResult(innerMap);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/series/images", method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> extractSeries(@RequestParam("id") String id,
                                                            @RequestParam("geometry") String geometryWKT,
                                                            @RequestParam("files") List<String> files) {
        GDALTileMapService gdalTileMapService = (GDALTileMapService) context.getBean("TileMap");
        try {
            if (files == null || files.isEmpty()) {
                throw new IllegalArgumentException("[files] cannot be empty");
            }
            if (StringUtilities.isNullOrEmpty(id)) {
                throw new IllegalArgumentException("[id] cannot be empty");
            }
            final String sitePath = ConfigurationManager.getInstance().getValue("site.path");
            if (sitePath == null || sitePath.isEmpty()) {
                throw new IOException("Cannot determine site path");
            }
            Path cachePath = Paths.get(sitePath).resolve("series");
            if (!Files.exists(cachePath)) {
                Files.createDirectories(cachePath);
            }
            List<Path> paths = files.stream().map(Paths::get).collect(Collectors.toList());
            final Matcher matcher = Pattern.compile("_T(\\d{2}\\w{3})_").matcher(files.get(0));
            final String utmZone;
            if (matcher.find()) {
                utmZone = matcher.group(1);
            } else {
                utmZone = null;
            }
            final ServiceRegistry<FileGeometryFilter> registry = ServiceRegistryManager.getInstance().getServiceRegistry(FileGeometryFilter.class);
            final Set<FileGeometryFilter> services = registry.getServices();
            final FileGeometryFilter filter;
            if (services.isEmpty()) {
                //throw new Exception("Not implemented");
                filter = new FileGeometryFilter() {

                    private int getEpsgCode() {
                        return 32600 + (utmZone.charAt(2) >= 70 ? 100 : 0) + Integer.parseInt(utmZone.substring(0, 2));
                    }

                    @Override
                    public Extent computeEnvelope(String geometryWKT) {
                        Extent envelope = null;
                        WKTReader2 reader = new WKTReader2();
                        try {
                            final Geometry geometry = reader.read(geometryWKT);
                            Geometry env = geometry.getEnvelope();
                            /*env = JTS.transform(env, CRS.findMathTransform(CRS.decode("EPSG:4326", true),
                                                                           CRS.decode("EPSG:" + getEpsgCode(), true)));*/
                            final Envelope internal = env.buffer(0.01).getEnvelopeInternal();
                            envelope = new Extent();
                            envelope.xmin = internal.getMinX();
                            envelope.ymin = internal.getMinY();
                            envelope.xmax = internal.getMaxX();
                            envelope.ymax = internal.getMaxY();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        return envelope;
                    }

                    @Override
                    public List<Path> filter(String geometryWKT) {
                        return new ArrayList<>();
                    }
                };
            } else {
                filter = services.iterator().next();
            }
            final Extent extent = filter.computeEnvelope(geometryWKT);
            // Convert polygon to multilinestring so that vector will not be filled when rasterized
            geometryWKT = geometryWKT.replace("POLYGON", "MULTILINESTRING");
            final List<String> outFiles = new ArrayList<>();
            for (Path file : paths) {
                try {
                    ProductDescriptor productDescriptor = getCompositeDescriptor(file);
                    FormatDescriptor formatDescriptor = productDescriptor.getFormatDescriptor();
                    formatDescriptor.setPriority(1);
                    outFiles.add("/series/" + id + "/" + gdalTileMapService.extractSubset(productDescriptor, extent, geometryWKT, id));
                } catch (Exception e) {
                    warn(e.getMessage());
                }
            }
            return prepareResult(outFiles);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    private ProductDescriptor getCompositeDescriptor(Path file) throws IOException {
        final String bandPatterns;
        final String namePattern;
        final String subfolderPattern;
        if (Boolean.parseBoolean(ConfigurationManager.getInstance().getValue("downloader.use.esa.l2a", "true"))) {
            bandPatterns = ConfigurationManager.getInstance().getValue("series.composite.band.patterns", "B04_10m,B03_10m,B02_10m");
            namePattern = "S2[ABCD]_MSIL2A_\\d{8}T\\d{6}_N\\d{4}_R\\d{3}_T\\d{2}\\w{3}_\\d{8}T\\d{6}.SAFE";
            subfolderPattern = null;
        } else {
            bandPatterns = ConfigurationManager.getInstance().getValue("series.composite.band.patterns", "FRE_B4,FRE_B3,FRE_B2");
            namePattern = "S2[ABCD]_MSIL2A_(\\d{8})T\\d{6}_N\\d{4}_R\\d{3}_T(\\d{2}\\w{3})_\\d{8}T\\d{6}.SAFE";
            subfolderPattern = "SENTINEL2[A-D]_\\d{8}-\\d{6}-\\d{3}_L2A_T\\d{2}[A-Z]{3}.*";
        }
        final List<FormatDescriptor.Band> bands = Arrays.stream(bandPatterns.split(",")).map(p -> {
            final FormatDescriptor.Band band = new FormatDescriptor.Band();
            band.setPattern(p);
            band.setIndexInRaster(1);
            return band;
        }).collect(Collectors.toList());
        FormatDescriptor formatDescriptor = new FormatDescriptor();
        formatDescriptor.setBands(bands);
        formatDescriptor.setPriority(1);
        formatDescriptor.setPixelMinScale(0);
        formatDescriptor.setPixelMaxScale(8191);
        formatDescriptor.setNoDataValue(-1000);
        formatDescriptor.setNamePatterns(new ArrayList<>() {{ add(namePattern); }});
        formatDescriptor.setBands(bands);
        return new ProductDescriptor(formatDescriptor, file);
    }

    private Repository getLocalRepository() {
        return repositoryProvider.getUserSystemRepositories(currentUser()).stream().filter(w -> w.getType() == RepositoryType.LOCAL).findFirst().get();
    }
}