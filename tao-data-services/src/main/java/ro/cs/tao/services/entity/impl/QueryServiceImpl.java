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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ro.cs.tao.datasource.DataSourceManager;
import ro.cs.tao.datasource.beans.Query;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.interfaces.QueryService;

import java.util.List;

@Service("queryService")
public class QueryServiceImpl extends EntityService<Query>
        implements QueryService {

    @Autowired
    private PersistenceManager persistenceManager;


    @Override
    public Query findById(Long id) throws PersistenceException {
        return getQueryById(id);
    }

    @Override
    public Query getQueryById(long id) {
        //return persistenceManager.findQueryById(id);
        try {
            return persistenceManager.getQuery(id);
        } catch (PersistenceException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<Query> list() {
        //return persistenceManager.getQueries(SessionStore.currentContext().getPrincipal().getName());
        try {
            return persistenceManager.getUserQueries(SessionStore.currentContext().getPrincipal().getName());
        } catch (PersistenceException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<Query> list(Iterable<Long> ids) {
        return persistenceManager.getQueries(ids);
    }

    @Override
    public Query getQuery(String userId, String sensor, String dataSource, long workflowNodeId) {
        return persistenceManager.getQuery(userId, sensor, dataSource, workflowNodeId);
    }

    @Override
    public List<Query> getQueries(String userId, String sensor, String dataSource) {
        return persistenceManager.getQueries(userId, sensor, dataSource);
    }

    @Override
    public List<Query> getQueries(String userId, long nodeid) {
        return persistenceManager.getQueries(userId, nodeid);
    }

    @Override
    public List<Query> getQueries(String userId) {
        //return persistenceManager.getQueries(userId);
        try {
            return persistenceManager.getUserQueries(userId);
        } catch (PersistenceException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<Query> getQueriesBySensor(String userId, String sensor) {
        return persistenceManager.getQueriesBySensor(userId, sensor);
    }

    @Override
    public List<Query> getQueriesByDataSource(String userId, String dataSource) {
        return persistenceManager.getQueriesByDataSource(userId, dataSource);
    }

    @Override
    public Page<Query> getAllQueries(Pageable pageable) {
        return persistenceManager.getAllQueries(pageable);
    }

    @Override
    public Query save(Query object) {
        try {
            return persistenceManager.saveQuery(object);
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
        persistenceManager.removeQuery(id);
    }

    @Override
    protected void validateFields(Query entity, List<String> errors) {
        if (entity.getWorkflowNodeId() <= 0) {
            errors.add("[workflowNodeId] Invalid node identifier");
        }
        String value = entity.getSensor();
        if (value == null || value.isEmpty() ||
                !DataSourceManager.getInstance().getSupportedSensors().contains(value)) {
            errors.add("[sensor] Invalid value");
        } else {
            value = entity.getDataSource();
            if (value == null || value.isEmpty() ||
                    DataSourceManager.getInstance().get(entity.getSensor(), value) == null) {
                errors.add("[dataSource] Invalid value");
            }
        }
    }
}
