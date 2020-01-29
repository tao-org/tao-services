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

package ro.cs.tao.services.entity.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.Sort;
import ro.cs.tao.datasource.DataSourceManager;
import ro.cs.tao.datasource.beans.Query;
import ro.cs.tao.datasource.param.DataSourceParameter;
import ro.cs.tao.datasource.persistence.QueryProvider;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.interfaces.QueryService;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service("queryService")
public class QueryServiceImpl extends EntityService<Query>
        implements QueryService {

    @Autowired
    private QueryProvider queryProvider;


    @Override
    public Query findById(Long id) {
        return getQueryById(id);
    }

    @Override
    public Query getQueryById(long id) {
        return queryProvider.get(id);
    }

    @Override
    public List<Query> list() {
        return queryProvider.list(SessionStore.currentContext().getPrincipal().getName());
    }

    @Override
    public List<Query> list(Iterable<Long> ids) {
        return queryProvider.list(ids);
    }

    @Override
    public Query getQuery(String userId, String sensor, String dataSource, long workflowNodeId) {
        return queryProvider.get(userId, sensor, dataSource, workflowNodeId);
    }

    @Override
    public List<Query> getQueries(String userId, String sensor, String dataSource) {
        return queryProvider.list(userId, sensor, dataSource);
    }

    @Override
    public List<Query> getQueries(String userId, long nodeId) {
        return queryProvider.list(userId, nodeId);
    }

    @Override
    public List<Query> getQueries(String userId) {
        return queryProvider.list(userId);
    }

    @Override
    public List<Query> getQueriesBySensor(String userId, String sensor) {
        return queryProvider.listBySensor(userId, sensor);
    }

    @Override
    public List<Query> getQueriesByDataSource(String userId, String dataSource) {
        return queryProvider.listByDataSource(userId, dataSource);
    }

    @Override
    public List<Query> getQueries(int pageNumber, int pageSize, Sort sort) {
        return queryProvider.list(pageNumber, pageSize, sort);
    }

    @Override
    public Query save(Query object) {
        try {
            return queryProvider.save(object);
        } catch (PersistenceException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public Query update(Query object) {
        return save(object);
    }

    @Override
    public void delete(Long id) throws PersistenceException {
        queryProvider.delete(id);
    }

    @Override
    protected void validateFields(Query entity, List<String> errors) {
        if (entity.getWorkflowNodeId() != null && entity.getWorkflowNodeId() <= 0) {
            errors.add("[workflowNodeId] Invalid node identifier");
        }
        DataSourceManager dataSourceManager = DataSourceManager.getInstance();
        String value = entity.getSensor();
        if (value == null || value.isEmpty() ||
                !dataSourceManager.getSupportedSensors().contains(value)) {
            errors.add("[sensor] Invalid value");
        } else {
            value = entity.getDataSource();
            if (value == null || value.isEmpty() ||
                    dataSourceManager.get(entity.getSensor(), value) == null) {
                errors.add("[dataSource] Invalid value");
            } else {
                final Map<String, DataSourceParameter> parameters = dataSourceManager.getSupportedParameters(entity.getSensor(), entity.getDataSource());
                final Map<String, String> values = entity.getValues();
                if (values != null) {
                    Set<String> keys = values.keySet();
                    for (String key : keys) {
                        if (!parameters.containsKey(key)) {
                            errors.add(String.format("[%s] Parameter not supported for this data source", key));
                        }
                    }
                }

            }
        }
    }
}
