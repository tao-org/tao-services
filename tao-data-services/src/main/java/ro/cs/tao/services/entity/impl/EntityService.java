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

import ro.cs.tao.component.DataDescriptor;
import ro.cs.tao.component.SourceDescriptor;
import ro.cs.tao.component.TaoComponent;
import ro.cs.tao.component.TargetDescriptor;
import ro.cs.tao.component.validation.ValidationException;
import ro.cs.tao.eodata.enums.DataFormat;

import java.util.ArrayList;
import java.util.EnumMap;
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

    protected static final EnumMap<DataFormat, String> templates;

    static {
        templates = new EnumMap<>(DataFormat.class);
        templates.put(DataFormat.RASTER, "raster file");
        templates.put(DataFormat.VECTOR, "vector file");
        templates.put(DataFormat.OTHER, "file");
        templates.put(DataFormat.DB_CONNECTION, "connection string");
        templates.put(DataFormat.FOLDER, "folder");
        templates.put(DataFormat.JSON, "JSON file");
        templates.put(DataFormat.ANY, "file");
    }

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
     * Adds input and output descriptions to the given component by analyzing its sources and targets.
     * Generates a description for inputs and outputs based on the data format and cardinality.
     *
     * @param <C> The type of TaoComponent.
     * @param component The TaoComponent for which input and output descriptions need to be added.
     *                  If the component is null, the method will return without performing any operations.
     */
    protected <C extends TaoComponent> void addInputOutputDescription(C component) {
        if (component == null) {
            return;
        }
        List<SourceDescriptor> sources = component.getSources();
        if (sources != null) {
            int cardinality;
            for (SourceDescriptor descriptor : sources) {
                if (descriptor.getDescription() == null) {
                    cardinality = descriptor.getCardinality();
                    DataDescriptor dataDescriptor = descriptor.getDataDescriptor();
                    if (dataDescriptor == null) {
                        dataDescriptor = new DataDescriptor(DataFormat.ANY);
                    }
                    descriptor.setDescription("The" + (cardinality != 1 ? " list of " : " ") + "input " + templates.get(dataDescriptor.getFormatType()) + (cardinality != 1 ? "s": ""));
                }
            }
        }
        List<TargetDescriptor> targets = component.getTargets();
        if (targets != null) {
            int cardinality;
            for (TargetDescriptor descriptor : targets) {
                if (descriptor.getDescription() != null) {
                    cardinality = descriptor.getCardinality();
                    DataDescriptor dataDescriptor = descriptor.getDataDescriptor();
                    if (dataDescriptor == null) {
                        dataDescriptor = new DataDescriptor(DataFormat.ANY);
                    }
                    descriptor.setDescription("The" + (cardinality != 1 ? " list of " : " ") + "output " + templates.get(descriptor.getDataDescriptor().getFormatType()) + (cardinality != 1 ? "s" : ""));
                }
            }
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
