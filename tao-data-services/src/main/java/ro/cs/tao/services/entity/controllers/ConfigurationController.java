/*
 * Copyright (C) 2017 CS ROMANIA
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

import org.reflections.Reflections;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.ConfigurationService;
import ro.cs.tao.services.model.KeyValuePair;

import java.util.*;

/**
 * @author Cosmin Cara
 */
@Controller
@RequestMapping("/config")
public class ConfigurationController extends DataEntityController<KeyValuePair, ConfigurationService> {

    @RequestMapping(value = "/enums", method = RequestMethod.GET)
    public ResponseEntity<ServiceResponse<Map<String, List<Enum>>>> getAvailableEnumValues() {
        Map<String, List<Enum>> enumValues = new HashMap<>();
        Reflections reflections = new Reflections("ro.cs.tao");
        Set<Class<? extends Enum>> enums = reflections.getSubTypesOf(Enum.class);
        for (Class<? extends Enum> anEnum : enums) {
            List<Enum> values = new ArrayList<>();
            Enum[] enumConstants = anEnum.getEnumConstants();
            Collections.addAll(values, enumConstants);
            enumValues.put(anEnum.getName(), values);
        }

        return prepareResult(enumValues);
    }
}
