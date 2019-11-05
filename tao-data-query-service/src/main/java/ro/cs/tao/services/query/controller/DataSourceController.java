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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.component.Variable;
import ro.cs.tao.datasource.beans.Query;
import ro.cs.tao.datasource.param.DataSourceParameter;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.eodata.Polygon2D;
import ro.cs.tao.eodata.metadata.DecodeStatus;
import ro.cs.tao.eodata.metadata.MetadataInspector;
import ro.cs.tao.products.landsat.Landsat8TileExtent;
import ro.cs.tao.products.sentinels.Sentinel2TileExtent;
import ro.cs.tao.serialization.SerializationException;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.DataSourceService;
import ro.cs.tao.services.model.datasource.DataSourceDescriptor;
import ro.cs.tao.services.query.beans.FetchRequest;
import ro.cs.tao.spi.ServiceRegistryManager;
import ro.cs.tao.utils.executors.NamedThreadPoolExecutor;

import java.awt.geom.Path2D;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * @author Cosmin Cara
 */
@Controller
@RequestMapping("/query")
public class DataSourceController extends BaseController {

    private static final Pattern s2TilePattern = Pattern.compile("\\d{2}\\w{3}");
    private static final Pattern l8TilePattern = Pattern.compile("\\d{6}");

    @Autowired
    private DataSourceService dataSourceService;

    @RequestMapping(value = "/", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getRegisteredSources() {
        List<DataSourceDescriptor> instances = dataSourceService.getDatasourceInstances();
        if (instances == null) {
            instances = new ArrayList<>();
        }
        return prepareResult(instances);
    }

    @RequestMapping(value = "/sensor/", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getSupportedSensors() {
        Set<String> sensors = dataSourceService.getSupportedSensors();
        if (sensors == null) {
            sensors = new HashSet<>();
        }
        return prepareResult(sensors);
    }

    @RequestMapping(value = "/sensor/{name}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getDatasourcesForSensor(@PathVariable("name") String sensorName) {
        List<String> sources = dataSourceService.getDatasourcesForSensor(sensorName);
        if (sources == null) {
            sources = new ArrayList<>();
        }
        return prepareResult(sources);
    }

    @RequestMapping(value = "/sensor/{name}/{source:.+}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getSupportedParameters(@PathVariable("name") String sensorName,
                                                                     @PathVariable("source") String dataSourceClassName) {
        List<DataSourceParameter> params = dataSourceService.getSupportedParameters(sensorName, dataSourceClassName);
        if (params == null) {
            params = new ArrayList<>();
        }
        int count = params.size();
        for (int i = 0; i < count; i++) {
            params.get(i).setOrder(i + 1);
        }
        return prepareResult(params);
    }

    @RequestMapping(value = "/footprint/sentinel2", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getS2TileFootprint(@RequestParam("tile") String tile) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        if (tile == null) {
            responseEntity = prepareResult("Empty parameter [tile]", ResponseStatus.FAILED);
        } else {
            if (!s2TilePattern.matcher(tile).matches()) {
                responseEntity = prepareResult(String.format("[%s] is not a valid UTM tile", tile), ResponseStatus.FAILED);
            } else {
                Path2D.Double tileExtent = Sentinel2TileExtent.getInstance().getTileExtent(tile);
                if (tileExtent == null) {
                    responseEntity = prepareResult(String.format("[%s] is not found", tile), ResponseStatus.FAILED);
                } else {
                    responseEntity = prepareResult(Polygon2D.fromPath2D(tileExtent).toWKT());
                }
            }
        }
        return responseEntity;
    }

    @RequestMapping(value = "/footprint/landsat8", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getL8TileFootprint(@RequestParam("tile") String tile) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        if (tile == null) {
            responseEntity = prepareResult("Empty parameter [tile]", ResponseStatus.FAILED);
        } else {
            if (!l8TilePattern.matcher(tile).matches()) {
                responseEntity = prepareResult(String.format("[%s] is not a valid Landsat-8 path row", tile), ResponseStatus.FAILED);
            } else {
                Path2D.Double tileExtent = Landsat8TileExtent.getInstance().getTileExtent(tile);
                if (tileExtent == null) {
                    responseEntity = prepareResult(String.format("[%s] is not found", tile), ResponseStatus.FAILED);
                } else {
                    responseEntity = prepareResult(Polygon2D.fromPath2D(tileExtent).toWKT());
                }
            }
        }
        return responseEntity;
    }

    @RequestMapping(value = "/count", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
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
                            e.printStackTrace();
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
                        e.printStackTrace();
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

    @RequestMapping(value = "/exec", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> doQuery(@RequestBody Query query) {
        List<DataSourceParameter> params = dataSourceService.getSupportedParameters(query.getSensor(),
                                                                                    query.getDataSource());
        if (params == null || params.isEmpty()) {
            return prepareResult(String.format("No data source named [%s] available for [%s]",
                                               query.getDataSource(), query.getSensor()), ResponseStatus.FAILED);
        }
        try {
            List<EOProduct> results = dataSourceService.query(query);
            if (results == null) {
                results = new ArrayList<>();
            }
            return prepareResult(results);
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @RequestMapping(value = "/fetch", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> doFetch(@RequestBody FetchRequest request) {
        try {
            List<EOProduct> results;
            if (request.getLocalPath() != null && request.getPathFormat() != null) {
                results = dataSourceService.fetch(request.getQuery(), request.getProducts(),
                                                  request.getMode(), request.getLocalPath(), request.getPathFormat());
            } else {
                results = dataSourceService.fetch(request.getQuery(), request.getProducts(), request.getMode(), null, null);
            }
            if (results == null) {
                results = new ArrayList<>();
            }
            return prepareResult(results);
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    @RequestMapping(value = "/inspect", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> inspect(@RequestParam("path") String productPath) {
        Set<MetadataInspector> services = ServiceRegistryManager.getInstance()
                .getServiceRegistry(MetadataInspector.class)
                .getServices();
        MetadataInspector metadataInspector = null;
        final Path path = Paths.get(SystemVariable.SHARED_WORKSPACE.value(), productPath);
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
}
