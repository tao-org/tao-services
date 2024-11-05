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
import ro.cs.tao.component.NodeAffinity;
import ro.cs.tao.component.SourceDescriptor;
import ro.cs.tao.component.TargetDescriptor;
import ro.cs.tao.component.enums.TagType;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.datasource.DataSourceConfiguration;
import ro.cs.tao.datasource.beans.Query;
import ro.cs.tao.datasource.persistence.DataSourceComponentProvider;
import ro.cs.tao.datasource.persistence.DataSourceConfigurationProvider;
import ro.cs.tao.datasource.persistence.QueryProvider;
import ro.cs.tao.datasource.remote.FetchMode;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.eodata.enums.ProductStatus;
import ro.cs.tao.eodata.enums.SensorType;
import ro.cs.tao.persistence.EOProductProvider;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.RepositoryProvider;
import ro.cs.tao.persistence.TagProvider;
import ro.cs.tao.persistence.repository.SourceDescriptorRepository;
import ro.cs.tao.persistence.repository.TargetDescriptorRepository;
import ro.cs.tao.services.interfaces.DataSourceComponentService;
import ro.cs.tao.services.model.component.ProductSetInfo;
import ro.cs.tao.workspaces.Repository;

import java.nio.file.Paths;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service("dataSourceComponentService")
public class DataSourceComponentServiceImpl implements DataSourceComponentService {

    @Autowired
    private DataSourceComponentProvider dataSourceComponentProvider;
    @Autowired
    private EOProductProvider productProvider;
    @Autowired
    private QueryProvider queryProvider;
    @Autowired
    private TagProvider tagProvider;
    @Autowired
    private DataSourceConfigurationProvider configurationProvider;
    @Autowired
    private RepositoryProvider repositoryProvider;

    @Autowired
    private SourceDescriptorRepository sourceDescriptorRepository;
    @Autowired
    private TargetDescriptorRepository targetDescriptorRepository;

    @Override
    public DataSourceComponent findById(String id) {
        return dataSourceComponentProvider.get(id);
    }

    @Override
    public List<DataSourceComponent> getOhterDataSourceComponents(Set<String> ids) {
        return dataSourceComponentProvider.getOtherDataSourceComponents(ids);
    }

    @Override
    public List<DataSourceComponent> getBySource(String dataSourceName) throws PersistenceException {
        return dataSourceComponentProvider.getBySource(dataSourceName);
    }

    @Override
    public List<DataSourceComponent> getBySourceAndSensor(String dataSourceName, String sensor) throws PersistenceException {
        return dataSourceComponentProvider.getBySourceAndSensor(dataSourceName, sensor);
    }

    @Override
    public List<DataSourceComponent> list() {
        return dataSourceComponentProvider.list();
    }

    @Override
    public List<DataSourceComponent> list(Optional<Integer> pageNumber, Optional<Integer> pageSize, Sort sort) {
        return dataSourceComponentProvider.list(pageNumber.orElse(0), pageSize.orElse(0), sort);
    }

    @Override
    public List<DataSourceComponent> list(Iterable<String> ids) {
        return dataSourceComponentProvider.list(ids);
    }

    @Override
    public List<DataSourceComponent> getUserDataSourceComponents(String userId) {
        return dataSourceComponentProvider.getUserDataSourceComponents(userId);
    }

    @Override
    public List<DataSourceComponent> getSystemDataSourceComponents() {
        return dataSourceComponentProvider.getSystemDataSourceComponents().stream()
                .filter(c -> !"Local Database".equals(c.getDataSourceName())).collect(Collectors.toList());
    }

    @Override
    public List<Tag> getDatasourceTags() {
        return tagProvider.list(TagType.DATASOURCE);
    }

