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
package ro.cs.tao.services.query.service;

import org.springframework.stereotype.Service;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.datasource.DataSourceManager;
import ro.cs.tao.datasource.param.ParameterDescriptor;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.execution.model.Query;
import ro.cs.tao.serialization.SerializationException;
import ro.cs.tao.services.interfaces.DataSourceService;
import ro.cs.tao.services.model.datasource.DataSourceDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

/**
 * @author Cosmin Cara
 */
@Service("dataSourceService")
public class DataSourceServiceImpl implements DataSourceService {

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
        Map<String, ParameterDescriptor> parameterDescriptorMap =
                DataSourceManager.getInstance().getSupportedParameters(sensorName, dataSourceName);
        List<ParameterDescriptor> parameters = null;
        if (parameterDescriptorMap != null) {
            parameters = new ArrayList<>(parameterDescriptorMap.values());
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
    public List<EOProduct> fetch(Query query, List<EOProduct> products) {
        if (products != null) {
            DataSourceComponent dsComponent = new DataSourceComponent(query.getSensor(), query.getDataSource());
            dsComponent.setUserCredentials(query.getUser(), query.getPassword());
            String path = ConfigurationManager.getInstance().getValue("product.location");
            products = dsComponent.doFetch(products, null, path);
        }
        return products;
    }
}
