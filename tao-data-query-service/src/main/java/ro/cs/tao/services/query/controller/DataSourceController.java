/*
 * Copyright (C) 2018 CS ROMANIA
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package ro.cs.tao.services.query.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.component.Variable;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.datasource.DataQuery;
import ro.cs.tao.datasource.DataSource;
import ro.cs.tao.datasource.DataSourceCredentials;
import ro.cs.tao.datasource.DataSourceManager;
import ro.cs.tao.datasource.beans.Query;
import ro.cs.tao.datasource.param.*;
import ro.cs.tao.eodata.EOData;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.eodata.Polygon2D;
import ro.cs.tao.eodata.metadata.DecodeStatus;
import ro.cs.tao.eodata.metadata.MetadataInspector;
import ro.cs.tao.messaging.Messaging;
import ro.cs.tao.messaging.Topic;
import ro.cs.tao.persistence.RepositoryProvider;
import ro.cs.tao.persistence.SiteProvider;
import ro.cs.tao.products.landsat.Landsat8TileExtent;
import ro.cs.tao.products.sentinels.Sentinel2TileExtent;
import ro.cs.tao.serialization.GeometryAdapter;
import ro.cs.tao.serialization.SerializationException;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.factory.StorageServiceFactory;
import ro.cs.tao.services.interfaces.DataSourceComponentService;
import ro.cs.tao.services.interfaces.DataSourceService;
import ro.cs.tao.services.interfaces.StorageService;
import ro.cs.tao.services.model.datasource.DataSourceDescriptor;
import ro.cs.tao.services.query.beans.FetchRequest;
import ro.cs.tao.services.query.beans.Filter;
import ro.cs.tao.services.query.beans.TileRequest;
import ro.cs.tao.spi.ServiceRegistryManager;
import ro.cs.tao.utils.FileUtilities;
import ro.cs.tao.utils.StringUtilities;
import ro.cs.tao.utils.executors.NamedThreadPoolExecutor;
import ro.cs.tao.workspaces.Repository;
import ro.cs.tao.workspaces.RepositoryType;
import ro.cs.tao.workspaces.Site;

import java.awt.geom.Path2D;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Cosmin Cara
 */
@RestController
@RequestMapping("/query")
@Tag(name = "Product search", description = "Operations related to product search and download")
public class DataSourceController extends BaseController {

    private static final Pattern s2TilePattern = Pattern.compile("\\d{2}\\w{3}");
    private static final Pattern l8TilePattern = Pattern.compile("\\d{6}");
    //private static final List<DataSourceDescriptorBean> cachedDataSources = new ArrayList<>();
    private static Geometry aoiGeometry;

    @Autowired
    private DataSourceService dataSourceService;
    @Autowired
    private DataSourceComponentService dataSourceComponentService;
    @Autowired
    private RepositoryProvider repositoryProvider;
    @Autowired
    private SiteProvider siteProvider;

