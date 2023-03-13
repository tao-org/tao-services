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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import ro.cs.tao.TaoEnum;
import ro.cs.tao.eodata.Projection;
import ro.cs.tao.eodata.sorting.Association;
import ro.cs.tao.eodata.sorting.DataFilter;
import ro.cs.tao.eodata.sorting.DataSorter;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.ConfigurationService;
import ro.cs.tao.services.model.KeyValuePair;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Cosmin Cara
 */
@RestController
@RequestMapping("/config")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Configuration", description = "Operations related to configuration")
public class ConfigurationController extends DataEntityController<KeyValuePair, String, ConfigurationService> {

    private WeakReference<List<String>> sorters;
    private WeakReference<List<String[]>> groupers;
    private WeakReference<List<String[]>> filters;
    private WeakReference<Map<String, List<KeyValuePair>>> enums;

    /**
     * Lists all the enumerations available.
     * A such enumeration must implement {@link TaoEnum}
     *
     */
    @RequestMapping(value = "/enums", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getAvailableEnumValues() {
        if (this.enums == null || this.enums.get() == null) {
            Map<String, List<KeyValuePair>> enumValues = new HashMap<>();
            Reflections reflections = new Reflections("ro.cs.tao");
            Set<Class<? extends TaoEnum>> enums = reflections.getSubTypesOf(TaoEnum.class);
            for (Class<? extends TaoEnum> anEnum : enums) {
                List<TaoEnum> values = Arrays.stream(anEnum.getEnumConstants()).collect(Collectors.toList());
                enumValues.put(anEnum.getName(),
                               values.stream().map(v -> new KeyValuePair(((Enum) v).name(), v.friendlyName()))
                                       .collect(Collectors.toList()));
            }
            this.enums = new WeakReference<>(enumValues);
        }
        return prepareResult(this.enums.get());
    }

    /**
     * Lists all sorters that can be applied to a link.
     * A sorter embeds a rule for sorting inputs of a link
     *
     */
    @RequestMapping(value = "/sorters", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getAvailableSorters() {
        if (this.sorters == null || this.sorters.get() == null) {
            List<String> descriptions = new ArrayList<>();
            Reflections reflections = new Reflections("ro.cs.tao");
            Set<Class<? extends DataSorter>> sorters = reflections.getSubTypesOf(DataSorter.class);
            for (Class<? extends DataSorter> sortClass : sorters) {
                try {
                    descriptions.add(sortClass.newInstance().getFriendlyName());
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            descriptions.sort(Comparator.naturalOrder());
            this.sorters = new WeakReference<>(descriptions);
        }
        return prepareResult(this.sorters.get());
    }

    /**
     * Lists all groupers that can be applied to a link.
     * A grouper embeds a rule for grouping inputs of a link (e.g. into pairs)
     *
     */
    @RequestMapping(value = "/groupers", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getAvailableGroupers() {
        if (this.groupers == null || this.groupers.get() == null) {
            List<String[]> descriptions = new ArrayList<>();
            Reflections reflections = new Reflections("ro.cs.tao");
            Set<Class<? extends Association>> groupers = reflections.getSubTypesOf(Association.class);
            for (Class<? extends Association> aClass : groupers) {
                try {
                    descriptions.add(aClass.newInstance().description());
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            descriptions.sort(Comparator.comparing(o -> o[0]));
            this.groupers = new WeakReference<>(descriptions);
        }
        return prepareResult(this.groupers.get());
    }

    /**
     * Lists all filters that can be applied to a link.
     * A filter embeds a rule for applying a certain criterion to inputs of a link.
     *
     */
    @RequestMapping(value = "/filters", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getAvailableFilters() {
        if (this.filters == null || this.filters.get() == null) {
            List<String[]> descriptions = new ArrayList<>();
            Reflections reflections = new Reflections("ro.cs.tao");
            Set<Class<? extends DataFilter>> filters = reflections.getSubTypesOf(DataFilter.class);
            for (Class<? extends DataFilter> filterClass : filters) {
                try {
                    descriptions.add(filterClass.newInstance().description());
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            descriptions.sort(Comparator.comparing(o -> o[0]));
            this.filters = new WeakReference<>(descriptions);
        }
        return prepareResult(this.filters.get());
    }

    /**
     * Lists all the projections that are known to the system (codes and definitions)
     *
     */
    @RequestMapping(value = "/projection/", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getAvailableProjections() {
        return prepareResult(Projection.getSupported());
    }

    /**
     * Lists all the projection codes (EPSG format) that are known to the system
     *
     */
    @RequestMapping(value = "/projection/codes/", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getAvailableProjectionCodes() {
        return prepareResult(Projection.getSupported().keySet());
    }
}