    @Override
    public DataSourceComponent save(DataSourceComponent object) {
        try {
            return dataSourceComponentProvider.save(object);
        } catch (PersistenceException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public DataSourceComponent update(DataSourceComponent object) {
        try {
            return dataSourceComponentProvider.update(object);
        } catch (PersistenceException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public void delete(String id) throws PersistenceException {
        if (id.startsWith("product-set-") || id.startsWith("stac-service-")) {
            dataSourceComponentProvider.delete(id);
        } else {
            throw new UnsupportedOperationException("Data source components cannot be deleted");
        }
    }

    @Override
    public DataSourceComponent createForProducts(List<EOProduct> products, String dataSource, Long queryId,
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
        Set<String> existingSet = new HashSet<>(productProvider.getExistingProductNames(products.stream().map(EOProduct::getName).toArray(String[]::new)));
        for (EOProduct product : products) {
            if (!existingSet.contains(product.getName())) {
                product.setProductStatus(ProductStatus.QUERIED);
                // add the referecnce to the user that performed the operation
                product.addReference(principal.getName());
                productProvider.save(product);
            }
            nameList.add(product.getName());
        }
        return createForLocations(nameList, productType, dataSource, queryId, label, principal);
    }

    @Override
    public DataSourceComponent createForLocations(List<String> productNames, String sensor, String dataSource,
                                                  Long queryId, String label, Principal principal) throws PersistenceException {
        if (productNames == null || productNames.isEmpty()) {
            throw new IllegalArgumentException("Product list is empty");
        }
        if (sensor == null || sensor.isEmpty()) {
            throw new IllegalArgumentException("[sensor] is null or empty");
        }
        if (dataSource == null || dataSource.isEmpty()) {
            throw new IllegalArgumentException("[dataSource] is null or empty");
        }
        if (principal == null) {
            throw new IllegalArgumentException("Invalid principal (null)");
        }
        DataSourceComponent userDSC = null;
        if (queryId != null) {
            userDSC = dataSourceComponentProvider.getQueryDataSourceComponent(queryId);
        }
        final Repository repository = this.repositoryProvider.getByUserAndName(principal.getName(), "LOCAL");
        if (userDSC != null) {
            SourceDescriptor sourceDescriptor = userDSC.getSources().stream().filter(s -> s.getName().equals("query")).findFirst().get();
            sourceDescriptor.getDataDescriptor().setLocation(productNames.stream()
                                                                         .map(n -> Paths.get(repository.resolve(n)).toUri().toString())
                                                                         .collect(Collectors.joining(",")));
            sourceDescriptor = sourceDescriptorRepository.save(sourceDescriptor);
            userDSC = dataSourceComponentProvider.update(userDSC);
        } else {
            DataSourceComponent systemDSC = dataSourceComponentProvider.get(sensor + "-Local Database");
            if (systemDSC == null) {
                //return null;
                userDSC = new DataSourceComponent(sensor, dataSource);
                userDSC.setFetchMode(FetchMode.OVERWRITE);
                userDSC.setVersion("1.0");
                userDSC.setDescription(sensor + " from " + dataSource);
                userDSC.setAuthors("TAO Team");
                userDSC.setCopyright("(C) TAO Team");
                userDSC.setNodeAffinity(NodeAffinity.Any);
                //dataSourceComponentProvider.save(systemDSC);
            }
            try {
                if (systemDSC != null) {
                    userDSC = systemDSC.clone();
                }
                if (userDSC != null) {
                    final List<SourceDescriptor> sources = new ArrayList<>(userDSC.getSources());
                    final List<TargetDescriptor> targets = new ArrayList<>(userDSC.getTargets());
                    userDSC.setSources(null);
                    userDSC.setTargets(null);
                    LocalDateTime time = LocalDateTime.now();
                    /*String newId = String.join("-", sensor, dataSource, principal.getName(),
                            time.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));*/
                    final String newId = String.join("-", "product", "set", principal.getName(),
                                                     time.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
                    userDSC.setId(newId);
                    userDSC.setLabel(label != null && !label.isEmpty() ?
                            label : String.join("-", sensor, dataSource, principal.getName()) +
                            " [customized on " + time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "]");
                    for (SourceDescriptor sourceDescriptor : sources) {
                        sourceDescriptor = sourceDescriptor.clone();
                        sourceDescriptor.setId(UUID.randomUUID().toString());
                        sourceDescriptor.setParentId(newId);
                        if (sourceDescriptor.getName().equals("query")) {
                            sourceDescriptor.getDataDescriptor().setLocation(String.join(",", productNames));
                            sourceDescriptor.setCardinality(productNames.size());
                        } else {
                            sourceDescriptor.setCardinality(0);
                        }
                        userDSC.addSource(sourceDescriptorRepository.save(sourceDescriptor));
                    }
                    TargetDescriptor targetDescriptor = targets.get(0);
                    targetDescriptor.setId(UUID.randomUUID().toString());
                    targetDescriptor.setParentId(newId);
                    targetDescriptor.setCardinality(productNames.size());
                    targetDescriptor.addConstraint("Same cardinality");
                    userDSC.addTarget(targetDescriptorRepository.save(targetDescriptor));
                    userDSC.setSystem(false);
                    userDSC = dataSourceComponentProvider.save(userDSC);
                    tag(userDSC.getId(), new ArrayList<String>() {{
                        add(sensor);
                        add(dataSource);
                        add(label);
                    }});
                    if (queryId != null) {
                        Query query = queryProvider.get(queryId);
                        query.setComponentId(userDSC.getId());
                        queryProvider.save(query);
                    }
                }
            } catch (CloneNotSupportedException e) {
                throw new PersistenceException(e);
            }
        }
        return userDSC;
    }

    @Override
    public DataSourceComponent createForLocations(List<String> productPaths, String label, Principal principal) throws PersistenceException {
        if (productPaths == null || productPaths.isEmpty()) {
            throw new IllegalArgumentException("Product list is empty");
        }
        if (principal == null) {
            throw new IllegalArgumentException("Invalid principal (null)");
        }
        final Repository repository = this.repositoryProvider.getByUserAndName(principal.getName(), "LOCAL");
        final List<String> list = productPaths.stream()
                                              .map(n -> Paths.get(repository.resolve(n)).toUri().toString())
                                              .collect(Collectors.toList());
        List<EOProduct> products = this.productProvider.getByLocation(list.toArray(new String[0]));
        final String sensor = products != null && products.size() == productPaths.size()
                              && products.stream().map(EOProduct::getProductType).distinct().count() == 1
                ? products.get(0).getProductType() : "n/a";
        DataSourceComponent userDSC = new DataSourceComponent(sensor, "Local");
        userDSC.setFetchMode(FetchMode.OVERWRITE);
        userDSC.setVersion("1.0");
        userDSC.setDescription(label + " (local product set)");
        userDSC.setAuthors("AVL Team");
        userDSC.setCopyright("(C) AVL Team");
        userDSC.setNodeAffinity(NodeAffinity.Any);
        LocalDateTime time = LocalDateTime.now();
        final String newId = String.join("-", "product", "set", principal.getName(),
                                         time.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        userDSC.setId(newId);
        userDSC.getSources().forEach(s -> s.setParentId(newId));
        userDSC.getTargets().forEach(t -> t.setParentId(newId));
        userDSC.setLabel(label != null && !label.isEmpty() ? label : newId);
        SourceDescriptor sourceDescriptor = userDSC.getSources().stream().filter(s -> s.getName().equals(DataSourceComponent.QUERY_PARAMETER)).findFirst().get();
        sourceDescriptor.getDataDescriptor().setSensorType(SensorType.UNKNOWN);
        sourceDescriptor.getDataDescriptor().setLocation(String.join(",", list));
        sourceDescriptor.setCardinality(productPaths.size());
        sourceDescriptorRepository.save(sourceDescriptor);
        TargetDescriptor targetDescriptor = userDSC.getTargets().get(0);
        targetDescriptor.setId(UUID.randomUUID().toString());
        targetDescriptor.setParentId(newId);
        targetDescriptor.setCardinality(productPaths.size());
        //targetDescriptor.addConstraint("Same cardinality");
        targetDescriptorRepository.save(targetDescriptor);
        userDSC.setSystem(false);
        userDSC = dataSourceComponentProvider.save(userDSC);
        tag(userDSC.getId(), new ArrayList<>() {{
            add(label);
        }});
        return userDSC;
    }

    @Override
    public DataSourceComponent tag(String id, List<String> tags) throws PersistenceException {
        DataSourceComponent entity = findById(id);
        if (entity == null) {
            throw new PersistenceException(String.format("Datasource with id '%s' not found", id));
        }
        if (tags != null && !tags.isEmpty()) {
            Set<String> existingTags = tagProvider.list(TagType.DATASOURCE).stream()
                    .map(Tag::getText).collect(Collectors.toSet());
            for (String value : tags) {
                if (!existingTags.contains(value)) {
                    tagProvider.save(new Tag(TagType.DATASOURCE, value));
                }
            }
            entity.setTags(tags);
            return update(entity);
        }
        return entity;
    }

    @Override
    public DataSourceComponent unTag(String id, List<String> tags) throws PersistenceException {
        DataSourceComponent entity = findById(id);
        if (entity == null) {
            throw new PersistenceException(String.format("Datasource with id '%s' not found", id));
        }
        if (tags != null && !tags.isEmpty()) {
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

    @Override
    public DataSourceConfiguration getConfiguration(String dataSourceId) throws PersistenceException {
        final DataSourceComponent entity = findById(dataSourceId);
        if (entity == null) {
            throw new PersistenceException(String.format("Datasource with id '%s' not found", dataSourceId));
        }
        return this.configurationProvider.get(dataSourceId);
    }

    @Override
    public void saveConfiguration(DataSourceConfiguration configuration) throws PersistenceException {
        final DataSourceComponent entity = findById(configuration.getId());
        if (entity == null) {
            throw new PersistenceException(String.format("Datasource with id '%s' not found", configuration.getId()));
        }
        this.configurationProvider.save(configuration);
    }

    public void updateConfiguration(DataSourceConfiguration configuration) throws PersistenceException {
        final DataSourceComponent entity = findById(configuration.getId());
        if (entity == null) {
            throw new PersistenceException(String.format("Datasource with id '%s' not found", configuration.getId()));
        }
        this.configurationProvider.update(configuration);
    }

    @Override
    public List<DataSourceComponent> getProductSets(String userId) throws PersistenceException {
        return this.dataSourceComponentProvider.getProductSets(userId);
    }

    @Override
    public DataSourceComponent updateProductSet(ProductSetInfo productSet) throws PersistenceException {
        DataSourceComponent component = this.dataSourceComponentProvider.get(productSet.getId());
        component.setLabel(productSet.getLabel());
        component.setDescription(productSet.getDescription());
        component.setSensorName(productSet.getType());
        component.setTags(productSet.getTags());
        SourceDescriptor descriptor = component.getSources().stream().filter(s -> DataSourceComponent.QUERY_PARAMETER.equals(s.getName())).findFirst().orElse(null);
        if (descriptor != null) {
            List<String> products = productSet.getProducts();
            descriptor.getDataDescriptor().setLocation(products != null ? String.join(",", products) : null);
            descriptor.setCardinality(products != null ? products.size() : 1);
            this.sourceDescriptorRepository.save(descriptor);
            TargetDescriptor targetDescriptor = component.getTargets().stream().filter(t -> DataSourceComponent.RESULTS_PARAMETER.equals(t.getName())).findFirst().get();
            targetDescriptor.setCardinality(descriptor.getCardinality());
            this.targetDescriptorRepository.save(targetDescriptor);
        }
        return this.dataSourceComponentProvider.update(component);
    }
}
