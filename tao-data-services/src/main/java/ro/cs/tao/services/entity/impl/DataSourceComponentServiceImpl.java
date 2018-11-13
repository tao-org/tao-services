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
import java.util.Set;
import java.util.UUID;
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
    public DataSourceComponent createForProducts(List<EOProduct> products, String label, Principal principal) throws PersistenceException {
        if (products == null || products.isEmpty()) {
            throw new PersistenceException("Product list is empty");
        }
        if (principal == null) {
            throw new PersistenceException("Invalid principal (null)");
        }
        boolean hasLabel = label != null && !label.isEmpty();
        String productType = products.stream()
                                     .filter(p -> p.getProductType() != null)
                                     .map(EOProduct::getProductType)
                                     .findFirst().orElse(null);
        if (productType == null) {
            throw new PersistenceException("Invalid product type (null)");
        }
        String systemDSCId = productType + "-Local Database";
        DataSourceComponent systemDSC = persistenceManager.getDataSourceInstance(systemDSCId);
        if (systemDSC == null) {
            throw new PersistenceException(String.format("No system datasource component with id = '%s' was found",
                                                         systemDSCId));
        }
        if (persistenceManager.getDataSourceComponentByLabel(label) != null) {
            throw new PersistenceException(String.format("A component with the label '%s' already exists", label));
        }
        DataSourceComponent userDSC;
        try {
            userDSC = systemDSC.clone();
            LocalDateTime time = LocalDateTime.now();
            String newId = hasLabel ? label :
                    systemDSC.getId() + "-" + principal.getName() + "-" + time.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            userDSC.setId(newId);
            userDSC.setLabel(hasLabel ? label :
                                     systemDSC.getLabel() + " (customized on " + time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + ")");
            List<String> nameList = products.stream().map(EOData::getName).collect(Collectors.toList());
            SourceDescriptor sourceDescriptor = userDSC.getSources().get(0);
            sourceDescriptor.setId(UUID.randomUUID().toString());
            sourceDescriptor.setParentId(newId);
            sourceDescriptor.getDataDescriptor().setLocation(String.join(",", nameList));
            sourceDescriptor.setCardinality(products.size());
            TargetDescriptor targetDescriptor = userDSC.getTargets().get(0);
            targetDescriptor.setId(UUID.randomUUID().toString());
            targetDescriptor.setParentId(newId);
            targetDescriptor.setCardinality(products.size());
            targetDescriptor.addConstraint("Same cardinality");
            userDSC.setSystem(false);
            userDSC.setLabel(label);
            userDSC = persistenceManager.saveDataSourceComponent(userDSC);
        } catch (CloneNotSupportedException e) {
            throw new PersistenceException(e);
        }
        return userDSC;
    }

    @Override
    public DataSourceComponent createForProductNames(List<String> productNames, String productType,
                                                     String label, Principal principal) throws PersistenceException {
        if (productNames == null || productNames.isEmpty() || productType == null || productType.isEmpty() ||
                label == null || label.isEmpty() || principal == null) {
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
            String newId = systemDSC.getId() + "-" + principal.getName() + "-" + time.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            userDSC.setId(newId);
            userDSC.setLabel(systemDSC.getLabel() + " (customized on " + time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + ")");
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
            userDSC.setLabel(label);
            userDSC = persistenceManager.saveDataSourceComponent(userDSC);
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
