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

import org.springframework.stereotype.Service;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.services.interfaces.ConfigurationService;
import ro.cs.tao.services.model.KeyValuePair;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author Cosmin Cara
 */
@Service("configurationService")
public class ConfigurationServiceImpl
    extends EntityService<KeyValuePair>
        implements ConfigurationService {

    @Override
    public KeyValuePair findById(String id) {
        final String value = ConfigurationManager.getInstance().getValue(id);
        return value != null ? new KeyValuePair(id, value) : null;
    }

    @Override
    public List<KeyValuePair> list() {
        final Map<String, String> properties = ConfigurationManager.getInstance().getAll();
        List<KeyValuePair> result = null;
        if (properties != null) {
            result = properties.entrySet().stream()
                    .map(e -> new KeyValuePair(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
        }
        return result;
    }

    @Override
    public List<KeyValuePair> list(Iterable<String> ids) {
        final Map<String, String> properties = ConfigurationManager.getInstance().getAll();
        List<KeyValuePair> result = null;
        if (properties != null) {
            Set<String> identifiers = StreamSupport.stream(ids.spliterator(), false).collect(Collectors.toSet());
            result = properties.entrySet().stream()
                    .filter(p -> identifiers.contains(p.getKey()))
                    .map(e -> new KeyValuePair(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
        }
        return result;
    }

    @Override
    public KeyValuePair save(KeyValuePair object) {
        //TODO: delegate to user preferences
        return null;
    }

    @Override
    public KeyValuePair update(KeyValuePair object) {
        //TODO: delegate to user preferences
        return null;
    }

    @Override
    public void delete(String id) {
        //TODO: delegate to user preferences
    }

    @Override
    protected void validateFields(KeyValuePair object, List<String> errors) {
        String value = object.getKey();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[key] cannot be empty");
        }
        value = object.getValue();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[value] cannot be empty");
        }
    }
}
