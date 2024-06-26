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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.Sort;
import ro.cs.tao.SortDirection;
import ro.cs.tao.Tag;
import ro.cs.tao.component.SourceDescriptor;
import ro.cs.tao.component.Variable;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.datasource.DataSourceComponentGroup;
import ro.cs.tao.datasource.DataSourceConfiguration;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.eodata.enums.ProductStatus;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.RepositoryProvider;
import ro.cs.tao.quota.QuotaException;
import ro.cs.tao.quota.UserQuotaManager;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.entity.beans.*;
import ro.cs.tao.services.entity.util.ServiceTransformUtils;
import ro.cs.tao.services.factory.StorageServiceFactory;
import ro.cs.tao.services.interfaces.*;
import ro.cs.tao.services.model.component.DataSourceInfo;
import ro.cs.tao.services.model.component.ProductSetInfo;
import ro.cs.tao.utils.AutoEvictableCache;
import ro.cs.tao.utils.Tuple;
import ro.cs.tao.workspaces.Repository;
import ro.cs.tao.workspaces.RepositoryType;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/datasource")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Data Sources", description = "Operations related to data sources")
public class DataSourceComponentController extends DataEntityController<DataSourceComponent, String, DataSourceComponentService>{

    private final AutoEvictableCache<String, DataSourceInfo> componentCache
            = new AutoEvictableCache<>(s -> new DataSourceInfo(service.findById(s)), 1800);

