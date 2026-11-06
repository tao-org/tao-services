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
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import ro.cs.tao.TaoEnum;
import ro.cs.tao.configuration.ConfigurationCategory;
import ro.cs.tao.configuration.ConfigurationItem;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.configuration.UserConfigurationItem;
import ro.cs.tao.eodata.Projection;
import ro.cs.tao.eodata.naming.ResolverFunction;
import ro.cs.tao.eodata.naming.TokenResolver;
import ro.cs.tao.eodata.sorting.Association;
import ro.cs.tao.eodata.sorting.DataFilter;
import ro.cs.tao.eodata.sorting.DataSorter;
import ro.cs.tao.messaging.Emitter;
import ro.cs.tao.messaging.Message;
import ro.cs.tao.messaging.Topic;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.services.commons.*;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.menu.Section;
import ro.cs.tao.services.commons.menu.SectionEntry;
import ro.cs.tao.services.commons.menu.ServiceEntry;
import ro.cs.tao.services.entity.beans.ExtendedKeyValuePair;
import ro.cs.tao.services.interfaces.ConfigurationService;
import ro.cs.tao.services.model.KeyValuePair;
import ro.cs.tao.spi.ServiceRegistryManager;
import ro.cs.tao.user.User;
import ro.cs.tao.utils.StringUtilities;
import ro.cs.tao.workspaces.RepositoryType;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Cosmin Cara
 */
