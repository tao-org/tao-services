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
import ro.cs.tao.component.enums.TagType;
import ro.cs.tao.component.validation.ValidationException;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.CRUDService;
import ro.cs.tao.utils.StringUtilities;

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

    /**
     * Returns the details of the entity
     * @param id    The entity identifier
     */
    @RequestMapping(value = { "/{id:.+}", "/{id:.+}/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> get(@PathVariable("id") K id) {
        ResponseEntity<ServiceResponse<?>> response;
        T entity;
        try {
            entity = service.findById(id);
            if (entity == null) {
                response = prepareResult(String.format("Entity [%s] not found", id), ResponseStatus.FAILED);
            } else {
                response = prepareResult(entity);
            }
        } catch (Exception e) {
            response = handleException(e);
        }
        return response;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @RequestMapping(value = { "/", "" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
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

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @RequestMapping(value = { "/filter", "/filter/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> filter(@RequestParam(name = "pageNumber", required = false) Optional<Integer> pageNumber,
                                                     @RequestParam(name = "pageSize", required = false) Optional<Integer> pageSize,
                                                     @RequestParam(name = "sortBy", required = false) Optional<String> sortByField,
                                                     @RequestParam(name = "sortDirection", required = false) Optional<SortDirection> sortDirection,
                                                     @RequestParam(name = "label", required = false) Optional<String> label,
                                                     @RequestParam(name = "tags", required = false) Optional<List<String>> tags) {
        if (pageNumber.isEmpty()) {
            return prepareResult(filterResults(service.list()));
        }
        try {
            Sort sort = new Sort().withField(sortByField.orElse("id"), sortDirection.orElse(SortDirection.ASC));
            return prepareResult(filterResults(service.list(pageNumber, pageSize.or(() -> Optional.of(10)), sort,
                                                             label.orElse(null), componentTags(tags))));
        } catch (Exception e) {
            return prepareResult(List.of());
        }
    }

    @RequestMapping(value = { "/count", "/count/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> count(@RequestParam(name = "label", required = false) Optional<String> label,
                                                    @RequestParam(name = "tags", required = false) Optional<List<String>> tags) {
        return prepareResult(service.count(label.orElse(null), componentTags(tags)));
    }

    @RequestMapping(value = { "/list", "/list/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> listByIds(@RequestParam(name = "id") String idList) {
        if (StringUtilities.isNullOrEmpty(idList)) {
            return prepareResult("Invalid id list", ResponseStatus.FAILED);
        }
        List<K> ids = Arrays.stream(idList.split(","))
                            .map(this::parseIdentifier)
                            .collect(Collectors.toList());
        return prepareResult(listResults(service.list(ids)));
    }

    @RequestMapping(value = { "/find", "/find/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> find(@RequestParam(name = "text") String text,
                                                   @RequestParam(name = "labels", required = false, defaultValue = "true") boolean labelsOnly) {
        if (StringUtilities.isNullOrEmpty(text)) {
            return prepareResult("Invalid search text", ResponseStatus.FAILED);
        }
        return prepareResult(labelsOnly
                             ? service.findLabelsBySimilarity(text, currentOwner())
                             : service.findBySimilarity(text, currentOwner()));
    }

    private List<Tag> componentTags(Optional<List<String>> tags) {
        return tags.orElse(List.of()).stream()
                   .filter(t -> !StringUtilities.isNullOrEmpty(t))
                   .map(t -> new Tag(TagType.COMPONENT, t))
                   .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    protected K parseIdentifier(String value) {
        return (K) value;
    }

    protected Object filterResults(List<T> entities) {
        return entities;
    }

    protected Object listResults(List<T> entities) {
        return entities;
    }
    /**
     * Creates a new entity.
     * If values of the entity fields are not correct, the response will contain which values were faulty.
     */
    @RequestMapping(value = { "/", "" }, method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> save(@RequestBody T entity) {
        final ResponseEntity<ServiceResponse<?>> validationResponse = validate(entity);
        ServiceResponse<?> body = validationResponse.getBody();
        if (body != null && body.getStatus() == ResponseStatus.SUCCEEDED) {
            entity = service.save(entity);
            return prepareResult(entity);
        } else {
            return validationResponse;
        }
    }
    /**
     * Updates an existing entity.
     * If values of the entity fields are not correct, the response will contain which values were faulty.
     */
    @RequestMapping(value = { "/{id:.+}", "/{id:.+}/" }, method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> update(@PathVariable("id") K id, @RequestBody T entity) {
        final ResponseEntity<ServiceResponse<?>> validationResponse = validate(entity);
        ServiceResponse<?> body = validationResponse.getBody();
        if (body != null && body.getStatus() == ResponseStatus.SUCCEEDED) {
            try {
                return prepareResult(service.update(entity));
            } catch (PersistenceException e) {
                return handleException(e);
            }
        } else {
            return validationResponse;
        }

    }
    /**
     * Associates one or more tags to an entity.
     * @param id    The entity identifier
     * @param tags  The list of tags to be associated
     */
    @RequestMapping(value = { "/{id:.+}/tag", "/{id:.+}/tag/" }, method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> tag(@PathVariable("id") K id, @RequestBody List<String> tags) {
        try {
            return prepareResult(service.tag(id, tags));
        } catch (PersistenceException e) {
            return handleException(e);
        }
    }
    /**
     * Dissociates one or more tags from an entity.
     * @param id    The entity identifier
     * @param tags  The list of tags to be removed
     */
    @RequestMapping(value = { "/{id:.+}/untag", "/{id:.+}/untag/" }, method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> untag(@PathVariable("id") K id, @RequestBody List<String> tags) {
        try {
            return prepareResult(service.unTag(id, tags));
        } catch (PersistenceException e) {
            return handleException(e);
        }
    }

    /**
     * Deletes an entity
     * @param id    The entity identifier
     */
    @RequestMapping(value = { "/{id:.+}", "/{id:.+}/" }, method = RequestMethod.DELETE)
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