    @Autowired
    private ProductService productService;
    @Autowired
    private DataSourceGroupService dataSourceGroupService;
    @Autowired
    private QueryService queryService;
    @Autowired
    private RepositoryProvider repositoryProvider;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @RequestMapping(value = "/page", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Override
    public ResponseEntity<ServiceResponse<?>> list(@RequestParam(name = "pageNumber", required = false) Optional<Integer> pageNumber,
                                                   @RequestParam(name = "pageSize", required = false) Optional<Integer> pageSize,
                                                   @RequestParam(name = "sortBy", required = false) Optional<String> sortByField,
                                                   @RequestParam(name = "sortDirection", required = false) Optional<SortDirection> sortDirection) {
        if (pageNumber.isPresent() && sortByField.isPresent()) {
            Sort sort = new Sort().withField(sortByField.get(), sortDirection.orElse(SortDirection.ASC));
            return prepareResult(ServiceTransformUtils.toDataSourceInfos(service.list(pageNumber, pageSize, sort)));
        } else {
            if (this.componentCache.size() == 0) {
                final List<DataSourceComponent> list = service.list();
                list.forEach(c -> this.componentCache.put(c.getId(), new DataSourceInfo(c)));
            } else {
                final List<DataSourceComponent> list = service.getOhterDataSourceComponents(this.componentCache.keySet());
                list.forEach(c -> this.componentCache.put(c.getId(), new DataSourceInfo(c)));
            }
            return prepareResult(this.componentCache.values());
        }
    }

    @RequestMapping(value = "/", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getSystemDatasources() {
        return prepareResult(ServiceTransformUtils.toDataSourceInfos(service.getSystemDataSourceComponents()));
    }

    @RequestMapping(value = "/list", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> list(@RequestParam(name = "id") String idList) {
        if (idList == null || idList.isEmpty()) {
            return prepareResult("Invalid id list", ResponseStatus.FAILED);
        }
        String[] ids = idList.split(",");
        return prepareResult(service.list(Arrays.asList(ids)));
    }

    @RequestMapping(value = "/user/", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getUserDataSourceComponents() {
        return prepareResult(ServiceTransformUtils.toDataSourceInfos(service.getUserDataSourceComponents(SessionStore.currentContext().getPrincipal().getName())));
    }

    @RequestMapping(value = "/user", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getUserDataSourceComponent(@RequestParam("id") String id) {
        try {
            return prepareResult(service.findById(id));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/user/group", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getUserDataSourceComponentGroup(@RequestParam("id") String groupId) {
        try {
            return prepareResult(dataSourceGroupService.findById(groupId));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/user/group/", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getUserDataSourceComponentGroups() {
        return prepareResult(ServiceTransformUtils.toDataSourceGroupInfos(
                dataSourceGroupService.getUserDataSourceComponentGroups(SessionStore.currentContext().getPrincipal().getName())));
    }

    @RequestMapping(value = "/user/group/save", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
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
                    if (queries[i].getProducts() == null && queries[i].getProducts().isEmpty()) {
                        errors.add(String.format("Empty product names list on position %d", i + 1));
                    }
                }
            }
            if (!errors.isEmpty()) {
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

    @RequestMapping(value = "/user/group/remove", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
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
                        if (this.productService.countAdditionalProductReferences(component.getId(), name) == 0) {
                            this.productService.delete(name);
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

    @RequestMapping(value = "/create", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> createComponentFor(@RequestBody DataSourceRequest request) {
        try {
            List<EOProduct> products = request.getProducts();
            String productType = request.getProductType();
            String dataSource = request.getDataSource();
            String description = request.getLabel();
            Principal currentUser = SessionStore.currentContext().getPrincipal();
            if (products != null && !products.isEmpty()) {
                return prepareResult(service.createForProducts(products, dataSource, request.getQueryId(), description, currentUser));
            } else {
                return prepareResult("Product list not provided", ResponseStatus.FAILED);
            }
        } catch (PersistenceException e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/create/names", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> createComponentFor(@RequestBody DataSourceNameRequest request) {
        try {
            if (request == null || request.getProducts() == null || request.getProducts().isEmpty()) {
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
                prods = this.productService.getByNames(names.toArray(new String[0]));
                long addedQuota = 0;
                if (prods != null && !prods.isEmpty()) {
                	// check if, by adding the selected products, the quota will not be reached
                    for (EOProduct p: prods) {
                    	if (p.getRefs().contains(currentUser.getName()) || 
                    		(p.getProductStatus() != ProductStatus.DOWNLOADED && p.getProductStatus() != ProductStatus.DOWNLOADING)) {
                    		// product already attached to the user or not yet downloaded.
                    		continue;
                    	}
                    	
                    	addedQuota += p.getApproximateSize();
                    	if (!UserQuotaManager.getInstance().checkUserInputQuota(currentUser, addedQuota)) 
                    	{
                    		// by adding this product to the user the quota will be reached
                    		throw new QuotaException("Cannot create query because you have reached your input quota! " +
                                                             "Please remove some of your products to continue.");
                    	}
                    	
                    	// attach the product to the current user
                    	p.addReference(currentUser.getName());
                    	//getPersistenceManager().saveEOProduct(p);
                    	
                    }
                	
                    components.add(service.createForLocations(names, entry.getKey(), dataSource, null, request.getLabel(), currentUser));
                }
            }
            return components.isEmpty()
                   ? prepareResult("No component was created", ResponseStatus.FAILED)
                   : prepareResult(components);
        } catch (PersistenceException | QuotaException e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/productset", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> createProductSet(@RequestBody ProductSetRequest request) {
        try {
            List<String> productPaths;
            if (request == null || (productPaths = request.getProductPaths()) == null || productPaths.isEmpty()) {
                throw new IllegalArgumentException("[productPaths] Body is empty");
            }
            final StorageService repositoryService = getLocalRepositoryService();
            final Repository localWorkspace = getLocalRepository(currentUser());
            for (String path : productPaths) {
                if (!repositoryService.exists(localWorkspace.resolve(path))) {
                    throw new FileNotFoundException(path + " does not exist");
                }
            }
            final DataSourceComponent component = service.createForLocations(productPaths, request.getLabel(), currentPrincipal());
            return component != null ? prepareResult(component) : prepareResult("No component was created", ResponseStatus.FAILED);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/tags", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> listTags() {
        List<Tag> objects = service.getDatasourceTags();
        if (objects == null ) {
            objects = new ArrayList<>();
        }
        return prepareResult(objects.stream().map(Tag::getText).collect(Collectors.toList()));
    }

    @RequestMapping(value = "/config", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getConfiguration(@RequestParam("dataSourceId") String dataSourceId) {
        try {
            return prepareResult(service.getConfiguration(dataSourceId));
        } catch (PersistenceException e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/config", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> saveConfiguration(@RequestBody DataSourceConfiguration configuration) {
        try {
            service.saveConfiguration(configuration);
            return prepareResult("Succeeded", ResponseStatus.SUCCEEDED);
        } catch (PersistenceException e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/config", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> update(@RequestBody DataSourceConfiguration configuration) {
        try {
            service.updateConfiguration(configuration);
            return prepareResult("Succeeded", ResponseStatus.SUCCEEDED);
        } catch (PersistenceException e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/productset", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> listProductSets() {
        try {
            final String user = currentUser();
            return prepareResult(ServiceTransformUtils.toProductSetInfos(this.service.getProductSets(user), user));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/productset/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getProductSet(@PathVariable("id") String id) {
        try {
            return prepareResult(new ProductSetInfo(this.service.findById(id), currentUser()));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/productset", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> updateProductSet(@RequestBody ProductSetInfo productSet) {
        try {
            List<String> products = productSet.getProducts();
            if (products != null) {
                Repository localRepository = getLocalRepository(currentUser());
                products.replaceAll(localRepository::resolve);
            }
            return prepareResult(new ProductSetInfo(this.service.updateProductSet(productSet), currentUser()));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/productset/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> deleteProductSet(@PathVariable("id") String id) {
        try {
            this.service.delete(id);
            return prepareResult(id, ResponseStatus.SUCCEEDED);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/productset/{id}/check", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> verifyProductSet(@PathVariable("id") String id) {
        try {
            final DataSourceComponent component = this.service.findById(id);
            final SourceDescriptor descriptor = component.getSources().stream().filter(s -> DataSourceComponent.QUERY_PARAMETER.equals(s.getName())).findFirst().orElse(null);
            Map<String, Boolean> products = new LinkedHashMap<>();
            if (descriptor != null) {
                String location = descriptor.getDataDescriptor().getLocation();
                if (location != null) {
                    products.putAll(Arrays.stream(location.split(",")).collect(Collectors.toMap(Function.identity(), p -> {
                        try {
                            return Files.exists(Paths.get(p));
                        } catch (Exception e) {
                            return false;
                        }
                    })));

                }
            }
            return prepareResult(id, ResponseStatus.SUCCEEDED);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    private Repository getLocalRepository(String user) {
        return repositoryProvider.getUserSystemRepositories(user).stream().filter(w -> w.getType() == RepositoryType.LOCAL).findFirst().orElse(null);
    }

    private StorageService getLocalRepositoryService() {
        Repository repository = getLocalRepository(currentUser());
        if (repository == null) {
            throw new RuntimeException("User [" + currentUser() + "] doesn't have a local repository");
        }
        StorageService instance = StorageServiceFactory.getInstance(repository);
        instance.associate(repository);
        return instance;
    }
}
