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

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.Sort;
import ro.cs.tao.SortDirection;
import ro.cs.tao.Tag;
import ro.cs.tao.component.ProcessingComponent;
import ro.cs.tao.component.ProcessingComponentBean;
import ro.cs.tao.component.SourceDescriptor;
import ro.cs.tao.component.TargetDescriptor;
import ro.cs.tao.component.enums.ProcessingComponentType;
import ro.cs.tao.component.validation.ValidationException;
import ro.cs.tao.eodata.naming.NamingRule;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.entity.beans.NamingRuleInfo;
import ro.cs.tao.services.entity.util.ServiceTransformUtils;
import ro.cs.tao.services.interfaces.ComponentService;
import ro.cs.tao.services.interfaces.GroupComponentService;
import ro.cs.tao.services.interfaces.NameTokenService;
import ro.cs.tao.utils.StringUtilities;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Cosmin Cara
 */
@RestController
@RequestMapping("/component")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Components", description = "Operations related to components (executables)")
public class ComponentController extends DataEntityController<ProcessingComponent, String, ComponentService> {

    @Autowired
    private GroupComponentService groupComponentService;

    @Autowired
    private NameTokenService nameTokenService;

    /**
     * Lists the components of a given type that are visible to the current user
     * @param componentType The type of the components (@see {@link ProcessingComponentType})
     *
     */
    @RequestMapping(value = "/user", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> listUserComponents(@RequestParam("type") ProcessingComponentType componentType) {
        ResponseEntity<ServiceResponse<?>> response;
        String userName = SessionStore.currentContext().getPrincipal().getName();
        switch (componentType) {
            case EXECUTABLE:
                response = prepareResult(service.getUserProcessingComponents(userName));
                break;
            case SCRIPT:
                response = prepareResult(service.getUserScriptComponents(userName));
                break;
            default:
                response = prepareResult("Unknown component type", ResponseStatus.FAILED);
        }
        return response;
    }

    /**
     * Lists all the components registered in the system, optionally providing paging and sorting information.
     * @param pageNumber        (optional) the page number
     * @param pageSize          (optional) the page size
     * @param sortByField       (optional) the field to sort on
     * @param sortDirection     (optional) the sort direction
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @RequestMapping(value = "/", method = RequestMethod.GET, produces = "application/json")
    @Override
    public ResponseEntity<ServiceResponse<?>> list(@RequestParam(name = "pageNumber", required = false) Optional<Integer> pageNumber,
                                                   @RequestParam(name = "pageSize", required = false) Optional<Integer> pageSize,
                                                   @RequestParam(name = "sortBy", required = false) Optional<String> sortByField,
                                                   @RequestParam(name = "sortDirection", required = false) Optional<SortDirection> sortDirection) {
        if (pageNumber.isPresent() && sortByField.isPresent()) {
            Sort sort = new Sort().withField(sortByField.get(), sortDirection.orElse(SortDirection.ASC));
            return prepareResult(ServiceTransformUtils.toProcessingComponentInfos(service.list(pageNumber, pageSize, sort)));
        } else {
            return prepareResult(ServiceTransformUtils.toProcessingComponentInfos(service.list()));
        }
    }

    /**
     * Returns the descriptors of one or more components, given their ids.
     * @param idList    The list of identifiers
     */
    @RequestMapping(value = "/list", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> list(@RequestParam(name = "id") String idList) {
        if (idList == null || idList.isEmpty()) {
            return prepareResult("Invalid id list", ResponseStatus.FAILED);
        }
        String[] ids = idList.split(",");
        return prepareResult(service.list(Arrays.asList(ids)).stream().map(ProcessingComponentBean::new).collect(Collectors.toList()));
    }

    /**
     * Returns the descriptors of one or more group components given their ids.
     * A group component relates to a node group.
     * @param groupIdList   The list of identifiers
     *
     */
    @RequestMapping(value = "/group", method = RequestMethod.GET, produces = "application/json")
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
    @RequestMapping(value = "/naming/", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getNamingRules() {
        return prepareResult(nameTokenService.list().stream().map(NamingRuleInfo::new).collect(Collectors.toList()));
    }

    /**
     * Returns the tokens of a naming rule associated with the given sensor.
     * @param sensor    The sensor
     *
     */
    @RequestMapping(value = "/naming/tokens", method = RequestMethod.GET, produces = "application/json")
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
    @RequestMapping(value = "/naming/find", method = RequestMethod.GET, produces = "application/json")
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
    @RequestMapping(value = "/naming/{id}", method = RequestMethod.GET, produces = "application/json")
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
    @RequestMapping(value = "/naming", method = RequestMethod.POST, produces = "application/json")
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
    @RequestMapping(value = "/naming", method = RequestMethod.PUT, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> updateRule(@RequestBody NamingRule rule) {
        return saveRule(rule);
    }
    /**
     * Removes a naming rule
     * @param id    The rule identifier
     *
     */
    @RequestMapping(value = "/naming/{id}", method = RequestMethod.DELETE, produces = "application/json")
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
    @RequestMapping(value = "/{id:.+}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> get(String id) {
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
     * Creates a new processing component for the current user
     * @param entity    The descriptor of the processing component
     *
     */
    @RequestMapping(value = "/", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
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
    @RequestMapping(value = "/{id:.+}", method = RequestMethod.PUT, consumes = "application/json", produces = "application/json")
    @Override
    public ResponseEntity<ServiceResponse<?>> update(@PathVariable("id") String id, @RequestBody ProcessingComponent entity) {
        if (currentUser().equals(entity.getOwner()) || isCurrentUserAdmin()) {
            return super.update(id, entity);
        } else {
            return prepareResult("Not authorized", ResponseStatus.FAILED);
        }
    }

    /**
     * Removes a processing component.
     * Only the owner of the component or an administrator are allowed to invoke this method.
     * @param id    The component identifier
     *
     */
    @RequestMapping(value = "/{id:.+}", method = RequestMethod.DELETE, produces = "application/json")
    @Override
    public ResponseEntity<ServiceResponse<?>> delete(@PathVariable("id") String id) {
        if (StringUtilities.isNullOrEmpty(id)) {
            return handleException(new IllegalArgumentException("Invalid identifier"));
        }
        ProcessingComponent component = service.findById(id);
        if (component == null) {
            return handleException(new IllegalArgumentException("Invalid identifier"));
        }
        if (currentUser().equals(component.getOwner()) || isCurrentUserAdmin()) {
            return super.delete(id);
        } else {
            return prepareResult("Not authorized", ResponseStatus.FAILED);
        }
    }

    /**
     * List all the constraints that can be applied to links of a workflow
     *
     */
    @RequestMapping(value = "/constraints", method = RequestMethod.GET, produces = "application/json")
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
    @RequestMapping(value = "/tags", method = RequestMethod.GET, produces = "application/json")
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
    @RequestMapping(value = "/import", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
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

}
