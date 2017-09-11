package ro.cs.tao.services.crud;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ro.cs.tao.services.commons.ServiceError;

import java.util.List;

/**
 * Base class for all CRUD services.
 *
 * @author Cosmin Cara
 */
public abstract class BasicController<T> {

    @Autowired
    private CRUDService<T> service;

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> get(@PathVariable("id") String id) {
        T entity = service.findById(id);
        if (entity == null) {
            return new ResponseEntity<>(new ServiceError(String.format("Entity [%s] not found", id)),
                                        HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(entity, HttpStatus.OK);
    }

    @RequestMapping(value = "/list/", method = RequestMethod.GET)
    public ResponseEntity<List<T>> list() {
        List<T> objects = service.list();
        if (objects == null || objects.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(objects, HttpStatus.OK);
    }

    @RequestMapping(value = "/update/{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> save(@PathVariable("id") String id, @RequestBody T entity) {
        service.save(entity);
        return new ResponseEntity<>(entity, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> delete(@PathVariable("id") String id) {
        service.delete(id);
        return new ResponseEntity<T>(HttpStatus.NO_CONTENT);
    }
}
