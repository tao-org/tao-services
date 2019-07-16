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
package ro.cs.tao.services.query.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.datasource.DataSource;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.datasource.DataSourceManager;
import ro.cs.tao.datasource.ProductStatusListener;
import ro.cs.tao.datasource.beans.Query;
import ro.cs.tao.datasource.param.ParameterName;
import ro.cs.tao.datasource.remote.FetchMode;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.eodata.enums.ProductStatus;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.serialization.SerializationException;
import ro.cs.tao.services.interfaces.DataSourceService;
import ro.cs.tao.services.model.datasource.DataSourceDescriptor;
import ro.cs.tao.services.model.datasource.ParameterDescriptor;
import ro.cs.tao.utils.async.Parallel;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Cosmin Cara
 */
@Service("dataSourceService")
public class DataSourceServiceImpl implements DataSourceService {

    @Autowired
    private DownloadListener downloadListener;

    @Autowired
    private PersistenceManager persistenceManager;

    @Override
    public SortedSet<String> getSupportedSensors() {
        return DataSourceManager.getInstance().getSupportedSensors();
    }

    @Override
    public List<String> getDatasourcesForSensor(String sensorName) {
        return DataSourceManager.getInstance().getNames(sensorName);
    }

    @Override
    public List<DataSourceDescriptor> getDatasourceInstances() {
        List<DataSourceDescriptor> instances = new ArrayList<>();
        final DataSourceManager dataSourceManager = DataSourceManager.getInstance();
        final SortedSet<String> sensors = dataSourceManager.getSupportedSensors();
        for (String sensor : sensors) {
            dataSourceManager.getNames(sensor).forEach(n ->
                instances.add(new DataSourceDescriptor(sensor, n, dataSourceManager.getSupportedParameters(sensor, n))));
        }
        return instances;
    }

    @Override
    public List<ParameterDescriptor> getSupportedParameters(String sensorName, String dataSourceName) {
        List<ParameterDescriptor> parameters = null;
        DataSource dataSource = DataSourceManager.getInstance().get(sensorName, dataSourceName);
        if (dataSource != null) {
            Map<String, Map<ParameterName, ro.cs.tao.datasource.param.DataSourceParameter>> map = dataSource.getSupportedParameters();
            Map<ParameterName, ro.cs.tao.datasource.param.DataSourceParameter> parameterDescriptorMap = map.get(sensorName);
            if (parameterDescriptorMap != null) {
                parameters = parameterDescriptorMap.entrySet().stream()
                        .map(e -> new ParameterDescriptor(e.getKey().getSystemName(), e.getKey().getLabel(),
                                                          e.getKey().getDescription(),
                                                          e.getValue().getType(),
                                                          e.getValue().getDefaultValue(), e.getValue().isRequired(),
                                                          e.getValue().getValueSet()))
                        .collect(Collectors.toList());
            }
        }
        return parameters;
    }

    @Override
    public long count(Query queryObject) throws SerializationException {
        return Query.toDataQuery(queryObject).getCount();
    }

    @Override
    public List<EOProduct> query(Query queryObject) throws SerializationException {
        return Query.toDataQuery(queryObject).execute();
    }

    @Override
    public List<EOProduct> fetch(Query query, final List<EOProduct> products, FetchMode mode, String localPath, String pathFormat) {
        final List<EOProduct> results;
        if (products != null) {
            final DataSourceComponent dsComponent = new DataSourceComponent(query.getSensor(), query.getDataSource());
            dsComponent.setUserCredentials(query.getUser(), query.getPassword());
            dsComponent.setFetchMode(mode);
            dsComponent.setProductStatusListener(downloadListener);
            String path = SystemVariable.SHARED_WORKSPACE.value();
            final Set<String> existingSet = new HashSet<>(persistenceManager.getExistingProductNames(products.stream().map(EOProduct::getName).toArray(String[]::new)));
            products.removeIf(p -> existingSet.contains(p.getName()) && mode != FetchMode.RESUME);
            final Logger logger = Logger.getLogger(DataSourceService.class.getName());
            for (EOProduct product : products) {
                product.setProductStatus(ProductStatus.QUERIED);
                product.addReference(query.getUser());
                try {
                    persistenceManager.saveEOProduct(product);
                } catch (PersistenceException e) {
                    logger.warning(String.format("Cannot persist product %s. Reason: %s",product.getName(), e.getMessage()));
                }
            }
            final int parallelism = DataSourceManager.getInstance().get(dsComponent.getSensorName(),
                                                                        dsComponent.getDataSourceName()).getMaximumAllowedTransfers();
            if ((mode == FetchMode.SYMLINK || mode == FetchMode.COPY) && localPath != null) {
                Properties properties = new Properties();
                properties.put("local.archive.path.format", pathFormat);
                if (parallelism > 1) {
                    results = Collections.synchronizedList(new ArrayList<>());
                    Parallel.For(0, products.size(), parallelism, (i) -> {
                        try {
                            DataSourceComponent clone = cloneComponent(dsComponent, downloadListener);
                            results.addAll(dsComponent.doFetch(products.subList(i, i + 1), null, path, localPath, properties));
                        } catch (Exception ex) {
                            logger.severe(ex.getMessage());
                        }
                    });
                } else {
                    results = dsComponent.doFetch(products, null, path, localPath, properties);
                }
            } else {
                if (parallelism > 1) {
                    results = Collections.synchronizedList(new ArrayList<>());
                    Parallel.For(0, products.size(), parallelism, (i) -> {
                        try {
                            DataSourceComponent clone = cloneComponent(dsComponent, downloadListener);
                            results.addAll(clone.doFetch(products.subList(i, i + 1), null, path));
                        } catch (Exception ex) {
                            logger.severe(ex.getMessage());
                        }
                    });
                } else {
                    results = dsComponent.doFetch(products, null, path);
                }
            }
        } else {
            results = null;
        }
        return results;
    }

    private DataSourceComponent cloneComponent(DataSourceComponent source, ProductStatusListener listener) throws CloneNotSupportedException {
        DataSourceComponent clone = source.clone();
        clone.setUserCredentials(source.getUserName(), source.getPassword());
        clone.setFetchMode(source.getFetchMode());
        clone.setProductStatusListener(listener);
        return clone;
    }
}
