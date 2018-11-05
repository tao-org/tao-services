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
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.Sort;
import ro.cs.tao.SortDirection;
import ro.cs.tao.component.validation.ValidationException;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.CRUDService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Base class for all CRUD services.
 *
 * @author Cosmin Cara
 */
public abstract class DataEntityController<T, K, S extends CRUDService<T, K>> extends BaseController {

    @Autowired
    protected S service;

    @RequestMapping(value = "/{id:.+}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> get(@PathVariable("id") K id) {
        ResponseEntity<ServiceResponse<?>> response;
        T entity = null;
        try {
            entity = service.findById(id);
            if (entity == null) {
                response = prepareResult(String.format("Entity [%s] not found", id), ResponseStatus.FAILED);
            } else {
                response = prepareResult(entity);
            }
        } catch (PersistenceException e) {
            response = handleException(e);
        }
        return response;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @RequestMapping(value = "/", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> list(@RequestParam(name = "pageNumber", required = false) Optional<Integer> pageNumber,
                                                   @RequestParam(name = "pageSize", required = false) Optional<Integer> pageSize,
                                                   @RequestParam(name = "sortBy", required = false) Optional<String> sortByField,
                                                   @RequestParam(name = "sortDirection", required = false) Optional<SortDirection> sortDirection) {
        if (pageNumber.isPresent() && sortByField.isPresent()) {
            Sort sort = new Sort().withField(sortByField.get(), sortDirection.orElse(SortDirection.ASC));
            return prepareResult(service.list(pageNumber, pageSize, sort));
        } else {
            return prepareResult(service.list());
        }
    }

    @RequestMapping(value = "/", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> save(@RequestBody T entity) {
        final ResponseEntity<ServiceResponse<?>> validationResponse = validate(entity);
        if (validationResponse.getBody().getStatus() == ResponseStatus.SUCCEEDED) {
            entity = service.save(entity);
            return prepareResult(entity);
        } else {
            return validationResponse;
        }
    }

    @RequestMapping(value = "/{id:.+}", method = RequestMethod.PUT, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> update(@PathVariable("id") K id, @RequestBody T entity) {
        final ResponseEntity<ServiceResponse<?>> validationResponse = validate(entity);
        if (validationResponse.getBody().getStatus() == ResponseStatus.SUCCEEDED) {
            try {
                return prepareResult(service.update(entity));
            } catch (PersistenceException e) {
                return handleException(e);
            }
        } else {
            return validationResponse;
        }

    }

    @RequestMapping(value = "/{id:.+}/tag", method = RequestMethod.PUT, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> tag(@PathVariable("id") K id, @RequestBody List<String> tags) {
        try {
            return prepareResult(service.tag(id, tags));
        } catch (PersistenceException e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/{id:.+}/untag", method = RequestMethod.PUT, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> untag(@PathVariable("id") K id, @RequestBody List<String> tags) {
        try {
            return prepareResult(service.untag(id, tags));
        } catch (PersistenceException e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/{id:.+}", method = RequestMethod.DELETE, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> delete(@PathVariable("id") K id) {
        ResponseEntity<ServiceResponse<?>> response;
        try {
            service.delete(id);
            response = prepareResult("", ResponseStatus.SUCCEEDED);
        } catch (PersistenceException e) {
            response = handleException(e);
        }
        return response;
    }

    private ResponseEntity<ServiceResponse<?>> validate(T entity) {
        try {
            service.validate(entity);
            return prepareResult(entity);
        } catch (ValidationException ex) {
            List<String> errors = new ArrayList<>();
            String message = ex.getMessage();
            if (message != null && !message.isEmpty()) {
                errors.add(message);
            }
            final Map<String, Object> info = ex.getAdditionalInfo();
            if (info != null) {
                if (info.values().stream().allMatch(Objects::isNull)) {
                    errors.addAll(info.keySet());
                } else {
                    errors.addAll(info.entrySet().stream()
                                          .map(e -> e.getKey() + (e.getValue() != null ? ":" + e.getValue() : ""))
                                          .collect(Collectors.toSet()));
                }
            }
            return prepareResult(String.join(";", errors), ResponseStatus.FAILED);
        }
    }
}
