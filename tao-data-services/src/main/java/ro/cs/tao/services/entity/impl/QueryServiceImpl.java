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
import ro.cs.tao.execution.model.Query;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.interfaces.QueryService;
import ro.cs.tao.utils.Crypto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service("queryService")
public class QueryServiceImpl extends EntityService<Query>
        implements QueryService {

    @Autowired
    private PersistenceManager persistenceManager;


    @Override
    public Query findById(Long id) throws PersistenceException {
        Query query = getQueryById(id);
        if (query != null) {
            ensurePasswordDecrypted(query);
        }
        return query;
    }

    @Override
    public Query getQueryById(long id) {
        Query query = persistenceManager.findQueryById(id);
        if (query != null) {
            ensurePasswordDecrypted(query);
        }
        return query;
    }

    @Override
    public List<Query> list() {
        List<Query> queries = persistenceManager.getQueries(SessionStore.currentContext().getPrincipal().getName());
        if (queries != null) {
            queries.forEach(this::ensurePasswordDecrypted);
        }
        return queries;
    }

    @Override
    public Query getQuery(String userId, String sensor, String dataSource, long workflowNodeId) {
        Query query = persistenceManager.getQuery(userId, sensor, dataSource, workflowNodeId);
        if (query != null) {
            ensurePasswordDecrypted(query);
        }
        return query;
    }

    @Override
    public List<Query> getQueries(String userId, String sensor, String dataSource) {
        List<Query> queries = persistenceManager.getQueries(userId, sensor, dataSource);
        if (queries != null) {
            queries.forEach(this::ensurePasswordDecrypted);
        }
        return queries;
    }

    @Override
    public List<Query> getQueries(String userId, long nodeid) {
        List<Query> queries = persistenceManager.getQueries(userId, nodeid);
        if (queries != null) {
            queries.forEach(this::ensurePasswordDecrypted);
        }
        return queries;
    }

    @Override
    public List<Query> getQueries(String userId) {
        List<Query> queries = persistenceManager.getQueries(userId);
        if (queries != null) {
            queries.forEach(this::ensurePasswordDecrypted);
        }
        return queries;
    }

    @Override
    public List<Query> getQueriesBySensor(String userId, String sensor) {
        List<Query> queries = persistenceManager.getQueriesBySensor(userId, sensor);
        if (queries != null) {
            queries.forEach(this::ensurePasswordDecrypted);
        }
        return queries;
    }

    @Override
    public List<Query> getQueriesByDataSource(String userId, String dataSource) {
        List<Query> queries = persistenceManager.getQueriesByDataSource(userId, dataSource);
        if (queries != null) {
            queries.forEach(this::ensurePasswordDecrypted);
        }
        return queries;
    }

    @Override
    public Page<Query> getAllQueries(Pageable pageable) {
        Page<Query> queries = persistenceManager.getAllQueries(pageable);
        if (queries != null) {
            queries.forEach(this::ensurePasswordDecrypted);
        }
        return queries;
    }

    @Override
    public Query save(Query object) {
        if (object.getUser() != null && object.getPassword() != null) {
            ensurePasswordDecrypted(object);
        }
        if (object.getLabel() == null) {
            object.setLabel(String.format("Query for %s on %s - %s",
                                          object.getSensor(), object.getDataSource(),
                                          LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        }
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

    private void ensurePasswordEncrypted(Query object) {
        if (Crypto.decrypt(object.getPassword(), object.getUser()) == null) {
            // we have an unencrypted password
            object.setPassword(Crypto.encrypt(object.getPassword(), object.getUser()));
        }
    }

    private void ensurePasswordDecrypted(Query object) {
        if (Crypto.decrypt(object.getPassword(), object.getUser()) != null) {
            // we have an unencrypted password
            object.setPassword(Crypto.decrypt(object.getPassword(), object.getUser()));
        }
    }
}
