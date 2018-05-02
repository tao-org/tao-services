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

package ro.cs.tao.services.entity.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.services.interfaces.DataSourceComponentService;

import java.util.List;

@Service("dataSourceComponentService")
public class DataSourceComponentServiceImpl implements DataSourceComponentService {

    @Autowired
    private PersistenceManager persistenceManager;

    @Override
    public DataSourceComponent findById(String id) throws PersistenceException {
        return persistenceManager.getDataSourceInstance(id);
    }

    @Override
    public List<DataSourceComponent> list() {
        return persistenceManager.getDataSourceComponents();
    }

    @Override
    public DataSourceComponent save(DataSourceComponent object) {
        try {
            return persistenceManager.saveDataSourceComponent(object);
        } catch (PersistenceException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public DataSourceComponent update(DataSourceComponent object) {
        return save(object);
    }

    @Override
    public void delete(String id) throws PersistenceException {
        throw new UnsupportedOperationException("Data source components cannot be deleted");
    }
}
