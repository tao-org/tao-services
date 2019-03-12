package ro.cs.tao.services.entity.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.cs.tao.Tag;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.datasource.DataSourceComponentGroup;
import ro.cs.tao.datasource.beans.Query;
import ro.cs.tao.eodata.EOProduct;
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
import java.util.function.Predicate;
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
            if (groupId == null) { // new group
                group = createGroup(user.getName(), groupLabel);
                for (Triple<Query, List<EOProduct>, String> triple : groupQueries) {
                    Query q = triple.getKeyOne();
                    q.setUserId(user.getName());
                    addQueryToGroup(group, q, triple.getKeyTwo());
                }
                group = persistenceManager.updateDataSourceComponentGroup(group);
            } else { // existing group
                group = findById(groupId);
                group.setLabel(groupLabel);
                Set<Query> querySet = group.getDataSourceQueries();
                for (Triple<Query, List<EOProduct>, String> triple : groupQueries) {
                    Query incomingQuery = triple.getKeyOne();
                    Query dbQuery = querySet.stream().filter(q -> Objects.equals(q.getId(), incomingQuery.getId())).findFirst().orElse(null);
                    if (dbQuery == null) { // new query
                        incomingQuery.setUserId(user.getName());
                        addQueryToGroup(group, incomingQuery, triple.getKeyTwo());
                    } else { // existing query
                        dbQuery = updateQuery(incomingQuery, dbQuery, user.getName());
                        String componentId = triple.getKeyThree();
                        List<EOProduct> products = filterProducts(componentId, triple.getKeyTwo());
                        persister.handle(products);
                        // remove from temporary set
                        querySet.remove(dbQuery);
                    }
                }
                // remove previously persisted queries that are no longer part of this group
                // to prevent a ConcurrentModificationException thrown by Hibernate, wrap queries in an array
                Query[] obsoleteQueries = querySet.toArray(new Query[0]);
                for (Query query : obsoleteQueries) {
                    DataSourceComponent component = group.getDataSourceComponents().stream()
                            .filter(c -> c.getId().equals(query.getComponentId())).findFirst().orElse(null);
                    if (component != null) {
                        try {
                            String productNameList = component.getSources().get(0).getDataDescriptor().getLocation();
                            if (productNameList != null) {
                                String[] productNames = productNameList.split(",");
                                for (String productName : productNames) {
                                    persistenceManager.deleteIfNotReferenced(component.getId(), productName);
                                }
                            }
                            persistenceManager.removeQuery(query.getId());
                            group.removeQuery(query.getId());
                            persistenceManager.deleteDataSourceComponent(component.getId());
                            group.removeDataSourceComponent(component);
                        } catch (PersistenceException e) {
                            logger.severe(String.format("Cannot remove data source component %s. Reason: %s",
                                                        component.getId(), e.getMessage()));
                        }
                    }
                }
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

    private DataSourceComponentGroup createGroup(String userName, String label) throws PersistenceException {
        DataSourceComponentGroup group = new DataSourceComponentGroup();
        group.setId(UUID.randomUUID().toString());
        group.setUserName(userName);
        group.setLabel(label);
        group.setVersion("1.0");
        group.setDescription(group.getLabel());
        group.setAuthors(userName);
        group.setCopyright("(C) " + userName);
        group.setNodeAffinity("Any");
        return persistenceManager.saveDataSourceComponentGroup(group);
    }

    private void addQueryToGroup(DataSourceComponentGroup group, Query query, List<EOProduct> products) throws PersistenceException {
        DataSourceComponent component =
                dataSourceComponentService.createForProductNames(products.stream().map(EOProduct::getName).collect(Collectors.toList()),
                                                                 query.getSensor(), query.getDataSource(), query.getLabel(),
                                                                 SessionStore.currentContext().getPrincipal());
        products = filterProducts(component.getId(), products);
        ProductPersister persister = new ProductPersister();
        persister.handle(products);
        query.setComponentId(component.getId());
        query = persistenceManager.saveQuery(query);
        group.addDataSourceComponent(component);
        group.addQuery(query, component.getSources().get(0).getId());
    }

    private List<EOProduct> filterProducts(String componentId, List<EOProduct> products) {
        String[] names = products.stream().map(EOProduct::getName).toArray(String[]::new);
        Map<String, EOProduct> existingProducts = persistenceManager.getProductsByNames(names).stream()
                .collect(Collectors.toMap(EOProduct::getName, Function.identity()));
        List<EOProduct> results = new ArrayList<>(products);
        for (EOProduct product : products) {
            // remove products that are already in database
            if (existingProducts.containsKey(product.getName())) {
                results.remove(product);
            }
        }
        if (results.size() > 0) {
            List<String> candidatesToRemove = products.stream()
                    .filter(not(new HashSet<>(results)::contains))
                    .map(EOProduct::getName)
                    .collect(Collectors.toList());
            ;
            for (String product : candidatesToRemove) {
                persistenceManager.deleteIfNotReferenced(componentId, product);
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
        return persistenceManager.saveQuery(dbQuery);
    }

    private static <T> Predicate<T> not(Predicate<T> predicate) {
        return predicate.negate();
    }
}
