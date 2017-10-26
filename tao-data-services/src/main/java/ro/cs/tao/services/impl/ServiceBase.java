package ro.cs.tao.services.impl;

import ro.cs.tao.component.validation.ValidationException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Cosmin Cara
 */
public abstract class ServiceBase<T> {

    public void validate(T entity) throws ValidationException {
        List<String> errors = new ArrayList<>();
        if (entity == null) {
            throw new ValidationException("Entity cannot be null");
        } else {
            try {
                validateFields(entity, errors);
            } catch (Throwable t) {
                errors.add(t.getMessage());
            }
        }
        if (errors.size() > 0) {
            ValidationException ex = new ValidationException("Entity has validation errors");
            errors.forEach(e -> ex.addAdditionalInfo(e, null));
            throw ex;
        }
    }

    protected abstract void validateFields(T entity, List<String> errors);
}