    /**
     * Lists the available data sources
     */
    @RequestMapping(value = "/", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getRegisteredSources() {
        //if (cachedDataSources.isEmpty()) {
        //    synchronized (cachedDataSources) {
                List<DataSourceDescriptor> instances = dataSourceService.getDatasourceInstances();
                if (instances == null) {
                    instances = new ArrayList<>();
                }
        //        cachedDataSources.addAll(instances.stream().map(DataSourceDescriptorBean::new).collect(Collectors.toList()));
                return prepareResult(instances.stream().map(DataSourceDescriptorBean::new).collect(Collectors.toList()));
        //    }
        //}
        //return prepareResult(cachedDataSources, 60);
    }

    /**
     * Lists the short description of the available data sources
     */
    @RequestMapping(value = "/summary", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getRegisteredSourcesSummary() {
        List<DataSourceDescriptor> instances = dataSourceService.getDatasourceInstances();
        if (instances == null) {
            instances = new ArrayList<>();
        }
        return prepareResult(instances.stream().map(DataSourceSummaryBean::new).collect(Collectors.toList()));
    }

    /**
     * Lists the registered providers (e.g. USGS) and the collections available from them
     */
    @RequestMapping(value = "/providers", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getProviders() {
        Map<String, String[]> providers = dataSourceService.getDataSourceProviders();
        if (providers == null) {
            providers = new HashMap<>();
        }
        return prepareResult(providers);
    }

    /**
     * Lists the registered sensors (or collections)
     */
    @RequestMapping(value = "/sensor/", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getSupportedSensors() {
        Set<String> sensors = dataSourceService.getSupportedSensors();
        if (sensors == null) {
            sensors = new HashSet<>();
        }
        return prepareResult(sensors);
    }

    /**
     * Returns any defined filter for data sources
     */
    @RequestMapping(value = "/filter/", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getFilter() {
        final String prefix = "datasource.filter.";
        final Map<String, String> values = ConfigurationManager.getInstance().getValues(prefix);
        Filter filter;
        if (values != null) {
            filter = new Filter(values.get(prefix + "category"), values.get(prefix + "collection"), values.get(prefix + "provider"));
        } else {
            filter = new Filter(null, null, null);
        }
        return prepareResult(filter);
    }

    /**
     * Lists the data sources that provide the given sensor or collection
     * @param sensorName    The sensor (or collection) name
     */
    @RequestMapping(value = "/sensor/{name}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getDataSourcesForSensor(@PathVariable("name") String sensorName) {
        List<String> sources = dataSourceService.getDatasourcesForSensor(sensorName);
        if (sources == null) {
            sources = new ArrayList<>();
        }
        return prepareResult(sources);
    }

    /**
     * Returns the parameters supported for the given sensor (or collection) by the given data source
     * @param sensorName        The sensor (or collection) name
     * @param dataSourceName   The name of the data source
     */
    @RequestMapping(value = "/sensor/{name}/{source:.+}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getSupportedParameters(@PathVariable("name") String sensorName,
                                                                     @PathVariable("source") String dataSourceName) {
        List<DataSourceParameter> params = dataSourceService.getSupportedParameters(sensorName, dataSourceName);
        DataSourceCredentials dsCreds = dataSourceService.getDataSourceCredentials(dataSourceName);
        if (params == null) {
            params = new ArrayList<>();
        }
        int count = params.size();
        for (int i = 0; i < count; i++) {
            params.get(i).setOrder(i + 1);
        }
        return prepareResult(new DataSourceInfoBean(params.stream().map(DataSourceParameterBean::new).collect(Collectors.toList()),dsCreds));
    }

    /**
     * Returns the WKT footprint (in WGS84) of the given tile product.
     * @param satellite The satellite. Supported values are 'sentinel2' and 'landsat8'
     * @param tile  The tile identifier (for Landsat-8, this is the path and row)
     */
    @RequestMapping(value = "/footprint", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getTileFootprint(@RequestParam("satellite") String satellite, @RequestParam("tile") String tile) {
        try {
            if (StringUtilities.isNullOrEmpty(satellite)) {
                throw new IllegalArgumentException("Empty parameter [satellite]");
            }
            if (StringUtilities.isNullOrEmpty(tile)) {
                throw new IllegalArgumentException("Empty parameter [tile]");
            }
            final List<String> tiles = new ArrayList<>();
            if (tile.indexOf(',') > 0) { // we have a list of tiles
                tiles.addAll(Arrays.asList(tile.split(",")));
            } else {
                tiles.add(tile);
            }
            final WKTReader reader = new WKTReader();
            Geometry geometry = null;
            for (String oneTile : tiles) {
                oneTile = oneTile.trim();
                if ("sentinel2".equalsIgnoreCase(satellite) || "sentinel-2".equalsIgnoreCase(satellite) || "s2".equalsIgnoreCase(satellite)) {
                    if (!s2TilePattern.matcher(oneTile).matches()) {
                        throw new IllegalArgumentException(String.format("[%s] is not a valid UTM tile", oneTile));
                    } else {
                        Path2D.Double tileExtent = Sentinel2TileExtent.getInstance().getTileExtent(oneTile);
                        if (tileExtent == null) {
                            throw new Exception(String.format("[%s] is not found", oneTile));
                        }
                        if (geometry == null) {
                            geometry = reader.read(Polygon2D.fromPath2D(tileExtent).toWKT());
                        } else {
                            geometry = geometry.union(reader.read(Polygon2D.fromPath2D(tileExtent).toWKT()));
                        }
                    }
                } else if ("landsat8".equalsIgnoreCase(satellite) || "landsat-8".equalsIgnoreCase(satellite) || "l8".equalsIgnoreCase(satellite)) {
                    if (!l8TilePattern.matcher(oneTile).matches()) {
                        throw new IllegalArgumentException(String.format("[%s] is not a valid Landsat-8 path row", oneTile));
                    } else {
                        Path2D.Double tileExtent = Landsat8TileExtent.getInstance().getTileExtent(oneTile);
                        if (tileExtent == null) {
                            throw new Exception(String.format("[%s] is not found", oneTile));
                        }
                        if (geometry == null) {
                            geometry = reader.read(Polygon2D.fromPath2D(tileExtent).toWKT());
                        } else {
                            geometry = geometry.union(reader.read(Polygon2D.fromPath2D(tileExtent).toWKT()));
                        }
                    }
                } else {
                    throw new IllegalArgumentException("Satellite not supported");
                }
            }
            final String wkt;
            if (geometry != null) {
                wkt = geometry.toText().replace("POLYGON ", "POLYGON");
            } else {
                wkt = null;
            }
            return prepareResult(wkt);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * Returns a list of product tiles that intersect an area of interest.
     * @param request   A structure containing: the area of interest (as WKT), the satellite (supported values: 'sentinel2', 'landsat8')
     */
    @RequestMapping(value = "/tiles", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getTilesForFootprint(@RequestBody TileRequest request) {
        try {
            if (request == null) {
                throw new IllegalArgumentException("Empty request");
            }
            final String satellite = request.getSatellite();
            final String wkt = request.getWkt();
            if (StringUtilities.isNullOrEmpty(satellite)) {
                throw new IllegalArgumentException("Empty parameter [satellite]");
            }
            if (StringUtilities.isNullOrEmpty(wkt)) {
                throw new IllegalArgumentException("Empty parameter [wkt]");
            }
            if ("sentinel2".equalsIgnoreCase(satellite) || "sentinel-2".equalsIgnoreCase(satellite) || "s2".equalsIgnoreCase(satellite)) {
                final Set<String> tiles = Sentinel2TileExtent.getInstance().intersectingTiles(Polygon2D.fromWKT(wkt));
                return prepareResult(String.join(",", tiles));
            } else if ("landsat8".equalsIgnoreCase(satellite) || "landsat-8".equalsIgnoreCase(satellite) || "l8".equalsIgnoreCase(satellite)) {
                final Set<String> tiles = Landsat8TileExtent.getInstance().intersectingTiles(Polygon2D.fromWKT(wkt));
                return prepareResult(String.join(",", tiles));
            } else if ("sentinel1".equalsIgnoreCase(satellite) || "sentinel-1".equalsIgnoreCase(satellite) || "s1".equalsIgnoreCase(satellite)) {
                final DataSource<?, ?> dataSource = DataSourceManager.getInstance().createInstance("Sentinel1", "Scientific Data Hub");
                if (dataSource == null) {
                    throw new NoSuchMethodException("Scientific Data Hub plugin not found");
                }
                dataSource.setCredentials(request.getUser(), request.getPassword());
                final DataQuery query = dataSource.createQuery("Sentinel1");
                query.addParameter(CommonParameterNames.PLATFORM, "Sentinel-1");
                QueryParameter<LocalDateTime> begin = query.createParameter(CommonParameterNames.START_DATE, LocalDateTime.class);
                final int year = LocalDate.now().getYear() - 1;
                begin.setMinValue(LocalDateTime.of(year, 1, 1, 0, 0, 0, 0));
                begin.setMaxValue(LocalDateTime.of(year, 12, 31, 0, 0, 0, 0));
                query.addParameter(begin);
                query.addParameter("sensorOperationalMode", "IW");
                query.addParameter(CommonParameterNames.PRODUCT_TYPE, "SLC");
                query.addParameter(CommonParameterNames.FOOTPRINT, Polygon2D.fromWKT(wkt));
                query.setPageSize(100);
                final List<EOProduct> results = query.execute();
                final long count = results.stream().map(EOData::getName).distinct().count();
                return prepareResult(results.stream()
                                            .map(p -> Integer.parseInt(p.getAttributeValue("relativeorbitnumber"))).distinct().sorted().map(String::valueOf)
                                            .collect(Collectors.joining(",")), count + " products");
            } else {
                throw new IllegalArgumentException("Satellite not supported");
            }
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * Returns the number of products that match the search criteria.
     * @param query The search critera
     */
    @RequestMapping(value = "/count", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> doCount(@RequestBody Query query) {
        List<DataSourceParameter> params = dataSourceService.getSupportedParameters(query.getSensor(),
                                                                                    query.getDataSource());
        if (params == null || params.isEmpty()) {
            return prepareResult(String.format("No data source named [%s] available for [%s]",
                                               query.getDataSource(),
                                               query.getSensor()), ResponseStatus.FAILED);
        }
        ExecutorService threadPool = new NamedThreadPoolExecutor("data-query-pool", Runtime.getRuntime().availableProcessors() / 2);
        try {
            CompletionService<Variable> completionService = new ExecutorCompletionService<>(threadPool);
            if (query.getValues().containsKey("tileId")) {
                Map<String, String> counts = Collections.synchronizedMap(new HashMap<>());
                List<Query> subQueries = query.splitByParameter("tileId");
                for(Query subQuery : subQueries) {
                    completionService.submit(() -> {
                        Variable variable = new Variable();
                        variable.setKey(subQuery.getValues().get("tileId"));
                        long value;
                        try {
                            value = dataSourceService.count(subQuery);
                        } catch (SerializationException e) {
                            warn(e.getMessage());
                            value = 0;
                        }
                        variable.setValue(String.valueOf(value));
                        return variable;
                    });
                }
                AtomicInteger received = new AtomicInteger(subQueries.size());
                while (received.get() > 0) {
                    try {
                        Future<Variable> result = completionService.take();
                        Variable variable = result.get();
                        counts.put(variable.getKey(), variable.getValue());
                    } catch (Exception e) {
                        warn(e.getMessage());
                    }
                }
                LinkedHashMap<String, String> sorted = new LinkedHashMap<>();
                counts.keySet().stream().sorted().forEachOrdered(s -> sorted.put(s, counts.get(s)));
                return prepareResult(sorted);
            } else {
                return prepareResult(dataSourceService.count(query));
            }
        } catch (SerializationException ex) {
            return handleException(ex);
        } finally {
            threadPool.shutdownNow();
        }
    }

    /**
     * Performs a search on a given data source and returns a list of results.
     * @param mode  The return mode: 'list' (only the product names) or 'details'
     * @param query The search criteria
     */
    @RequestMapping(value = "/exec", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> doQuery(@RequestParam(name = "mode", required = false) String mode,
                                                      @RequestBody Query query) {
        List<DataSourceParameter> params = dataSourceService.getSupportedParameters(query.getSensor(),
                                                                                    query.getDataSource());
        if (params == null || params.isEmpty()) {
            return prepareResult(String.format("No data source named [%s] available for [%s]",
                                               query.getDataSource(), query.getSensor()), ResponseStatus.FAILED);
        }
        try {
            final String actualMode = mode == null || mode.trim().isEmpty() ? "details" : mode.toLowerCase();
            if (!actualMode.equals("details") && !actualMode.equals("list")) {
                throw new IllegalArgumentException("Unsupported parameter value [mode=" + actualMode + "]");
            }
            checkContentRestriction(query);
            List<EOProduct> results = dataSourceService.query(query);
            if (results == null) {
                results = new ArrayList<>();
            }
            switch (actualMode) {
                case "details":
                    return prepareResult(results);
                case "list":
                    return prepareResult(results.stream().map(EOData::getName).collect(Collectors.joining(",")));
                default:
                    return prepareResult("");
            }
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    /**
     * Retrieves the products given in the request.
     * @param request   The request structure
     */
    @RequestMapping(value = "/fetch", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> doFetch(@RequestBody FetchRequest request) {
        final String userId = currentUser();
        final StorageService repositoryService = getLocalRepositoryService();
        final Repository localWorkspace = getLocalRepository(currentUser());
        asyncExecute(() -> {
            try {
                List<EOProduct> results;
                Query query = request.getQuery();
                checkContentRestriction(query);
                if (StringUtilities.isNullOrEmpty(query.getUserId())) {
                    query.setUserId(userId);
                }
                if (request.getLocalPath() != null && request.getPathFormat() != null) {
                    results = dataSourceService.fetch(query, request.getProducts(),
                                                      request.getMode(), request.getLocalPath(), request.getPathFormat());
                } else {
                    results = dataSourceService.fetch(query, request.getProducts(), request.getMode(), null, null);
                }
                if (results == null) {
                    results = new ArrayList<>();
                }
                List<String> productPaths = results.stream().map(EOProduct::getLocation).collect(Collectors.toList());
                if (!productPaths.isEmpty()) {
                    try {
                        final int size = productPaths.size();
                        for (int i = 0; i < size; i++) {
                            String path = FileUtilities.toUnixPath(productPaths.get(i));
                            if (!path.startsWith("/")) {
                                path = localWorkspace.resolve(path);
                                if (!repositoryService.exists(path)) {
                                    throw new FileNotFoundException(path + " does not exist");
                                }
                            }
                            productPaths.set(i, path);
                        }
                        final String label = StringUtilities.isNullOrEmpty(query.getLabel()) ? results.get(0).getProductType() : query.getLabel();
                        dataSourceComponentService.createForLocations(productPaths, label, currentPrincipal());
                        Messaging.send(userId, Topic.INFORMATION.getCategory(),
                                       "Product set [" + label + "] was added to the user [" + userId + "] workspace");
                    } catch (Exception e) {
                        warn(e.getMessage());
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(getClass().getName()).warning(ex.getMessage());
            }
        });
        return prepareResult("Download queued", ResponseStatus.SUCCEEDED);
    }

    /**
     * Finds all the supported products in a path.
     * A product, besides the files constituting it, holds additional metadata in the TAO database.
     * @param productPath   The path to inspect, relative to the user workspace
     */
    @RequestMapping(value = "/inspect", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> inspect(@RequestParam("path") String productPath) {
        Set<MetadataInspector> services = ServiceRegistryManager.getInstance()
                .getServiceRegistry(MetadataInspector.class)
                .getServices();
        MetadataInspector metadataInspector = null;
        final Path path = Paths.get(SystemVariable.USER_WORKSPACE.value(), productPath);
        if (services != null) {
            metadataInspector = services.stream()
                    .filter(s -> s.decodeQualification(path) == DecodeStatus.SUITABLE)
                    .findFirst().orElse(null);
        }
        if (metadataInspector != null) {
            try {
                MetadataInspector.Metadata metadata = metadataInspector.getMetadata(path);
                return prepareResult(metadata.toString());
            } catch (IOException e) {
                return handleException(e);
            }
        } else {
            return prepareResult("No metadata inspector found", ResponseStatus.FAILED);
        }
    }

    private void checkContentRestriction(Query query) throws Exception {
        // Verify that the incoming query respects the (optionally) defined area content restriction
        final String aoiRestriction = ConfigurationManager.getInstance().getValue("area.content.restriction", null);
        if (!StringUtilities.isNullOrEmpty(aoiRestriction)) {
            final double aoiMinIntersection = Double.parseDouble(ConfigurationManager.getInstance().getValue("area.content.intersection", "0.1"));
            final GeometryAdapter adapter = new GeometryAdapter();
            if (aoiGeometry == null) {
                final Geometry aoiGeom = adapter.marshal(aoiRestriction);
                if (aoiGeom != null) {
                    final int numGeometries = aoiGeom.getNumGeometries();
                    for (int i = 0; i < numGeometries; i++) {
                        final Geometry geometry = aoiGeom.getGeometryN(i);
                        if (geometry instanceof Polygon || geometry instanceof MultiPolygon) {
                            try {
                                aoiGeometry = aoiGeometry == null ? geometry : aoiGeometry.union(geometry);
                            } catch (TopologyException ex) {
                                warn(ex.getMessage());
                            }
                        }
                    }
                }
            }
            final String siteId = query.getSiteId();
            String footprint = null;
            if (siteId != null) {
                final Site site = siteProvider.get(siteId);
                if (site != null) {
                    footprint = site.getFootprint();
                    final Geometry siteGeom = adapter.marshal(footprint);
                    if (Double.compare(siteGeom.intersection(aoiGeometry).getArea() / siteGeom.getArea(), aoiMinIntersection) < 0) {
                        throw new IllegalArgumentException("Selected area is outside the allowed search area");
                    }
                }
            } else {
                footprint = query.getValues().entrySet().stream()
                                 .filter(e -> CommonParameterNames.FOOTPRINT.equals(e.getKey()))
                                 .map(Map.Entry::getValue).findFirst().orElse(null);
            }
            if (footprint != null) {
                final Geometry siteGeom = adapter.marshal(footprint);
                if (Double.compare(siteGeom.intersection(aoiGeometry).getArea() / siteGeom.getArea(), aoiMinIntersection) < 0) {
                    throw new IllegalArgumentException("Selected area is outside the allowed search area");
                }
            }
        }
    }

    private Repository getLocalRepository(String user) {
        return repositoryProvider.getUserSystemRepositories(user).stream().filter(w -> w.getType() == RepositoryType.LOCAL).findFirst().orElse(null);
    }

    private StorageService getLocalRepositoryService() {
        Repository repository = getLocalRepository(currentUser());
        if (repository == null) {
            throw new RuntimeException("User [" + currentUser() + "] doesn't have a local repository");
        }
        StorageService instance = StorageServiceFactory.getInstance(repository);
        instance.associate(repository);
        return instance;
    }
}