@RestController
@RequestMapping({ "/config", "/config/" })
@io.swagger.v3.oas.annotations.tags.Tag(name = "Configuration", description = "Operations related to configuration")
public class ConfigurationController
        extends DataEntityController<KeyValuePair, String, ConfigurationService>
        implements Emitter {

    private WeakReference<List<String>> sorters;
    private WeakReference<List<String[]>> groupers;
    private WeakReference<List<String[]>> filters;
    private WeakReference<Map<String, List<ExtendedKeyValuePair>>> enums;
    private WeakReference<Map<String, List<ResolverFunction>>> functions;

    @Autowired
    private PersistenceManager persistenceManager;

    /**
     * Lists all the enumerations available.
     * A such enumeration must implement {@link TaoEnum}
     *
     */
    @RequestMapping(value = { "/enums", "/enums/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
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
    @RequestMapping(value = { "/sorters", "/sorters/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
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
    @RequestMapping(value = { "/groupers", "/groupers/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
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
    @RequestMapping(value = { "/filters", "/filters/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
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
    @RequestMapping(value = { "/projection/", "/projection" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getAvailableProjections() {
        return prepareResult(Projection.getSupported());
    }

    /**
     * Lists all the projection codes (EPSG format) that are known to the system
     *
     */
    @RequestMapping(value = { "/projection/codes/", "/projection/codes" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getAvailableProjectionCodes() {
        return prepareResult(Projection.getSupported().keySet());
    }

    /**
     * Returns a list of functions that can be used in component templates and/or parameter values
     */
    @RequestMapping(value = { "/functions", "/functions/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getAvailableFunctions() {
        if (this.functions == null || this.functions.get() == null) {
            final Map<String, List<ResolverFunction>> descriptions = new TreeMap<>();
            final Set<TokenResolver> resolvers = ServiceRegistryManager.getInstance().getServiceRegistry(TokenResolver.class).getServices();
            for (TokenResolver resolver : resolvers) {
                final List<ResolverFunction> list = resolver.definedFunctions();
                if (!list.isEmpty()) {
                    for (ResolverFunction function : list) {
                        final String category = function.getCategory();
                        if (!descriptions.containsKey(category)) {
                            descriptions.put(category, new ArrayList<>());
                        }
                        descriptions.get(category).add(function);
                    }
                }
            }
            this.functions = new WeakReference<>(descriptions);
        }
        return prepareResult(this.functions.get());
    }

    /**
     * Toggles the development (test) mode on/off
     */
    @RequestMapping(value = { "/devmode", "/devmode/" }, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @RoleRequired(roles = ro.cs.tao.user.Role.ADMIN)
    public ResponseEntity<ServiceResponse<?>> toggleDevMode(@RequestParam("on") boolean mode) {
        ConfigurationManager.getInstance().setValue("tao.dev.mode", String.valueOf(mode));
        return prepareResult("Development mode is " + (mode ? "on" : "off"));
    }

    /**
     * Retrieves the configuration items based on provided filters, category, and grouping options.
     *
     * @param filter     An optional filter string used to match against item IDs. If provided,
     *                   only items whose IDs contain the filter string will be returned.
     * @param categoryId An optional category ID. If specified, only configuration items belonging
     *                   to the specified category will be retrieved.
     * @param grouped    An optional flag indicating whether the items should be grouped by category.
     *                   Defaults to false, meaning items will not be grouped.
     * @param userId     If present, only configuration items set by the user will be returned
     * @return A {@link ResponseEntity} containing a {@link ServiceResponse} with
     *         the retrieved configuration items. If a category ID is invalid or an error occurs,
     *         an appropriate error response will be returned.
     */
    @RequestMapping(value = { "/detail/items", "/detail/items/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getConfigurationItems(@RequestParam(name = "filter", required = false) String filter,
                                                                    @RequestParam(name = "category", required = false) Integer categoryId,
                                                                    @RequestParam(name = "grouped", required = false, defaultValue = "false") boolean grouped,
                                                                    @RequestParam(name = "userId", required = false) String userId) {
        if (!StringUtilities.isNullOrEmpty(userId) &&
                !(currentUser().equals(userId) || currentOwner().equals(userId) || isCurrentUserAdmin())) {
            return prepareResult(null, HttpStatus.FORBIDDEN);
        }
        if (StringUtilities.isNullOrEmpty(userId) && !isCurrentUserAdmin()) {
            return prepareResult(ConfigurationManager.getInstance().getAll().entrySet().stream()
                    .filter(e -> ConfigurationAccess.isPublic(e.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        }
        try {
            if (!StringUtilities.isNullOrEmpty(userId)) {
                User user = persistenceManager.users().get(userId);
                final String id = user.getWorkspaceId() != null ? user.getWorkspaceId() : userId;
                final List<UserConfigurationItem> items = StringUtilities.isNullOrEmpty(filter)
                        ? persistenceManager.userConfiguration().getItems(id)
                        : persistenceManager.userConfiguration().getItems(id, filter);
                if (grouped) {
                    final Map<String, List<UserConfigurationItem>> groupedItems =
                            items.stream().collect(Collectors.groupingBy(item -> item.getItem().getCategory().getName(), Collectors.collectingAndThen(Collectors.toList(), Collections::<UserConfigurationItem> unmodifiableList)));
                    // sort items by category order
                    if (groupedItems.keySet().size() == 1) {
                        return prepareResult(items);
                    } else {
                        final Map<String, List<UserConfigurationItem>> sortedItems = groupedItems.entrySet().stream()
                                                                                                 .sorted(Comparator.comparing(item -> item.getValue().get(0).getItem().getCategory().getOrder()))
                                                                                                 .collect(Collectors.toMap(Map.Entry::getKey,
                                                                                                                           Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));
                        return prepareResult(sortedItems);
                    }
                } else {
                    return prepareResult(items);
                }
            } else {
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
            }
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * Retrieves the list of configuration categories available in the system.
     *
     * @return a {@link ResponseEntity} containing a {@link ServiceResponse} with
     *         the retrieved list of configuration categories. The HTTP status is
     *         {@code OK} if the operation is successful.
     */
    @RequestMapping(value = { "/detail/categories", "/detail/categories/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getCategories() {
        return prepareResult(persistenceManager.configuration().getCategories());
    }

    /**
     * Saves a list of configuration items.
     * This method updates the `lastUpdated` timestamp for each item and persists them into the configuration storage.
     * If the list is not empty, a notification message regarding the configuration change is sent to the appropriate topic.
     *
     * @param items the list of configuration items to be saved. Each item should contain the necessary details to be updated
     *              or inserted into the configuration storage.
     * @return a {@link ResponseEntity} containing a {@link ServiceResponse} object that represents the outcome of the
     *         operation. If the operation succeeds, a success response is returned with the count of saved items. If an
     *         error occurs, an appropriate error response is returned.
     */
    @RoleRequired(roles = ro.cs.tao.user.Role.ADMIN)
    @RequestMapping(value = { "/detail/items", "/detail/items/" }, method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> saveConfigurationItems(@RequestBody List<ConfigurationItem> items) {
        try {
            int size = 0;
            if (items != null && !items.isEmpty()) {
                final Message message = Message.create(currentUser(), getClass().getSimpleName(), "Configuration changed", false);
                for (ConfigurationItem item : items) {
                    /*item.setLastUpdated(LocalDateTime.now());
                    persistenceManager.configuration().saveItem(item);*/
                    ConfigurationManager.getInstance().setValue(item.getId(), item.getValue());
                    message.addItem(item.getId(), item.getValue());
                }
                size = items.size();
                send(Topic.CONFIGURATION, message);
            }
            return prepareResult(size + " configuration items saved", ResponseStatus.SUCCEEDED);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = { "/detail/items/{userId}", "/detail/items/{userId}/" }, method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> saveUserConfigurationItems(@PathVariable("userId") String userId,
                                                                         @RequestBody List<UserConfigurationItem> items) {
        if (!(currentUser().equals(userId) || currentOwner().equals(userId) || isCurrentUserAdmin())) {
            return prepareResult(null, HttpStatus.FORBIDDEN);
        }
        try {
            int size = 0;
            if (items != null && !items.isEmpty()) {
                final Message message = Message.create(currentUser(), getClass().getSimpleName(), "Configuration changed", false);
                User user = persistenceManager.users().get(userId);
                final String id = user.getWorkspaceId() != null ? user.getWorkspaceId() : userId;
                // Validate the entire batch before changing either runtime or persisted settings.
                Map<String, ConfigurationItem> allowed = persistenceManager.configuration().getItems().stream()
                        .filter(ConfigurationItem::isPerUser)
                        .collect(Collectors.toMap(ConfigurationItem::getId, v -> v));
                for (UserConfigurationItem item : items) {
                    if (item == null || !allowed.containsKey(item.getKey())) {
                        throw new IllegalArgumentException("Setting is not user-configurable");
                    }
                }
                for (UserConfigurationItem item : items) {
                    String value = item.getValue();
                    item.setItem(allowed.get(item.getKey()));
                    item.setValue(value);
                    item.setUserId(id);
                    ConfigurationManager.getInstance().setUserValue(id, item.getId().getKey(), item.getValue());
                    persistenceManager.userConfiguration().saveItem(item);
                    message.addItem(item.getItem().getId(), item.getValue());
                }
                size = items.size();
                send(Topic.CONFIGURATION, message);
            }
            return prepareResult(size + " configuration items saved", ResponseStatus.SUCCEEDED);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * Changes the log level of the application by forwarding the request to the
     * actuator's /loggers endpoint. The desired log level is specified as a parameter.
     * This method requires admin privileges to execute.
     *
     * @param level the desired log level to set. This should be a valid log level
     *              (e.g., DEBUG, INFO, WARN, ERROR) and is case-insensitive.
     * @return a {@link ResponseEntity} containing a {@link ServiceResponse} object
     *         indicating the result of the operation. If successful, a message confirming
     *         the updated log level is returned. In case of an error, an appropriate error
     *         response is returned.
     */
    @RequestMapping(value = { "/log", "/log/" }, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @RoleRequired(roles = ro.cs.tao.user.Role.ADMIN)
    public ResponseEntity<ServiceResponse<?>> changeLogLevel(@RequestParam("level") String level) {
        try {
            final RestTemplate template = new RestTemplate();
            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            final MultiValueMap<String, String> map = new LinkedMultiValueMap<>() {{
                add("configuredLevel", level.toUpperCase());
            }};
            // The request will be forwaded to the actuator's /loggers endpoint
            template.postForEntity("/actuator/loggers", new HttpEntity<>(map, headers), String.class);
            return prepareResult("Log level changed to " + level);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * Retrieves the menu entries available in the system.
     * The method organizes service entries into sections based on their defined categories
     * and sorts them according to their positions within each section.
     *
     * @return a {@link ResponseEntity} containing a {@link ServiceResponse} object with
     *         the organized menu entries. If the operation succeeds, the response will
     *         contain the menu entries structured by sections. In case of an error, an
     *         appropriate error response is returned.
     */
    @RequestMapping(value = { "/menu", "/menu/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getMenuEntries() {
        try {
            final Map<Section, SectionEntry> sections = new LinkedHashMap<>();
            Arrays.stream(Section.values()).sorted(new Comparator<Section>() {
                @Override
                public int compare(Section o1, Section o2) {
                    return Integer.compare(o1.value(), o2.value());
                }
            }).toList().forEach(s -> sections.put(s, new SectionEntry(s)));
            final Set<ServiceLauncher> services = ServiceRegistryManager.getInstance()
                                                                        .getServiceRegistry(ServiceLauncher.class)
                                                                        .getServices();
            for (ServiceLauncher service : services) {
                final List<ServiceEntry> entries = service.getEntries();
                for (ServiceEntry entry : entries) {
                    final Section section = entry.getSection();
                    sections.get(section).addItem(entry);
                }
            }
            sections.entrySet().forEach(e -> {
                e.getValue().getItems().sort(new Comparator<ServiceEntry>() {
                    @Override
                    public int compare(ServiceEntry o1, ServiceEntry o2) {
                        return Integer.compare(o1.getPosition(), o2.getPosition());
                    }
                });
            });
            return prepareResult(sections.values());
        } catch (Exception e) {
            return handleException(e);
        }
    }
}
