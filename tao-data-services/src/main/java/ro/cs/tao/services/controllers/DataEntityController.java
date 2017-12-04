package ro.cs.tao.services.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ro.cs.tao.component.validation.ValidationException;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.ServiceError;
import ro.cs.tao.services.interfaces.CRUDService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Base class for all CRUD services.
 *
 * @author Cosmin Cara
 */
public abstract class DataEntityController<T> extends BaseController {

    @Autowired
    private CRUDService<T> service;

    @RequestMapping(value = "/{id:.+}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> get(@PathVariable("id") String id) {
        T entity = service.findById(id);
        if (entity == null) {
            return new ResponseEntity<>(new ServiceError(String.format("Entity [%s] not found", id)),
                                        HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(entity, HttpStatus.OK);
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ResponseEntity<List<T>> list() {
        List<T> objects = service.list();
        if (objects == null || objects.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(objects, HttpStatus.OK);
    }

    @RequestMapping(value = "/", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<?> save(@RequestBody T entity) {
        final ResponseEntity<?> validationResponse = validate(entity);
        if (validationResponse.getStatusCode() == HttpStatus.OK) {
            service.save(entity);
            return new ResponseEntity<>(entity, HttpStatus.OK);
        } else {
            return validationResponse;
        }
    }

    @RequestMapping(value = "/{id:.+}", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<?> update(@PathVariable("id") String id, @RequestBody T entity) {
        final ResponseEntity<?> validationResponse = validate(entity);
        if (validationResponse.getStatusCode() == HttpStatus.OK) {
            service.update(entity);
            return new ResponseEntity<>(entity, HttpStatus.OK);
        } else {
            return validationResponse;
        }

    }

    @RequestMapping(value = "/{id:.+}", method = RequestMethod.DELETE)
    public ResponseEntity<?> delete(@PathVariable("id") String id) {
        service.delete(id);
        return new ResponseEntity<T>(HttpStatus.OK);
    }

    private ResponseEntity<?> validate(T entity) {
        try {
            service.validate(entity);
            return new ResponseEntity<>(HttpStatus.OK);
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
            return new ResponseEntity<>(errors, HttpStatus.NOT_ACCEPTABLE);
        }
    }
}
