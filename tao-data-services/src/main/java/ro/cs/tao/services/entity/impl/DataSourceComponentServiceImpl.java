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
import ro.cs.tao.Tag;
import ro.cs.tao.component.TaoComponent;
import ro.cs.tao.component.enums.TagType;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.eodata.EOData;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.services.interfaces.DataSourceComponentService;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    public List<DataSourceComponent> list(Optional<Integer> pageNumber, Optional<Integer> pageSize, Sort sort) {
        return persistenceManager.getDataSourceComponents(pageNumber.orElse(0), pageSize.orElse(0), sort);
    }

    @Override
    public List<DataSourceComponent> getDataSourceComponents(Iterable<String> ids) {
        return persistenceManager.getDataSourceComponents(ids);
    }

    @Override
    public List<DataSourceComponent> getUserDataSourceComponents(String userName) {
        return persistenceManager.getUserDataSourceComponents(userName);
    }

    @Override
    public List<DataSourceComponent> getSystemDataSourceComponents() {
        return persistenceManager.getSystemDataSourceComponents();
    }

    @Override
    public List<Tag> getDatasourceTags() {
        return persistenceManager.getDatasourceTags();
    }

    @Override
    public DataSourceComponent save(DataSourceComponent object) {
        try {
            addTagsIfNew(object);
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

    public DataSourceComponent createFor(List<EOProduct> products, Principal principal) throws PersistenceException {
        if (products == null || products.isEmpty() || principal == null) {
            return null;
        }
        String productType = products.stream()
                                     .filter(p -> p.getProductType() != null)
                                     .map(EOProduct::getProductType)
                                     .findFirst().orElse(null);
        if (productType == null) {
            return null;
        }
        DataSourceComponent systemDSC = persistenceManager.getDataSourceInstance(productType + "-Local Database");
        if (systemDSC == null) {
            return null;
        }
        DataSourceComponent userDSC;
        try {
            userDSC = systemDSC.clone();
            LocalDateTime time = LocalDateTime.now();
            userDSC.setId(systemDSC.getId() + "-" + principal.getName() + "-" + time.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            userDSC.setLabel(systemDSC.getLabel() + " (customized on " + time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + ")");
            List<String> nameList = products.stream().map(EOData::getName).collect(Collectors.toList());
            userDSC.getSources().get(0).getDataDescriptor().setLocation(String.join(",", nameList));
            userDSC.getSources().get(0).setCardinality(products.size());
            userDSC.getTargets().get(0).setCardinality(products.size());
            userDSC.getTargets().get(0).addConstraint("Same cardinality");
            userDSC.setSystem(false);
            userDSC = persistenceManager.saveDataSourceComponent(userDSC);
        } catch (CloneNotSupportedException e) {
            throw new PersistenceException(e);
        }
        return userDSC;
    }

    private void addTagsIfNew(TaoComponent component) {
        List<String> tags = component.getTags();
        if (tags != null) {
            List<Tag> componentTags = persistenceManager.getComponentTags();
            for (String value : tags) {
                if (componentTags.stream().noneMatch(t -> t.getText().equalsIgnoreCase(value))) {
                    persistenceManager.saveTag(new Tag(TagType.COMPONENT, value));
                }
            }
        }
    }
}
