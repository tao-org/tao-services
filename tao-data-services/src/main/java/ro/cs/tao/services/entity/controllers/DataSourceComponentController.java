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

package ro.cs.tao.services.entity.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import ro.cs.tao.Sort;
import ro.cs.tao.SortDirection;
import ro.cs.tao.Tag;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.datasource.DataSourceComponentGroup;
import ro.cs.tao.datasource.beans.Query;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.entity.beans.DataSourceGroupRequest;
import ro.cs.tao.services.entity.beans.DataSourceRequest;
import ro.cs.tao.services.entity.beans.GroupQuery;
import ro.cs.tao.services.entity.util.ServiceTransformUtils;
import ro.cs.tao.services.interfaces.DataSourceComponentService;
import ro.cs.tao.services.interfaces.DataSourceGroupService;
import ro.cs.tao.services.interfaces.QueryService;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/datasource")
public class DataSourceComponentController extends DataEntityController<DataSourceComponent, String, DataSourceComponentService>{

    @Autowired
    private DataSourceGroupService dataSourceGroupService;
    @Autowired
    private QueryService queryService;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @RequestMapping(value = "/page", method = RequestMethod.GET, produces = "application/json")
    @Override
    public ResponseEntity<ServiceResponse<?>> list(@RequestParam(name = "pageNumber", required = false) Optional<Integer> pageNumber,
                                                   @RequestParam(name = "pageSize", required = false) Optional<Integer> pageSize,
                                                   @RequestParam(name = "sortBy", required = false) Optional<String> sortByField,
                                                   @RequestParam(name = "sortDirection", required = false) Optional<SortDirection> sortDirection) {
        if (pageNumber.isPresent() && sortByField.isPresent()) {
            Sort sort = new Sort().withField(sortByField.get(), sortDirection.orElse(SortDirection.ASC));
            return prepareResult(ServiceTransformUtils.toDataSourceInfos(service.list(pageNumber, pageSize, sort)));
        } else {
            return prepareResult(ServiceTransformUtils.toDataSourceInfos(service.list()));
        }
    }

