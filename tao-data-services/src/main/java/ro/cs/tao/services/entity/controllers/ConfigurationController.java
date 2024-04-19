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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.TaoEnum;
import ro.cs.tao.configuration.ConfigurationCategory;
import ro.cs.tao.configuration.ConfigurationItem;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.eodata.Projection;
import ro.cs.tao.eodata.sorting.Association;
import ro.cs.tao.eodata.sorting.DataFilter;
import ro.cs.tao.eodata.sorting.DataSorter;
import ro.cs.tao.messaging.Topic;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.entity.beans.ExtendedKeyValuePair;
import ro.cs.tao.services.interfaces.ConfigurationService;
import ro.cs.tao.services.model.KeyValuePair;
import ro.cs.tao.utils.StringUtilities;
import ro.cs.tao.workspaces.RepositoryType;

import java.lang.ref.WeakReference;
import java.time.LocalDateTime;
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
    private WeakReference<Map<String, List<ExtendedKeyValuePair>>> enums;

    @Autowired
    private PersistenceManager persistenceManager;

    /**
     * Lists all the enumerations available.
     * A such enumeration must implement {@link TaoEnum}
     *
     */
    @RequestMapping(value = "/enums", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getAvailableEnumValues() {
        if (this.enums == null || this.enums.get() == null) {
            Map<String, List<ExtendedKeyValuePair>> enumValues = new HashMap<>();
            Reflections reflections = new Reflections("ro.cs.tao");
            Set<Class<? extends TaoEnum>> enums = reflections.getSubTypesOf(TaoEnum.class);
            for (Class<? extends TaoEnum> anEnum : enums) {
                List<TaoEnum> values = Arrays.stream(anEnum.getEnumConstants()).collect(Collectors.toList());
                if (anEnum.equals(RepositoryType.class)) {
                    values.removeIf(v -> !((RepositoryType) v).visible());
                }
                enumValues.put(anEnum.getName(),
                               values.stream().map(v -> new ExtendedKeyValuePair(((Enum) v).name(), v.friendlyName(), v.isVisible()))
                                       .collect(Collectors.toList()));
            }
            enumValues.put("TAO",
                           new ArrayList<>() {{
                               add(new ExtendedKeyValuePair("version",
                                                    ConfigurationManager.getInstance().getValue("tao.version"), true));
            }});
            enumValues.put("TOPIC",
                           Topic.listTopics().stream()
                                .map(t -> new ExtendedKeyValuePair(t, null, true))
                                .collect(Collectors.toList()));
            this.enums = new WeakReference<>(enumValues);
        }
        return prepareResult(this.enums.get());
    }

    /**
     * Lists all sorters that can be applied to a link.
     * A sorter embeds a rule for sorting inputs of a link
     *
     */
    @RequestMapping(value = "/sorters", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
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
    @RequestMapping(value = "/groupers", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
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
    @RequestMapping(value = "/filters", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
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
    @RequestMapping(value = "/projection/", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getAvailableProjections() {
        return prepareResult(Projection.getSupported());
    }

    /**
     * Lists all the projection codes (EPSG format) that are known to the system
     *
     */
    @RequestMapping(value = "/projection/codes/", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getAvailableProjectionCodes() {
        return prepareResult(Projection.getSupported().keySet());
    }

    /**
     * Toggles the development (test) mode on/off
     */
    @RequestMapping(value = "/devmode", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> toggleDevMode(@RequestParam("on") boolean mode) {
        ConfigurationManager.getInstance().setValue("tao.dev.mode", String.valueOf(mode));
        return prepareResult("Development mode is " + (mode ? "on" : "off"));
    }

    @RequestMapping(value = "/detail/items", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getConfigurationItems(@RequestParam(name = "filter", required = false) String filter,
                                                                    @RequestParam(name = "category", required = false) Integer categoryId,
                                                                    @RequestParam(name = "grouped", required = false, defaultValue = "false") boolean grouped) {
        try {
            if (categoryId != null) {
                final ConfigurationCategory category = persistenceManager.configuration().getCategory(categoryId);
                if (category == null) {
                    throw new IllegalArgumentException("Invalid category");
                }
                final List<ConfigurationItem> items = persistenceManager.configuration().getCategoryItems(category);
                if (!StringUtilities.isNullOrEmpty(filter)) {
                    items.removeIf(i -> !i.getId().contains(filter));
                }
                return prepareResult(items);
            } else {
                if (grouped) {
                    final Map<String, List<ConfigurationItem>> items = persistenceManager.configuration().getGroupedItems();
                    if (!StringUtilities.isNullOrEmpty(filter)) {
                        items.entrySet().removeIf(current -> current.getValue().stream().noneMatch(item -> item.getId().contains(filter)));
                    }
                    // sort items by category order
                    final Map<String, List<ConfigurationItem>> sortedItems = items.entrySet().stream().sorted(Comparator.comparing(item -> item.getValue().get(0).getCategory().getOrder()))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));
                    return prepareResult(sortedItems);
                } else {
                    return StringUtilities.isNullOrEmpty(filter)
                           ? prepareResult(persistenceManager.configuration().getItems())
                           : prepareResult(persistenceManager.configuration().getItems(filter));
                }
            }
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/detail/categories", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getCategories() {
        return prepareResult(persistenceManager.configuration().getCategories());
    }

    @RequestMapping(value = "/detail/items", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> saveConfigurationItems(@RequestBody List<ConfigurationItem> items) {
        try {
            int size = 0;
            if (items != null) {
                for (ConfigurationItem item : items) {
                    item.setLastUpdated(LocalDateTime.now());
                    persistenceManager.configuration().saveItem(item);
                }
                size = items.size();
            }
            return prepareResult(size + " configuration items saved", ResponseStatus.SUCCEEDED);
        } catch (Exception e) {
            return handleException(e);
        }
    }
}
