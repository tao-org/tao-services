package ro.cs.tao.services.entity.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.cs.tao.Tag;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.datasource.DataSourceComponentGroup;
import ro.cs.tao.datasource.beans.Query;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.eodata.enums.ProductStatus;
import ro.cs.tao.execution.local.ProductPersister;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.interfaces.DataSourceComponentService;
import ro.cs.tao.services.interfaces.DataSourceGroupService;
import ro.cs.tao.utils.Triple;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service("dataSourceGroupService")
public class DataSourceGroupServiceImpl implements DataSourceGroupService {

    @Autowired
    private PersistenceManager persistenceManager;
    @Autowired
    private DataSourceComponentService dataSourceComponentService;

    private Logger logger = Logger.getLogger(DataSourceGroupService.class.getName());

    @Override
    public DataSourceComponentGroup findById(String id) throws PersistenceException {
        return persistenceManager.getDataSourceComponentGroup(id);
    }

    @Override
    public List<DataSourceComponentGroup> list() {
        return persistenceManager.getDataSourceComponentGroups();
    }

    @Override
    public List<DataSourceComponentGroup> list(Iterable<String> ids) {
        return persistenceManager.getDataSourceComponentGroups(ids);
    }

    @Override
    @Transactional
    public DataSourceComponentGroup save(DataSourceComponentGroup dataSourceGroup) {
        if (dataSourceGroup == null) {
            throw new NullPointerException("[dataSourceGroup]");
        }
        try {
            return persistenceManager.saveDataSourceComponentGroup(dataSourceGroup);
        } catch (PersistenceException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    @Transactional
    public DataSourceComponentGroup update(DataSourceComponentGroup dataSourceGroup) throws PersistenceException {
        if (dataSourceGroup == null) {
            throw new NullPointerException("[dataSourceGroup]");
        }
        return persistenceManager.updateDataSourceComponentGroup(dataSourceGroup);
    }

    @Override
    public void delete(String id) throws PersistenceException {
        persistenceManager.deleteDataSourceComponentGroup(id);
    }

    @Override
    public List<DataSourceComponentGroup> getUserDataSourceComponentGroups(String userName) {
        return persistenceManager.getUserDataSourceComponentGroup(userName);
    }

    @Override
    public List<DataSourceComponentGroup> getDataSourceComponentGroups() {
        return persistenceManager.getDataSourceComponentGroups();
    }

    @Override
    @Transactional(rollbackFor = { PersistenceException.class, Exception.class })
    public DataSourceComponentGroup saveDataSourceGroup(String groupId, String groupLabel,
                                                        List<Triple<Query, List<EOProduct>, String>> groupQueries,
                                                        Principal user) throws PersistenceException {
        DataSourceComponentGroup group;
        try {
            ProductPersister persister = new ProductPersister();
            if (groupId == null) {
                group = new DataSourceComponentGroup();
                group.setId(UUID.randomUUID().toString());
                group.setUserName(user.getName());
                group.setLabel(groupLabel);
                group.setVersion("1.0");
                group.setDescription(group.getLabel());
                group.setAuthors(user.getName());
                group.setCopyright("(C) " + user.getName());
                group.setNodeAffinity("Any");
                group = persistenceManager.saveDataSourceComponentGroup(group);
                for (Triple<Query, List<EOProduct>, String> triple : groupQueries) {
                    Query q = triple.getKeyOne();
                    List<EOProduct> products = triple.getKeyTwo();
                    DataSourceComponent component =
                            dataSourceComponentService.createForProductNames(products.stream().map(EOProduct::getName).collect(Collectors.toList()),
                                                                             q.getSensor(), q.getDataSource(), q.getLabel(),
                                                                             SessionStore.currentContext().getPrincipal());
                    products = filterProducts(component.getId(), products);
                    persister.handle(products);
                    q.setUserId(user.getName());
                    q.setComponentId(component.getId());
                    q = persistenceManager.saveQuery(q);
                    group.addDataSourceComponent(component);
                    group.addQuery(q, component.getSources().get(0).getId());
                }
                group = persistenceManager.updateDataSourceComponentGroup(group);
            } else {
                group = findById(groupId);
                Set<Query> querySet = group.getDataSourceQueries();
                for (Triple<Query, List<EOProduct>, String> triple : groupQueries) {
                    Query incomingQuery = triple.getKeyOne();
                    Query dbQuery = querySet.stream().filter(q -> Objects.equals(q.getId(), incomingQuery.getId())).findFirst().orElse(null);
                    dbQuery = updateQuery(incomingQuery, dbQuery, user.getName());
                    if (triple.getKeyThree() == null) {
                        List<EOProduct> products = triple.getKeyTwo();
                        DataSourceComponent component =
                                dataSourceComponentService.createForProductNames(products.stream().map(EOProduct::getName).collect(Collectors.toList()),
                                                                                 incomingQuery.getSensor(),
                                                                                 incomingQuery.getDataSource(), incomingQuery.getLabel(),
                                                                                 user);
                        products = filterProducts(component.getId(), products);
                        persister.handle(products);
                        group.addDataSourceComponent(component);
                        dbQuery.setComponentId(component.getId());
                        group.addQuery(dbQuery, component.getSources().get(0).getId());
                    }
                    persistenceManager.saveQuery(dbQuery);
                }
                List<Triple<Query, List<EOProduct>, String> > notExisting = groupQueries.stream()
                        .filter(i -> querySet.stream().noneMatch(q -> q.getId().equals(i.getKeyOne().getId())))
                        .collect(Collectors.toList());
                for (Triple<Query, List<EOProduct>, String>  triple : notExisting) {
                    Query q = triple.getKeyOne();
                    List<EOProduct> products = triple.getKeyTwo();
                    DataSourceComponent component =
                            dataSourceComponentService.createForProductNames(products.stream().map(EOProduct::getName).collect(Collectors.toList()),
                                                                             q.getSensor(),
                                                                             q.getDataSource(), q.getLabel(),
                                                                             user);
                    products = filterProducts(component.getId(), products);
                    persister.handle(products);
                    q.setComponentId(component.getId());
                    q.setUserId(user.getName());
                    q = persistenceManager.saveQuery(q);
                    group.addDataSourceComponent(component);
                    group.addQuery(q, component.getSources().get(0).getId());
                }
                final DataSourceComponentGroup finalGroup = group;
                querySet.stream().filter(q -> groupQueries.stream().noneMatch(i -> q.getId().equals(i.getKeyOne().getId())))
                        .forEach(q -> {
                            DataSourceComponent component = finalGroup.getDataSourceComponents().stream()
                                    .filter(c -> c.getId().equals(q.getComponentId())).findFirst().orElse(null);
                            if (component != null) {
                                try {
                                    String nameList = component.getSources().get(0).getDataDescriptor().getLocation();
                                    if (nameList != null) {
                                        String[] names = nameList.split(",");
                                        for (String name : names) {
                                            if (persistenceManager.getOtherProductReferences(component.getId(), name) == 0) {
                                                persistenceManager.removeProduct(name);
                                            }
                                        }
                                    }
                                    persistenceManager.deleteDataSourceComponent(component.getId());
                                } catch (PersistenceException e) {
                                    logger.severe(String.format("Cannot remove data source component %s. Reason: %s",
                                                                component.getId(), e.getMessage()));
                                }
                            }
                            persistenceManager.removeQuery(q.getId());
                        });
                group = persistenceManager.updateDataSourceComponentGroup(group);
            }
        } catch (Exception ex) {
            throw new PersistenceException(ex);
        }
        return group;
    }

    @Override
    public List<Tag> getDatasourceGroupTags() {
        return persistenceManager.getDatasourceTags();
    }

    private List<EOProduct> filterProducts(String componentId, List<EOProduct> products) {
        String[] names = products.stream().map(EOProduct::getName).toArray(String[]::new);
        Map<String, EOProduct> existing = persistenceManager.getProductsByNames(names).stream()
                .collect(Collectors.toMap(EOProduct::getName, Function.identity()));
        List<EOProduct> results = new ArrayList<>(products);
        EOProduct existingProduct;
        for (EOProduct product : products) {
            if (persistenceManager.getOtherProductReferences(componentId, product.getName()) > 0) {
                results.remove(product);
            } else if ((existingProduct = existing.get(product.getName())) != null &&
                    existingProduct.getProductStatus() != ProductStatus.QUERIED) {
                results.remove(product);
            }
        }
        products.removeAll(results);
        for (EOProduct product : products) {
            if (persistenceManager.getOtherProductReferences(componentId, product.getName()) > 0) {
                try {
                    persistenceManager.remove(product);
                } catch (PersistenceException ex) {
                    logger.severe(String.format("Cannot remove product %s. Reason: %s",
                                                product.getName(), ex.getMessage()));
                }
            }
        }
        return results;
    }

    private Query updateQuery(Query incomingQuery, Query dbQuery, String userName) throws PersistenceException {
        if (dbQuery != null) {
            dbQuery.setLabel(incomingQuery.getLabel());
            dbQuery.setLimit(incomingQuery.getLimit());
            dbQuery.setModified(LocalDateTime.now());
            dbQuery.setPageNumber(incomingQuery.getPageNumber());
            dbQuery.setPageSize(incomingQuery.getPageSize());
            dbQuery.setPassword(incomingQuery.getPassword());
            dbQuery.setSensor(incomingQuery.getSensor());
            dbQuery.setUser(incomingQuery.getUser());
            dbQuery.setUserId(userName);
            Map<String, String> parameters = dbQuery.getValues();
            if (parameters == null) {
                parameters = new HashMap<>();
            }
            parameters.putAll(incomingQuery.getValues());
            dbQuery.setValues(parameters);
        } else {
            dbQuery = incomingQuery;
        }
        return dbQuery;
    }
}