    @RequestMapping(value = "/", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getSystemDatasources() {
        return prepareResult(ServiceTransformUtils.toDataSourceInfos(service.getSystemDataSourceComponents()));
    }

    @RequestMapping(value = "/list", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> list(@RequestParam(name = "id") String idList) {
        if (idList == null || idList.isEmpty()) {
            return prepareResult("Invalid id list", ResponseStatus.FAILED);
        }
        String[] ids = idList.split(",");
        return prepareResult(service.list(Arrays.asList(ids)));
    }

    @RequestMapping(value = "/user", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getUserDataSourceComponents() {
        return prepareResult(ServiceTransformUtils.toDataSourceInfos(service.getUserDataSourceComponents(SessionStore.currentContext().getPrincipal().getName())));
    }

    @RequestMapping(value = "/user/group", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getUserDataSourceComponentGroup(@RequestParam("id") String groupId) {
        try {
            return prepareResult(dataSourceGroupService.findById(groupId));
        } catch (PersistenceException e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/user/group/", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getUserDataSourceComponentGroups() {
        return prepareResult(ServiceTransformUtils.toDataSourceGroupInfos(
                dataSourceGroupService.getUserDataSourceComponentGroups(SessionStore.currentContext().getPrincipal().getName())));
    }

    @RequestMapping(value = "/user/group/save", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> createOrUpdateDataSourceGroup(@RequestBody DataSourceGroupRequest request) throws PersistenceException {
        ResponseEntity<ServiceResponse<?>> result;
        List<String> errors = new ArrayList<>();
        if (request == null) {
            errors.add("Empty body");
        } else if (request.getGroupLabel() == null) {
            errors.add("Empty label");
        } else if (request.getQueries() == null || request.getQueries().length == 0) {
            errors.add("Query list is null or empty");
        } else {
            GroupQuery[] queries = request.getQueries();
            for (int i = 0; i < queries.length; i++) {
                if (queries[i].getQuery() == null) {
                    errors.add(String.format("Empty query on position %d", i + 1));
                }
                if (queries[i].getProductNames() == null && queries[i].getProductNames().size() == 0) {
                    errors.add(String.format("Empty product names list on position %d", i + 1));
                }
            }
        }
        if (errors.size() > 0) {
            result = prepareResult(String.join(";", errors), ResponseStatus.FAILED);
        } else {
            DataSourceComponentGroup group;
            String groupId = request.getGroupId();
            if (groupId == null) {
                group = new DataSourceComponentGroup();
                group.setId(UUID.randomUUID().toString());
                group.setUserName(currentUser());
                group.setLabel(request.getGroupLabel());
                group.setVersion("1.0");
                group.setDescription(group.getLabel());
                group.setAuthors(currentUser());
                group.setCopyright("(C) " + currentUser());
                group.setNodeAffinity("Any");
                group = getPersistenceManager().saveDataSourceComponentGroup(group);
                for (GroupQuery query : request.getQueries()) {
                    Query q = query.getQuery();
                    q = getPersistenceManager().saveQuery(q);
                    DataSourceComponent component = service.createForProductNames(query.getProductNames(), q.getSensor(),
                                                                                  q.getDataSource(), q.getLabel(),
                                                                                  SessionStore.currentContext().getPrincipal());
                    group.addDataSourceComponent(component);
                    group.addQuery(q, component.getSources().get(0).getId());
                }
                result = prepareResult(getPersistenceManager().updateDataSourceComponentGroup(group));
            } else {
                group = dataSourceGroupService.findById(groupId);
                Set<Query> querySet = group.getDataSourceQueries();
                GroupQuery[] incomingQueries = request.getQueries();
                for (GroupQuery query : incomingQueries) {
                    Query incomingQuery = query.getQuery();
                    Query dbQuery = querySet.stream().filter(q -> Objects.equals(q.getId(), incomingQuery.getId())).findFirst().orElse(null);
                    dbQuery = updateQuery(incomingQuery, dbQuery);
                    if (query.getComponentId() == null) {
                        DataSourceComponent component = service.createForProductNames(query.getProductNames(), incomingQuery.getSensor(),
                                                                                      incomingQuery.getDataSource(), incomingQuery.getLabel(),
                                                                                      SessionStore.currentContext().getPrincipal());
                        group.addDataSourceComponent(component);
                        group.addQuery(dbQuery, component.getSources().get(0).getId());
                    }
                }
                List<GroupQuery> notExisting = Arrays.stream(incomingQueries).filter(i -> !querySet.contains(i.getQuery()))
                        .collect(Collectors.toList());
                for (GroupQuery query : notExisting) {
                    Query q = query.getQuery();
                    q = getPersistenceManager().saveQuery(q);
                    DataSourceComponent component = service.createForProductNames(query.getProductNames(), q.getSensor(),
                                                                                  q.getDataSource(), q.getLabel(),
                                                                                  SessionStore.currentContext().getPrincipal());
                    group.addDataSourceComponent(component);
                    group.addQuery(q, component.getSources().get(0).getId());
                }
                result = prepareResult(getPersistenceManager().updateDataSourceComponentGroup(group));
            }
        }
        return result;
    }

    private Query updateQuery(Query incomingQuery, Query dbQuery) throws PersistenceException {
        if (dbQuery != null) {
            dbQuery.setLabel(incomingQuery.getLabel());
            dbQuery.setLimit(incomingQuery.getLimit());
            dbQuery.setModified(LocalDateTime.now());
            dbQuery.setPageNumber(incomingQuery.getPageNumber());
            dbQuery.setPageSize(incomingQuery.getPageSize());
            dbQuery.setPassword(incomingQuery.getPassword());
            dbQuery.setSensor(incomingQuery.getSensor());
            dbQuery.setUser(incomingQuery.getUser());
            dbQuery.setUserId(currentUser());
            Map<String, String> parameters = dbQuery.getValues();
            if (parameters == null) {
                parameters = new HashMap<>();
            }
            parameters.putAll(incomingQuery.getValues());
            dbQuery.setValues(parameters);
        } else {
            dbQuery = incomingQuery;
        }
        return getPersistenceManager().saveQuery(dbQuery);
    }

    @RequestMapping(value = "/user/group/remove", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> removeDataSourceComponentFromGroup(@RequestParam("groupId") String groupId,
                                                                                 @RequestParam("componentId") String componentId,
                                                                                 @RequestParam("queryId") long queryId) {
        ResponseEntity<ServiceResponse<?>> result;
        try {
            if (groupId == null || groupId.isEmpty()) {
                throw new IllegalArgumentException("Invalid group id");
            }
            if (componentId == null || componentId.isEmpty()) {
                throw new IllegalArgumentException("Invalid component id");
            }
            DataSourceComponentGroup group = dataSourceGroupService.findById(groupId);
            if (group == null) {
                throw new PersistenceException(String.format("Group with id '%s' does not exist", groupId));
            }
            DataSourceComponent component = service.findById(componentId);
            if (component == null) {
                throw new PersistenceException(String.format("Data source with id '%s' does not exist", component));
            }
            group.removeDataSourceComponent(component);
            group.removeQuery(queryId);
            Query query = queryService.findById(queryId);
            if (query != null) {
                queryService.delete(queryId);
            }
            result = prepareResult(dataSourceGroupService.update(group));
            service.delete(component.getId());
        } catch (PersistenceException e) {
            result = handleException(e);
        }
        return result;
    }

    @RequestMapping(value = "/create", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> createComponentFor(@RequestBody DataSourceRequest request) {
        try {
            List<EOProduct> products = request.getProducts();
            String productType = request.getProductType();
            String dataSource = request.getDataSource();
            String description = request.getLabel();
            Principal currentUser = SessionStore.currentContext().getPrincipal();
            if (products != null && products.size() > 0) {
                if (productType != null) {
                    return prepareResult(service.createForProductNames(products.stream().map(EOProduct::getName).collect(Collectors.toList()),
                                                                       productType, dataSource, description, currentUser));
                } else {
                    return prepareResult(service.createForProducts(products, dataSource, description, currentUser));
                }
            } else {
                return prepareResult("Product list not provided", ResponseStatus.FAILED);
            }
        } catch (PersistenceException e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/tags", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> listTags() {
        List<Tag> objects = service.getDatasourceTags();
        if (objects == null ) {
            objects = new ArrayList<>();
        }
        return prepareResult(objects.stream().map(Tag::getText).collect(Collectors.toList()));
    }
}
