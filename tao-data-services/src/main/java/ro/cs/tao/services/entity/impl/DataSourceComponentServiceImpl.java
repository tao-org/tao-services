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
import ro.cs.tao.component.SourceDescriptor;
import ro.cs.tao.component.TargetDescriptor;
import ro.cs.tao.component.enums.TagType;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.eodata.enums.ProductStatus;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.services.interfaces.DataSourceComponentService;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
    public List<DataSourceComponent> list(Iterable<String> ids) {
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
            return persistenceManager.saveDataSourceComponent(object);
        } catch (PersistenceException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public DataSourceComponent update(DataSourceComponent object) {
        try {
            return persistenceManager.updateDataSourceComponent(object);
        } catch (PersistenceException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public void delete(String id) throws PersistenceException {
        throw new UnsupportedOperationException("Data source components cannot be deleted");
    }

    @Override
    public DataSourceComponent createForProducts(List<EOProduct> products, String dataSource,
                                                 String label, Principal principal) throws PersistenceException {
        if (products == null || products.isEmpty()) {
            throw new IllegalArgumentException("Product list is empty");
        }
        Set<String> types = products.stream().map(EOProduct::getProductType).collect(Collectors.toSet());
        if (types.size() != 1) {
            throw new IllegalArgumentException("Products must be of the same type");
        }
        String productType = types.iterator().next();
        List<String> nameList = new ArrayList<>();
        for (EOProduct product : products) {
            product.setProductStatus(ProductStatus.QUERIED);
            nameList.add(product.getName());
            persistenceManager.saveEOProduct(product);
        }
        return createForProductNames(nameList, productType, dataSource, label, principal);
    }

    @Override
    public DataSourceComponent createForProductNames(List<String> productNames, String sensor, String dataSource,
                                                     String label, Principal principal) throws PersistenceException {
        if (productNames == null || productNames.isEmpty()) {
            throw new IllegalArgumentException("Product list is empty");
        }
        if (sensor == null || sensor.isEmpty()) {
            throw new IllegalArgumentException("[sensor] is null or empty");
        }
        if (dataSource == null || dataSource.isEmpty()) {
            throw new IllegalArgumentException("[sensor] is null or empty");
        }
        if (principal == null) {
            throw new IllegalArgumentException("Invalid principal (null)");
        }
        DataSourceComponent systemDSC = persistenceManager.getDataSourceInstance(sensor + "-Local Database");
        if (systemDSC == null) {
            return null;
        }
        DataSourceComponent userDSC;
        try {
            userDSC = systemDSC.clone();
            LocalDateTime time = LocalDateTime.now();
            String newId = String.join("-", sensor, dataSource, principal.getName(),
                                       time.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            userDSC.setId(newId);
            userDSC.setLabel(label != null && !label.isEmpty() ?
                                     label : String.join("-", sensor, dataSource, principal.getName()) +
                                     " [customized on " + time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "]");
            SourceDescriptor sourceDescriptor = userDSC.getSources().get(0);
            sourceDescriptor.setId(UUID.randomUUID().toString());
            sourceDescriptor.setParentId(newId);
            sourceDescriptor.getDataDescriptor().setLocation(String.join(",", productNames));
            sourceDescriptor.setCardinality(productNames.size());
            TargetDescriptor targetDescriptor = userDSC.getTargets().get(0);
            targetDescriptor.setId(UUID.randomUUID().toString());
            targetDescriptor.setParentId(newId);
            targetDescriptor.setCardinality(productNames.size());
            targetDescriptor.addConstraint("Same cardinality");
            userDSC.setSystem(false);
            userDSC = persistenceManager.saveDataSourceComponent(userDSC);
            tag(userDSC.getId(), new ArrayList<String>() {{ add(sensor); add(dataSource); add(label); }});
        } catch (CloneNotSupportedException e) {
            throw new PersistenceException(e);
        }
        return userDSC;
    }

    @Override
    public DataSourceComponent tag(String id, List<String> tags) throws PersistenceException {
        DataSourceComponent entity = findById(id);
        if (entity == null) {
            throw new PersistenceException(String.format("Datasource with id '%s' not found", id));
        }
        if (tags != null && tags.size() > 0) {
            Set<String> existingTags = persistenceManager.getDatasourceTags().stream()
                    .map(Tag::getText).collect(Collectors.toSet());
            for (String value : tags) {
                if (!existingTags.contains(value)) {
                    persistenceManager.saveTag(new Tag(TagType.DATASOURCE, value));
                }
            }
            entity.setTags(tags);
            return update(entity);
        }
        return entity;
    }

    @Override
    public DataSourceComponent untag(String id, List<String> tags) throws PersistenceException {
        DataSourceComponent entity = findById(id);
        if (entity == null) {
            throw new PersistenceException(String.format("Datasource with id '%s' not found", id));
        }
        if (tags != null && tags.size() > 0) {
            List<String> entityTags = entity.getTags();
            if (entityTags != null) {
                for (String value : tags) {
                    entityTags.remove(value);
                }
                entity.setTags(entityTags);
                return update(entity);
            }
        }
        return entity;
    }
}
