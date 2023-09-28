package ro.cs.tao.services.entity.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.cs.tao.Tag;
import ro.cs.tao.component.enums.TagType;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.datasource.DataSourceComponentGroup;
import ro.cs.tao.datasource.beans.Query;
import ro.cs.tao.datasource.persistence.DataSourceComponentGroupProvider;
import ro.cs.tao.datasource.persistence.QueryProvider;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.execution.local.ProductPersister;
import ro.cs.tao.persistence.EOProductProvider;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.ProcessingComponentProvider;
import ro.cs.tao.persistence.TagProvider;
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
    private DataSourceComponentGroupProvider groupProvider;
    @Autowired
    private TagProvider tagProvider;
    @Autowired
    private QueryProvider queryProvider;
    @Autowired
    private ProcessingComponentProvider componentProvider;
    @Autowired
    private EOProductProvider productProvider;
    @Autowired
    private DataSourceComponentService dataSourceComponentService;

    private final Logger logger = Logger.getLogger(DataSourceGroupService.class.getName());

    @Override
    public DataSourceComponentGroup findById(String id) {
        return groupProvider.get(id);
    }

    @Override
    public List<DataSourceComponentGroup> list() {
        return groupProvider.list();
    }

    @Override
    public List<DataSourceComponentGroup> list(Iterable<String> ids) {
        return groupProvider.list(ids);
    }

    @Override
    @Transactional
    public DataSourceComponentGroup save(DataSourceComponentGroup dataSourceGroup) {
        if (dataSourceGroup == null) {
            throw new NullPointerException("[dataSourceGroup]");
        }
        try {
            return groupProvider.save(dataSourceGroup);
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
        return groupProvider.update(dataSourceGroup);
    }

    @Override
    public void delete(String id) throws PersistenceException {
        if (id == null || id.isEmpty()) {
            throw new NullPointerException("[id]");
        }
        DataSourceComponentGroup group = groupProvider.get(id);
        Query[] queries = group.getDataSourceQueries().toArray(new Query[0]);
        DataSourceComponent[] components = group.getDataSourceComponents().toArray(new DataSourceComponent[0]);
        for (Query query : queries) {
            group.removeQuery(query.getId());
            queryProvider.delete(query);
        }
        for (DataSourceComponent component : components) {
            group.removeDataSourceComponent(component);
            String productNameList = component.getSources().get(0).getDataDescriptor().getLocation();
            if (productNameList != null) {
                String[] productNames = productNameList.split(",");
                for (String productName : productNames) {
                    productProvider.deleteIfNotReferenced(component.getId(), productName);
                }
            }
            dataSourceComponentService.delete(component.getId());
        }
        groupProvider.delete(id);
    }

    @Override
    public List<DataSourceComponentGroup> getUserDataSourceComponentGroups(String userId) {
        return groupProvider.listByUser(userId);
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
                group = groupProvider.update(group);
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
                        dataSourceComponentService.update(component);
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
                                    productProvider.deleteIfNotReferenced(component.getId(), productName);
                                }
                            }
                            logger.finest(String.format("Query [id=%s, label=%s] will be removed from the group [id=%s]",
                                                        query.getId(), query.getLabel(), group.getId()));
                            queryProvider.delete(query.getId());
                            group.removeQuery(query.getId());
                            logger.finest(String.format("Data source component [id=%s] will be removed from the group [id=%s]",
                                                        component.getId(), group.getId()));
                            dataSourceComponentService.delete(component.getId());
                            group.removeDataSourceComponent(component);
                        } catch (PersistenceException e) {
                            logger.severe(String.format("Cannot remove data source component %s. Reason: %s",
                                                        component.getId(), e.getMessage()));
                        }
                    }
                }
                group = groupProvider.update(group);
            }
        } catch (Exception ex) {
            throw new PersistenceException(ex);
        }
        return group;
    }

    @Override
    public List<Tag> getDatasourceGroupTags() {
        return tagProvider.list(TagType.DATASOURCE);
    }

    private DataSourceComponentGroup createGroup(String userId, String label) throws PersistenceException {
        DataSourceComponentGroup group = new DataSourceComponentGroup();
        group.setId(UUID.randomUUID().toString());
        group.setUserId(userId);
        group.setLabel(label);
        group.setVersion("1.0");
        group.setDescription(group.getLabel());
        group.setAuthors(userId);
        group.setCopyright("(C) " + userId);
        group.setNodeAffinity("Any");
        group = groupProvider.save(group);
        logger.finest(String.format("Created data source group [userName=%s, label=%s, id=%s",
                                    userId, label, group.getId()));
        return group;
    }

    private Query addQueryToGroup(DataSourceComponentGroup group, Query query, List<EOProduct> products) throws PersistenceException {
        DataSourceComponent component =
                dataSourceComponentService.createForLocations(products.stream().map(EOProduct::getName).collect(Collectors.toList()),
                                                              query.getSensor(), query.getDataSource(), query.getId(), query.getLabel(),
                                                              SessionStore.currentContext().getPrincipal());
        products = updateProducts(component.getId(), products, null);
        ProductPersister persister = new ProductPersister();
        persister.handle(products);
        query.setComponentId(component.getId());
        query = queryProvider.save(query);
        group.addDataSourceComponent(component);
        group.addQuery(query, component.getSources().get(0).getId());
        logger.finest(String.format("Query [id=%s, label=%s] added to data source group [id=%s]",
                                    query.getId(), query.getLabel(), group.getId()));
        return query;
    }

    private List<EOProduct> updateProducts(String componentId, List<EOProduct> products, Set<String> previousNames) {
        String[] names = products.stream().map(EOProduct::getName).toArray(String[]::new);
        Set<String> existingProducts = productProvider.getProductsByNames(names).stream()
                                                      .map(EOProduct::getName).collect(Collectors.toSet());
        List<EOProduct> candidatesToSave = new ArrayList<>(products);
        // remove from the list the products that are already in database
        candidatesToSave.removeIf(p -> existingProducts.contains(p.getName()));
        if (previousNames != null) {
            final Set<String> pNames = products.stream().map(EOProduct::getName).collect(Collectors.toSet());
            Collection<String> candidatesToRemove = previousNames.stream().filter(n -> !pNames.contains(n)).collect(Collectors.toList());
            for (String product : candidatesToRemove) {
                logger.finest(String.format("Product [%s] will be removed from database if not already downloaded or referenced by another component",
                                            product));
                productProvider.deleteIfNotReferenced(componentId, product);
            }
        }
        if (!candidatesToSave.isEmpty()) {
            ProductPersister persister = new ProductPersister();
            persister.handle(candidatesToSave);
        }
        return candidatesToSave;
    }

    private Query updateQuery(Query incomingQuery, Query dbQuery, String userId) throws PersistenceException {
        if (dbQuery != null) {
            dbQuery.setLabel(incomingQuery.getLabel());
            dbQuery.setLimit(incomingQuery.getLimit());
            dbQuery.setModified(LocalDateTime.now());
            dbQuery.setPageNumber(incomingQuery.getPageNumber());
            dbQuery.setPageSize(incomingQuery.getPageSize());
            dbQuery.setPassword(incomingQuery.getPassword());
            dbQuery.setSensor(incomingQuery.getSensor());
            dbQuery.setUser(incomingQuery.getUser());
            dbQuery.setUserId(userId);
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
        dbQuery = queryProvider.save(dbQuery);
        logger.finest(String.format("Query [id=%s, label=%s] was found in database and updated",
                                    dbQuery.getId(), dbQuery.getLabel()));
        return dbQuery;
    }

    private static <T> Predicate<T> not(Predicate<T> predicate) {
        return predicate.negate();
    }
}
