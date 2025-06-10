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
package ro.cs.tao.services.entity.impl;

import ro.cs.tao.component.validation.ValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * A generic abstract service class for managing and validating entities.
 * This class implements basic validation functionality and provides an abstract method
 * for validating specific fields of the entity. It is designed to be extended by
 * specific service implementations.
 *
 * @param <T> The type of the entity managed by this service.
 *
 * @author Cosmin Cara
 */
public abstract class EntityService<T> {

    protected Logger logger = Logger.getLogger(getClass().getName());

    /**
     * Validates the given entity by checking its fields and ensuring it is not null.
     * If the entity is invalid, a {@link ValidationException} is thrown with the associated errors.
     *
     * @param entity The entity to be validated
     * @throws ValidationException if the entity is null or contains validation errors
     */
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
        if (!errors.isEmpty()) {
            ValidationException ex = new ValidationException("Entity has validation errors");
            errors.forEach(e -> ex.addAdditionalInfo(e, null));
            throw ex;
        }
    }

    /**
     * Validates the fields of a given entity and collects error messages if field validation fails.
     *
     * @param entity The entity whose fields require validation.
     * @param errors A list to collect validation error messages. If a field validation fails,
     *               the corresponding error message should be added to this list.
     */
    protected abstract void validateFields(T entity, List<String> errors);
}
