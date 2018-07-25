/*
 * Copyright (C) 2017 CS ROMANIA
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ro.cs.tao.component.Variable;
import ro.cs.tao.datasource.param.ParameterDescriptor;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.execution.model.Query;
import ro.cs.tao.serialization.SerializationException;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.ServiceError;
import ro.cs.tao.services.interfaces.DataSourceService;
import ro.cs.tao.services.model.datasource.DataSourceDescriptor;
import ro.cs.tao.services.query.beans.FetchRequest;
import ro.cs.tao.utils.executors.NamedThreadPoolExecutor;

import java.util.*;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Cosmin Cara
 */
@Controller
@RequestMapping("/query")
public class DataSourceController extends BaseController {

    @Autowired
    private DataSourceService dataSourceService;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ResponseEntity<List<DataSourceDescriptor>> getRegisteredSources() {
        List<DataSourceDescriptor> instances = dataSourceService.getDatasourceInstances();
        if (instances == null || instances.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(instances, HttpStatus.OK);
    }

    @RequestMapping(value = "/sensor/", method = RequestMethod.GET)
    public ResponseEntity<SortedSet<String>> getSupportedSensors() {
        SortedSet<String> sensors = dataSourceService.getSupportedSensors();
        if (sensors == null || sensors.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(sensors, HttpStatus.OK);
    }

    @RequestMapping(value = "/sensor/{name}", method = RequestMethod.GET)
    public ResponseEntity<?> getDatasourcesForSensor(@PathVariable("name") String sensorName) {
        List<String> sources = dataSourceService.getDatasourcesForSensor(sensorName);
        if (sources == null) {
            return new ResponseEntity<>(new ServiceError(String.format("No data source available for [%s]", sensorName)),
                                        HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(sources, HttpStatus.OK);
    }

    @RequestMapping(value = "/sensor/{name}/{source:.+}", method = RequestMethod.GET)
    public ResponseEntity<?> getSupportedParameters(@PathVariable("name") String sensorName,
                                                                            @PathVariable("source") String dataSourceClassName) {
        List<ParameterDescriptor> params = dataSourceService.getSupportedParameters(sensorName, dataSourceClassName);
        if (params == null) {
            return new ResponseEntity<>(new ServiceError(String.format("No data source available for [%s]", sensorName)),
                                        HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(params, HttpStatus.OK);
    }

    @RequestMapping(value = "/count", method = RequestMethod.POST)
    public ResponseEntity<?> doCount(@RequestBody Query query) {
        List<ParameterDescriptor> params = dataSourceService.getSupportedParameters(query.getSensor(),
                                                                                    query.getDataSource());
        if (params == null) {
            return new ResponseEntity<>(new ServiceError(String.format("No data source named [%s] available for [%s]",
                                                                       query.getDataSource(),
                                                                       query.getSensor())),
                                        HttpStatus.BAD_REQUEST);
        }
        ExecutorService threadPool = new NamedThreadPoolExecutor("data-query-pool", Runtime.getRuntime().availableProcessors() / 2);
        //Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() / 2);
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
                return new ResponseEntity<>(sorted, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(dataSourceService.count(query), HttpStatus.OK);
            }
        } catch (SerializationException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        } finally {
            threadPool.shutdownNow();
        }
    }

    @RequestMapping(value = "/exec", method = RequestMethod.POST)
    public ResponseEntity<?> doQuery(@RequestBody Query query) {
        List<ParameterDescriptor> params = dataSourceService.getSupportedParameters(query.getSensor(),
                query.getDataSource());
        if (params == null) {
            return new ResponseEntity<>(new ServiceError(String.format("No data source named [%s] available for [%s]",
                    query.getDataSource(),
                    query.getSensor())),
                    HttpStatus.BAD_REQUEST);
        }
        try {
            final List<EOProduct> results = dataSourceService.query(query);
            if (results == null) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(results, HttpStatus.OK);
        } catch (SerializationException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @RequestMapping(value = "/fetch", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<?> doFetch(@RequestBody FetchRequest request) {
        try {
            final List<EOProduct> results;
            if (request.getLocalPath() != null && request.getPathFormat() != null) {
                results = dataSourceService.fetch(request.getQuery(), request.getProducts(),
                                                  request.getMode(), request.getLocalPath(), request.getPathFormat());
            } else {
                results = dataSourceService.fetch(request.getQuery(), request.getProducts(), request.getMode(), null, null);
            }
            if (results == null) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(results, HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
