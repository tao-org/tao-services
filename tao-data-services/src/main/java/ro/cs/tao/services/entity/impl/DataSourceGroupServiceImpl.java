package ro.cs.tao.services.entity.impl;

import org.apache.commons.collections.CollectionUtils;
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
import ro.cs.tao.utils.Tuple;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
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
        if (id == null || id.isEmpty()) {
            throw new NullPointerException("[id]");
        }
        DataSourceComponentGroup group = persistenceManager.getDataSourceComponentGroup(id);
        Query[] queries = group.getDataSourceQueries().toArray(new Query[0]);
        DataSourceComponent[] components = group.getDataSourceComponents().toArray(new DataSourceComponent[0]);
        for (Query query : queries) {
            group.removeQuery(query.getId());
            persistenceManager.removeQuery(query);
        }
        for (DataSourceComponent component : components) {
            group.removeDataSourceComponent(component);
            String productNameList = component.getSources().get(0).getDataDescriptor().getLocation();
            if (productNameList != null) {
                String[] productNames = productNameList.split(",");
                for (String productName : productNames) {
                    persistenceManager.deleteIfNotReferenced(component.getId(), productName);
                }
            }
            persistenceManager.deleteDataSourceComponent(component.getId());
        }
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
                                                        List<Tuple<Query, List<EOProduct>>> groupQueries,
                                                        Principal user) throws PersistenceException {
        if (groupLabel == null || groupLabel.isEmpty()) {
            throw new PersistenceException("Group label is required");
        }
        DataSourceComponentGroup group;
        try {
            if (groupId == null) { // new group
                group = createGroup(user.getName(), groupLabel);
                for (Tuple<Query, List<EOProduct>> tuple : groupQueries) {
                    Query q = tuple.getKeyOne();
                    q.setUserId(user.getName());
                    addQueryToGroup(group, q, tuple.getKeyTwo());
                }
                group = persistenceManager.updateDataSourceComponentGroup(group);
            } else { // existing group
                group = findById(groupId);
                if (group == null) {
                    throw new PersistenceException(String.format("Non-existent group [id=%s]", groupId));
                }
                group.setLabel(groupLabel);
                Set<Query> querySet = group.getDataSourceQueries();
                Set<Long> updatedQueries = new HashSet<>();
                for (Tuple<Query, List<EOProduct>> tuple : groupQueries) {
                    Query incomingQuery = tuple.getKeyOne();
                    Query dbQuery = querySet.stream().filter(q -> Objects.equals(q.getId(), incomingQuery.getId())).findFirst().orElse(null);
                    List<EOProduct> productList = tuple.getKeyTwo();
                    if (dbQuery == null) { // new query
                        if (incomingQuery.getComponentId() != null) {
                            logger.warning(String.format("Query [label=%s] seems to be new, but it has an associated component [id=%s]. A new component will be created instead.",
                                                         incomingQuery.getLabel(), incomingQuery.getComponentId()));
                        }
                        incomingQuery.setUserId(user.getName());
                        dbQuery = addQueryToGroup(group, incomingQuery, productList);
                    } else { // existing query
                        dbQuery = updateQuery(incomingQuery, dbQuery, user.getName());
                        String componentId = dbQuery.getComponentId();
                        if (componentId == null || componentId.isEmpty()) {
                            String message = String.format("Query [id=%s, label=%s] was already persisted, but its associated component Id is empty.",
                                          dbQuery.getId(), dbQuery.getLabel());
                            logger.warning(message);
                            throw new PersistenceException(message);
                        }
                        DataSourceComponent component = group.getDataSourceComponents().stream().filter(c -> c.getId().equals(componentId)).findFirst().orElse(null);
                        if (component == null) {
                            throw new PersistenceException(String.format("Component [id=%s] was not found in the group[id=%s]",
                                                                         componentId, group.getId()));
                        }
                        String previousProductList = component.getSources().get(0).getDataDescriptor().getLocation();
                        Set<String> previousNames = null;
                        if (previousProductList != null) {
                            previousNames = Arrays.stream(previousProductList.split(",")).collect(Collectors.toSet());
                        }
                        updateProducts(componentId, productList, previousNames);
                        component.getSources().get(0).getDataDescriptor().setLocation(productList.stream().map(EOProduct::getName).collect(Collectors.joining(",")));
                        persistenceManager.updateDataSourceComponent(component);
                    }
                    updatedQueries.add(dbQuery.getId());
                }
                // remove previously persisted queries that are no longer part of this group
                // to prevent a ConcurrentModificationException thrown by Hibernate, wrap queries in an array
                Query[] obsoleteQueries = querySet.stream().filter(q -> !updatedQueries.contains(q.getId())).toArray(Query[]::new);
                for (Query query : obsoleteQueries) {
                    DataSourceComponent component = group.getDataSourceComponents().stream()
                            .filter(c -> c.getId().equals(query.getComponentId())).findFirst().orElse(null);
                    if (component != null) {
                        try {
                            String productNameList = component.getSources().get(0).getDataDescriptor().getLocation();
                            if (productNameList != null) {
                                String[] productNames = productNameList.split(",");
                                for (String productName : productNames) {
                                    logger.finest(String.format("Product [%s] will be removed from database if not already downloaded or referenced by another component",
                                                                productName));
                                    persistenceManager.deleteIfNotReferenced(component.getId(), productName);
                                }
                            }
                            logger.finest(String.format("Query [id=%s, label=%s] will be removed from the group [id=%s]",
                                                        query.getId(), query.getLabel(), group.getId()));
                            persistenceManager.removeQuery(query.getId());
                            group.removeQuery(query.getId());
                            logger.finest(String.format("Data source component [id=%s] will be removed from the group [id=%s]",
                                                        component.getId(), group.getId()));
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
        group = persistenceManager.saveDataSourceComponentGroup(group);
        logger.finest(String.format("Created data source group [userName=%s, label=%s, id=%s",
                                    userName, label, group.getId()));
        return group;
    }

    private Query addQueryToGroup(DataSourceComponentGroup group, Query query, List<EOProduct> products) throws PersistenceException {
        DataSourceComponent component =
                dataSourceComponentService.createForProductNames(products.stream().map(EOProduct::getName).collect(Collectors.toList()),
                                                                 query.getSensor(), query.getDataSource(), query.getId(), query.getLabel(),
                                                                 SessionStore.currentContext().getPrincipal());
        products = updateProducts(component.getId(), products, null);
        ProductPersister persister = new ProductPersister();
        persister.handle(products);
        query.setComponentId(component.getId());
        query = persistenceManager.saveQuery(query);
        group.addDataSourceComponent(component);
        group.addQuery(query, component.getSources().get(0).getId());
        logger.finest(String.format("Query [id=%s, label=%s] added to data source group [id=%s]",
                                    query.getId(), query.getLabel(), group.getId()));
        return query;
    }

    private List<EOProduct> updateProducts(String componentId, List<EOProduct> products, Set<String> previousNames) {
        String[] names = products.stream().map(EOProduct::getName).toArray(String[]::new);
        Set<String> existingProducts = persistenceManager.getProductsByNames(names).stream()
                                                         .map(EOProduct::getName).collect(Collectors.toSet());
        List<EOProduct> candidatesToSave = new ArrayList<>(products);
        // remove from the list the products that are already in database
        candidatesToSave.removeIf(p -> existingProducts.contains(p.getName()));
        if (previousNames != null) {
            Collection<String> candidatesToRemove = CollectionUtils.subtract(previousNames,
                                                                             products.stream().map(EOProduct::getName).collect(Collectors.toSet()));
            for (String product : candidatesToRemove) {
                logger.finest(String.format("Product [%s] will be removed from database if not already downloaded or referenced by another component",
                                            product));
                persistenceManager.deleteIfNotReferenced(componentId, product);
            }
        }
        if (candidatesToSave.size() > 0) {
            ProductPersister persister = new ProductPersister();
            persister.handle(candidatesToSave);
        }
        return candidatesToSave;
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
            dbQuery.setComponentId(incomingQuery.getComponentId());
            Map<String, String> parameters = dbQuery.getValues();
            if (parameters == null) {
                parameters = new HashMap<>();
            }
            parameters.putAll(incomingQuery.getValues());
            dbQuery.setValues(parameters);
        } else {
            dbQuery = incomingQuery;
        }
        dbQuery = persistenceManager.saveQuery(dbQuery);
        logger.finest(String.format("Query [id=%s, label=%s] was found in database and updated",
                                    dbQuery.getId(), dbQuery.getLabel()));
        return dbQuery;
    }

    private static <T> Predicate<T> not(Predicate<T> predicate) {
        return predicate.negate();
    }
}
