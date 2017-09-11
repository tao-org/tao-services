package ro.cs.tao.services.crud;

import java.util.List;

/**
 * Interface defining a CRUD entity service
 *
 * @author Cosmin Cara
 */
public interface CRUDService<T> {

    T findById(String id);

    List<T> list();

    void save(T object);

    void delete(String id);

}
