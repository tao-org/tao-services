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
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.entity.beans.CustomDataSourceRequest;
import ro.cs.tao.services.entity.util.ServiceTransformUtils;
import ro.cs.tao.services.interfaces.DataSourceComponentService;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/datasource")
public class DataSourceComponentController extends DataEntityController<DataSourceComponent, String, DataSourceComponentService>{

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
        return prepareResult(service.getDataSourceComponents(Arrays.asList(ids)));
    }

    @RequestMapping(value = "/user", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getUserDataSourceComponents() {
        return prepareResult(ServiceTransformUtils.toDataSourceInfos(service.getUserDataSourceComponents(SessionStore.currentContext().getPrincipal().getName())));
    }

    @RequestMapping(value = "/create", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> createComponentFor(@RequestBody CustomDataSourceRequest request) {
        try {
            List<EOProduct> products = request.getProducts();
            String productType = request.getProductType();
            String description = request.getLabel();
            Principal currentUser = SessionStore.currentContext().getPrincipal();
            if (productType != null) {
                return prepareResult(service.createForProductNames(products.stream().map(EOProduct::getName).collect(Collectors.toList()),
                                                                   productType, description, currentUser));
            } else {
                if (products.stream().map(EOProduct::getProductType).distinct().count() > 1) {
                    return prepareResult("The selected products must be of the same type", ResponseStatus.FAILED);
                } else {
                    return prepareResult(service.createForProducts(products, description, currentUser));
                }
            }
        } catch (PersistenceException e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/tags", method = RequestMethod.GET)
    public ResponseEntity<ServiceResponse<?>> listTags() {
        List<Tag> objects = service.getDatasourceTags();
        if (objects == null ) {
            objects = new ArrayList<>();
        }
        return prepareResult(objects.stream().map(Tag::getText).collect(Collectors.toList()));
    }
}
