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
import ro.cs.tao.component.Variable;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.datasource.DataSourceComponentGroup;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.eodata.enums.ProductStatus;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.quota.QuotaException;
import ro.cs.tao.quota.UserQuotaManager;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.entity.beans.DataSourceGroupRequest;
import ro.cs.tao.services.entity.beans.DataSourceNameRequest;
import ro.cs.tao.services.entity.beans.DataSourceRequest;
import ro.cs.tao.services.entity.beans.GroupQuery;
import ro.cs.tao.services.entity.util.ServiceTransformUtils;
import ro.cs.tao.services.interfaces.DataSourceComponentService;
import ro.cs.tao.services.interfaces.DataSourceGroupService;
import ro.cs.tao.services.interfaces.QueryService;
import ro.cs.tao.utils.Tuple;

import java.security.Principal;
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

    @RequestMapping(value = "/user/", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getUserDataSourceComponents() {
        return prepareResult(ServiceTransformUtils.toDataSourceInfos(service.getUserDataSourceComponents(SessionStore.currentContext().getPrincipal().getName())));
    }

    @RequestMapping(value = "/user", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getUserDataSourceComponent(@RequestParam("id") String id) {
        try {
            return prepareResult(service.findById(id));
        } catch (PersistenceException e) {
            return handleException(e);
        }
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
        try {
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
                    if (queries[i].getProducts() == null && queries[i].getProducts().size() == 0) {
                        errors.add(String.format("Empty product names list on position %d", i + 1));
                    }
                }
            }
            if (errors.size() > 0) {
                result = prepareResult(String.join(";", errors), ResponseStatus.FAILED);
            } else {
                DataSourceComponentGroup group =
                        dataSourceGroupService.saveDataSourceGroup(request.getGroupId(), request.getGroupLabel(),
                                                                   Arrays.stream(request.getQueries())
                                                                           .map(q -> new Tuple<>(q.getQuery(), q.getProducts()))
                                                                           .collect(Collectors.toList()),
                                                                   SessionStore.currentContext().getPrincipal());
                result = prepareResult(group);

            }
        } catch (Exception ex) {
            result = handleException(ex);
        }
        return result;
    }

    @RequestMapping(value = "/user/group/remove", method = RequestMethod.DELETE, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> removeDataSourceComponentFromGroup(@RequestParam("groupId") String groupId) {
        ResponseEntity<ServiceResponse<?>> result;
        try {
            if (groupId == null || groupId.isEmpty()) {
                throw new IllegalArgumentException("Invalid group id");
            }
            DataSourceComponentGroup group = dataSourceGroupService.findById(groupId);
            if (group == null) {
                throw new PersistenceException(String.format("Group with id '%s' does not exist", groupId));
            }
            List<DataSourceComponent> components = group.getDataSourceComponents();
            for (DataSourceComponent component : components) {
                String nameList = component.getSources().get(0).getDataDescriptor().getLocation();
                if (nameList != null) {
                    String[] names = nameList.split(",");
                    for (String name : names) {
                        if (getPersistenceManager().getOtherProductReferences(component.getId(), name) == 0) {
                            getPersistenceManager().removeProduct(name);
                        }
                    }
                }
            }
            dataSourceGroupService.delete(groupId);
            result = prepareResult("Group " + groupId + " deleted", ResponseStatus.SUCCEEDED);
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
                return prepareResult(service.createForProducts(products, dataSource, request.getQueryId(), description, currentUser));
            } else {
                return prepareResult("Product list not provided", ResponseStatus.FAILED);
            }
        } catch (PersistenceException e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/create/names", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> createComponentFor(@RequestBody DataSourceNameRequest request) {
        try {
            if (request == null || request.getProducts() == null || request.getProducts().size() == 0) {
                throw new IllegalArgumentException("[products] Body is empty");
            }
            final List<Variable> products = request.getProducts();
            Map<String, List<String>> groups = new HashMap<>();
            for (Variable entry : products) {
                if (!groups.containsKey(entry.getValue())) {
                    groups.put(entry.getValue(), new ArrayList<>());
                }
                groups.get(entry.getValue()).add(entry.getKey());
            }
            final Principal currentUser = SessionStore.currentContext().getPrincipal();
            final String dataSource = "Local Database";
            List<EOProduct> prods;
            List<String> names;
            List<DataSourceComponent> components = new ArrayList<>();
            for (Map.Entry<String, List<String>> entry : groups.entrySet()) {
                names = entry.getValue();
                prods = getPersistenceManager().getProductsByNames(names.toArray(new String[0]));
                long addedQuota = 0;
                if (prods != null && prods.size() > 0) {
                	// check if, by adding the selected products, the quota will not be reached
                    for (EOProduct p: prods)
                    {
                    	if (p.getRefs().contains(currentUser.getName()) || 
                    		(p.getProductStatus() != ProductStatus.DOWNLOADED && p.getProductStatus() != ProductStatus.DOWNLOADING)) {
                    		// product already attached to the user or not yet downloaded.
                    		continue;
                    	}
                    	
                    	addedQuota += p.getApproximateSize();
                    	if (!UserQuotaManager.getInstance().checkUserInputQuota(currentUser, addedQuota)) 
                    	{
                    		// by adding this product to the user the quota will be reached
                    		throw new QuotaException("Cannot create query because you have reached your input quota! "
                    				+ "Please remove some of your products to continue.");
                    	}
                    	
                    	// attach the product to the current user
                    	p.addReference(currentUser.getName());
                    	getPersistenceManager().saveEOProduct(p);
                    	
                    }
                	
                    components.add(service.createForProductNames(names, entry.getKey(), dataSource, null, request.getLabel(), currentUser));
                }
            }
            return components.size() > 0 ? prepareResult(components) : prepareResult("No component was created", ResponseStatus.FAILED);
        } catch (PersistenceException e) {
            return handleException(e);
        } catch (QuotaException e) {
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
