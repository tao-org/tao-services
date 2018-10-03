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

import org.reflections.Reflections;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ro.cs.tao.TaoEnum;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.ConfigurationService;
import ro.cs.tao.services.model.KeyValuePair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Cosmin Cara
 */
@Controller
@RequestMapping("/config")
public class ConfigurationController extends DataEntityController<KeyValuePair, String, ConfigurationService> {

    @RequestMapping(value = "/enums", method = RequestMethod.GET)
    public ResponseEntity<ServiceResponse<?>> getAvailableEnumValues() {
        Map<String, List<KeyValuePair>> enumValues = new HashMap<>();
        Reflections reflections = new Reflections("ro.cs.tao");
        Set<Class<? extends TaoEnum>> enums = reflections.getSubTypesOf(TaoEnum.class);
        for (Class<? extends TaoEnum> anEnum : enums) {
            List<TaoEnum> values = Arrays.stream(anEnum.getEnumConstants()).collect(Collectors.toList());
            enumValues.put(anEnum.getName(),
                           values.stream().map(v -> new KeyValuePair(((Enum) v).name(), v.friendlyName()))
                                          .collect(Collectors.toList()));
        }

        return prepareResult(enumValues);
    }
}
