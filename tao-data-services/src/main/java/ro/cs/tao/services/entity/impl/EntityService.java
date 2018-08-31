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

/**
 * @author Cosmin Cara
 */
public abstract class EntityService<T> {

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
