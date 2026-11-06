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

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ro.cs.tao.Sort;
import ro.cs.tao.SortDirection;
import ro.cs.tao.Tag;
import ro.cs.tao.component.*;
import ro.cs.tao.component.enums.ProcessingComponentType;
import ro.cs.tao.component.enums.ProcessingComponentVisibility;
import ro.cs.tao.component.validation.ValidationException;
import ro.cs.tao.eodata.naming.NamingRule;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.repository.ContainerRepository;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.entity.beans.NamingRuleInfo;
import ro.cs.tao.services.entity.util.ServiceTransformUtils;
import ro.cs.tao.services.interfaces.ComponentService;
import ro.cs.tao.services.interfaces.GroupComponentService;
import ro.cs.tao.services.interfaces.NameTokenService;
import ro.cs.tao.services.model.component.ProcessingComponentInfo;
import ro.cs.tao.utils.AutoEvictableMemoryCache;
import ro.cs.tao.utils.FileUtilities;
import ro.cs.tao.utils.GenericComparator;
import ro.cs.tao.utils.StringUtilities;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The ComponentController class manages various operations related to components,
 * including listing, retrieving, creating, updating, and deleting components,
 * as well as handling naming rules and tokens associated with components.
 *
 * This controller provides RESTful endpoints for component and naming rule management.
 * The class includes user-specific functionality, comprehensive query options, and administrative operations.
 *
 * Fields:
 * - componentCache: Manages caching for components to enhance performance.
 * - groupComponentService: Manages operations related to group components.
 * - nameTokenService: Handles services regarding naming rules and tokens.
 * - containerRepository: Repository for managing component data persistence.
 *
 * Methods:
 * - listUserComponents: Lists components of a specified type visible to the current user.
 * - list: Lists all registered components with optional pagination and sorting.
 * - list (overloaded): Retrieves descriptors for specified component IDs.
 * - findLabels: Finds and suggests labels for components based on input text.
 * - getGroupComponent: Retrieves descriptors for group components by their IDs.
 * - getNamingRules: Returns a list of available naming rules.
 * - getTokens: Gets naming rule tokens associated with a specific sensor.
 * - getNamingRuleDetails (overloaded 1): Retrieves tokens available for a workflow node by its ID.
 * - getNamingRuleDetails (overloaded 2): Retrieves details of a naming rule by its ID.
 * - saveRule: Creates a new naming rule.
 * - updateRule: Updates an existing naming rule.
 * - deleteRule: Deletes a naming rule by its ID.
 * - get: Retrieves a processing component descriptor by its ID.
 * - export: Exports a processing component descriptor as a JSON file.
 * - save: Creates a new processing component for the current user.
 * - update: Updates a processing component, accessible only to the owner or an administrator.
 * - updateTemplateParameter: Updates a template parameter of a processing component; restricted to owners or administrators.
 *
 * @author Cosmin Cara
 */
@RestController
@RequestMapping({ "/component", "/component/" })
@io.swagger.v3.oas.annotations.tags.Tag(name = "Components", description = "Operations related to components (executables)")
public class ComponentController extends DataEntityController<ProcessingComponent, String, ComponentService> {

    private final AutoEvictableMemoryCache<String, ProcessingComponentInfo> componentCache
            = new AutoEvictableMemoryCache<>(s -> new ProcessingComponentInfo(service.findById(s)), 1800);

    @Autowired
    private GroupComponentService groupComponentService;

    @Autowired
    private NameTokenService nameTokenService;
    @Autowired
    @Qualifier("containerRepository")
    private ContainerRepository containerRepository;

    @PostConstruct
    public void initializeCache() {
        final List<ProcessingComponentInfo> infos = ServiceTransformUtils.toProcessingComponentInfos(service.list());
        final Map<String, Boolean> fields = new HashMap<>();
        fields.put("id", true);
        infos.sort(new GenericComparator(ProcessingComponentInfo.class, fields));
        for (ProcessingComponentInfo info : infos) {
            this.componentCache.put(info.getId(), info);
        }
    }

    @Override
    protected Object filterResults(List<ProcessingComponent> entities) {
        return ServiceTransformUtils.toProcessingComponentInfos(entities);
    }

    @Override
    protected Object listResults(List<ProcessingComponent> entities) {
        return entities.stream().map(ProcessingComponentBean::new).collect(Collectors.toList());
    }

    /**
     * Lists the components of a given type that are visible to the current user
     * @param componentType The type of the components (@see {@link ProcessingComponentType})
     *
     */
    @RequestMapping(value = { "/user", "/user/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> listUserComponents(@RequestParam("type") ProcessingComponentType componentType) {
        ResponseEntity<ServiceResponse<?>> response;
        String userId = currentOwner();
        switch (componentType) {
            case EXECUTABLE:
                response = prepareResult(service.getUserProcessingComponents(userId));
                break;
            case SCRIPT:
                response = prepareResult(service.getUserScriptComponents(userId));
                break;
            default:
                response = prepareResult("Unknown component type", ResponseStatus.FAILED);
        }
        return response;
    }

    /**
     * Returns the descriptors of one or more group components given their ids.
     * A group component relates to a node group.
     * @param groupIdList   The list of identifiers
     *
     */
    @RequestMapping(value = { "/group", "/group/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getGroupComponent(@RequestParam(name = "id") String groupIdList) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        if (groupIdList == null || groupIdList.isEmpty()) {
            responseEntity = prepareResult("Invalid group ids", ResponseStatus.FAILED);
        } else {
            String[] ids = groupIdList.split(",");
            responseEntity = prepareResult(groupComponentService.list(Arrays.asList(ids)));
        }
        return responseEntity;
    }

    /**
     * Returns the list of available naming rules.
     * A naming rule is a set of regexes that are mapped to tokens in the name of products of a known type (i.e. Sentinel-2)
     *
     */
    @RequestMapping(value = { "/naming/", "/naming" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getNamingRules(@RequestParam(name = "pageNumber", required = false) Optional<Integer> pageNumber,
                                                             @RequestParam(name = "pageSize", required = false) Optional<Integer> pageSize,
                                                             @RequestParam(name = "sortBy", required = false) Optional<String> sortByField,
                                                             @RequestParam(name = "sortDirection", required = false) Optional<SortDirection> sortDirection) {
        final List<NamingRule> rules;
        if (pageNumber.isPresent()) {
            Sort sort = new Sort().withField(sortByField.orElse("id"), sortDirection.orElse(SortDirection.ASC));
            rules = nameTokenService.list(pageNumber, pageSize.or(() -> Optional.of(10)), sort);
        } else {
            rules = nameTokenService.list();
        }
        return prepareResult(rules.stream().map(NamingRuleInfo::new).collect(Collectors.toList()));
    }

    @RequestMapping(value = { "/naming/count", "/naming/count/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> countNamingRules() {
        return prepareResult(nameTokenService.count());
    }

    /**
     * Returns the tokens of a naming rule associated with the given sensor.
     * @param sensor    The sensor
     *
     */
    @RequestMapping(value = { "/naming/tokens", "/naming/tokens/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getTokens(@RequestParam(name = "sensor") String sensor) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        if (StringUtils.isBlank(sensor)) {
            responseEntity = prepareResult("Invalid sensor value", ResponseStatus.FAILED);
        } else {
            responseEntity = prepareResult(nameTokenService.getNameTokens(sensor));
        }
        return responseEntity;
    }

    /**
     * Returns the tokens that are available for a workflow node.
     * @param nodeId    The identifier of the workflow node
     *
     */
    @RequestMapping(value = { "/naming/find", "/naming/find/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getNamingRuleDetails(@RequestParam("nodeId") long nodeId) {
        try {
            return prepareResult(nameTokenService.findTokens(nodeId));
        } catch (PersistenceException e) {
            return handleException(e);
        }
    }

    /**
     * Returns the details of a naming rule
     * @param id    The identifier of the naming rule
     *
     */
    @RequestMapping(value = { "/naming/{id}", "/naming/{id}/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> getNamingRuleDetails(@PathVariable("id") int id) {
        try {
            return prepareResult(nameTokenService.findById(id));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * Creates a new naming rule
     * @param rule  The naming rule description, containing the regex and the matching tokens
     *
     */
    @RequestMapping(value = { "/naming", "/naming/" }, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> saveRule(@RequestBody NamingRule rule) {
        try {
            return prepareResult(nameTokenService.save(rule));
        } catch (Exception e) {
            return handleException(e);
        }
    }
    /**
     * Updates a naming rule
     * @param rule  The naming rule description, containing the regex and the matching tokens
     *
     */
    @RequestMapping(value = { "/naming", "/naming/" }, method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> updateRule(@RequestBody NamingRule rule) {
        try {
            return prepareResult(nameTokenService.update(rule));
        } catch (Exception e) {
            return handleException(e);
        }
    }
    /**
     * Removes a naming rule
     * @param id    The rule identifier
     *
     */
    @RequestMapping(value = { "/naming/{id}", "/naming/{id}/" }, method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> deleteRule(@PathVariable("id") int id) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            nameTokenService.delete(id);
            responseEntity = prepareResult("Entity deleted", ResponseStatus.SUCCEEDED);
        } catch (PersistenceException e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

    /**
     * Returns a processing component descriptor by id.
     * @param id    The component identifier
     *
     */
    @Override
    @RequestMapping(value = { "/{id:.+}", "/{id:.+}/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> get(@PathVariable("id") String id) {
        ResponseEntity<ServiceResponse<?>> response;
        ProcessingComponent entity;
        try {
            entity = service.findById(id);
            if (entity == null) {
                response = prepareResult(String.format("Entity [%s] not found", id), ResponseStatus.FAILED);
            } else {
                response = prepareResult(new ProcessingComponentBean(entity));
            }
        } catch (Exception e) {
            response = handleException(e);
        }
        return response;
    }

    /**
     * Export a processing component descriptor as json file.
     * @param id    The component identifier
     *
     */
    @RequestMapping(value = { "/{id:.+}/export", "/{id:.+}/export/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void export(@PathVariable("id") String id, HttpServletResponse response) {
        try {
            ProcessingComponent entity;
            entity = service.findById(id);
            if (entity == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else {
                response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename="
                        + FileUtilities.ensureValidFileName(entity.getId())
                        + ".json");
                response.setStatus(HttpStatus.OK.value());
                response.getOutputStream().write(service.exportComponent(entity).getBytes());
            }
        } catch (Exception e) {
            error(e.getMessage());
        }
    }

    /**
     * Creates a new processing component for the current user
     * @param entity    The descriptor of the processing component
     *
     */
    @RequestMapping(value = { "/", "" }, method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Override
    public ResponseEntity<ServiceResponse<?>> save(@RequestBody ProcessingComponent entity) {
        if (entity.getOwner() == null || entity.getOwner().isEmpty()) {
            entity.setOwner(SessionStore.currentContext().getPrincipal().getName());
        }
        return super.save(entity);
    }
    /**
     * Updates a processing component.
     * Only the owner of the component or an administrator are allowed to invoke this method.
     *
     * @param entity    The descriptor of the processing component
     *
     */
    @RequestMapping(value = { "/{id:.+}", "/{id:.+}/" }, method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Override
    public ResponseEntity<ServiceResponse<?>> update(@PathVariable("id") String id, @RequestBody ProcessingComponent entity) {
        if (ownsEntity(entity) || isCurrentUserAdmin()) {
            return super.update(id, entity);
        } else {
            return prepareResult("Not authorized", ResponseStatus.FAILED);
        }
    }

    /**
     * Updates a template parameter of a processing component.
     * Only the owner of the component or an administrator are allowed to invoke this method.
     *
     * @param parameter    The descriptor of the template parameter
     *
     */
    @RequestMapping(value = { "/{id:.+}/{parameterId:.+}", "/{id:.+}/{parameterId:.+}/" }, method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> updateTemplateParameter(@PathVariable("id") String id,
                                                                      @PathVariable("parameterId") String paramId,
                                                                      @RequestBody TemplateParameterDescriptor parameter) {
        try {
            if (StringUtilities.isNullOrEmpty(id)) {
                throw new IllegalArgumentException("[id] missing");
            }
            if (StringUtilities.isNullOrEmpty(paramId)) {
                throw new IllegalArgumentException("[parameterId] missing");
            }
            if (parameter == null) {
                throw new IllegalArgumentException("empty body");
            }
            final List<ParameterDescriptor> parameters = parameter.getParameters();
            if (parameters != null) {
                for (ParameterDescriptor parameterDescriptor : parameters) {
                    if (StringUtilities.isNullOrEmpty(parameterDescriptor.getId()))
                        parameterDescriptor.setId(UUID.randomUUID().toString());
                }
            }
            final ProcessingComponent component = service.findById(id);
            if (ownsEntity(component) || isCurrentUserAdmin()) {
                final Set<ParameterDescriptor> descriptors = component.getParameterDescriptors();
                descriptors.removeIf(p -> p.getId().equals(paramId));
                descriptors.add(parameter);
                return super.update(id, component);
            } else {
                return prepareResult("Not authorized", ResponseStatus.FAILED);
            }
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * Import a processing component descriptor from a json file.
     * @param file    The JSON file containing the processing component
     *
     */
    @RequestMapping(value = { "/import", "/import/" }, method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ServiceResponse<?>> importFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.getOriginalFilename() == null) {
                throw new IOException("Empty file");
            }
            try (InputStream stream = file.getInputStream()) {
                 return prepareResult(service.importComponent(stream));
            }
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * Removes a processing component.
     * Only the owner of the component or an administrator are allowed to invoke this method.
     * @param id    The component identifier
     *
     */
    @RequestMapping(value = { "/{id:.+}", "/{id:.+}/" }, method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Override
    public ResponseEntity<ServiceResponse<?>> delete(@PathVariable("id") String id) {
        if (StringUtilities.isNullOrEmpty(id)) {
            return handleException(new IllegalArgumentException("Invalid identifier"));
        }
        ProcessingComponent component = service.findById(id);
        if (component == null) {
            return handleException(new IllegalArgumentException("Invalid identifier"));
        }
        if (ownsEntity(component) || isCurrentUserAdmin()) {
            final ResponseEntity<ServiceResponse<?>> deleted = super.delete(id);
            this.componentCache.remove(id);
            return prepareResult(deleted);
        } else {
            return prepareResult("Not authorized", ResponseStatus.FAILED);
        }
    }
    /**
     * Clones an existing processing component for the current user
     * @param componentId    The id of the processing component
     *
     */
    @RequestMapping(value = { "/clone", "/clone/" }, method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> clone(@RequestParam("componentId") String componentId) {
        try {
            if (StringUtilities.isNullOrEmpty(componentId)) {
                throw new IllegalArgumentException("[componentId] Missing");
            }
            final ProcessingComponent original = service.findById(componentId);
            ProcessingComponent cloned = original.clone();
            cloned.setLabel("Clone of " + cloned.getLabel());
            cloned.setVisibility(ProcessingComponentVisibility.USER);
            cloned.setId(UUID.randomUUID().toString());
            cloned.setContainerId(original.getContainerId());
            cloned.setOwner(currentOwner());
            cloned = service.save(cloned);
            this.componentCache.put(cloned.getId(), new ProcessingComponentInfo(cloned));
            return prepareResult(cloned);
        } catch (Exception e) {
            return handleException(e);
        }
    }
    /**
     * List all the constraints that can be applied to links of a workflow
     *
     */
    @RequestMapping(value = { "/constraints", "/constraints/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> listConstraints() {
        List<String> objects = service.getAvailableConstraints();
        if (objects == null ) {
            objects = new ArrayList<>();
        }
        return prepareResult(objects);
    }

    /**
     * Lists all the tags that are associated with components
     *
     */
    @RequestMapping(value = { "/tags", "/tags/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> listTags() {
        List<Tag> objects = service.getComponentTags();
        if (objects == null ) {
            objects = new ArrayList<>();
        }
        return prepareResult(objects.stream().map(Tag::getText).collect(Collectors.toList()));
    }

    /**
     * Creates a processing component from a JSON descriptor.
     * @param component     The component descriptor
     *
     */
    @RequestMapping(value = { "/import", "/import/" }, method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> importFrom(@RequestBody ProcessingComponent component) {
        ResponseEntity<ServiceResponse<?>> response;
        if (component != null) {
            try {
                List<SourceDescriptor> sources = component.getSources();
                if (sources != null) {
                    sources.forEach(sourceDescriptor -> {
                        if (sourceDescriptor != null) {
                            if (sourceDescriptor.getId() == null || sourceDescriptor.getId().isEmpty()) {
                                sourceDescriptor.setId(UUID.randomUUID().toString());
                            }
                        }
                    });
                }
                List<TargetDescriptor> targets = component.getTargets();
                if (targets != null) {
                    targets.forEach(targetDescriptor -> {
                        if (targetDescriptor != null) {
                            if (targetDescriptor.getId() == null || targetDescriptor.getId().isEmpty()) {
                                targetDescriptor.setId(UUID.randomUUID().toString());
                            }
                        }
                    });
                }
                this.service.validate(component);
                this.service.save(component);
                //getPersistenceManager().saveProcessingComponent(component);
                response = prepareResult(component);
            } catch (ValidationException vex) {
                response = handleException(vex);
            }
        } else {
            response =prepareResult("Empty request body", ResponseStatus.FAILED);
        }
        return response;
    }

    @RequestMapping(value = { "/code", "/code/" }, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> openNotebook(@RequestParam("componentId") String componentId) {
        try {
            if (StringUtilities.isNullOrEmpty(componentId)) {
                throw new IllegalArgumentException("Invalid component id");
            }
            final ProcessingComponent component = this.service.findById(componentId);
            if (component == null) {
                throw new IllegalArgumentException("Component does not exist");
            }

            return prepareResult("Notebook created", ResponseStatus.SUCCEEDED);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = { "/change", "/change/" }, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> changeParameter(@RequestParam("parameterId") String parameterId,
                                                              @RequestParam("componentId") String componentId,
                                                              @RequestParam("toSourceDescriptor") boolean toSourceDescriptor) {
        try {
            if (StringUtilities.isNullOrEmpty(componentId)) {
                throw new IllegalArgumentException("Invalid component id");
            }
            final ProcessingComponent component = this.service.findById(componentId);
            if (component == null) {
                throw new IllegalArgumentException("Component does not exist");
            }
            if (toSourceDescriptor) {
                service.transformIntoSource(parameterId, component);
            } else {
                service.transformIntoTarget(parameterId, component);
            }
            return prepareResult("Processing component updated", ResponseStatus.SUCCEEDED);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = { "/toParameter", "/toParameter/" }, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> changeDescriptor(@RequestParam("descriptorId") String descriptorId,
                                                              @RequestParam("componentId") String componentId) {
        try {
            if (StringUtilities.isNullOrEmpty(componentId)) {
                throw new IllegalArgumentException("Invalid component id");
            }
            final ProcessingComponent component = this.service.findById(componentId);
            if (component == null) {
                throw new IllegalArgumentException("Component does not exist");
            }
            service.transformIntoParameter(descriptorId, component);
            return prepareResult("Processing component updated", ResponseStatus.SUCCEEDED);
        } catch (Exception e) {
            return handleException(e);
        }
    }
}

