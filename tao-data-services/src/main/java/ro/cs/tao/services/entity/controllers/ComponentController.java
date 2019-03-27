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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.Sort;
import ro.cs.tao.SortDirection;
import ro.cs.tao.Tag;
import ro.cs.tao.component.ProcessingComponent;
import ro.cs.tao.component.SourceDescriptor;
import ro.cs.tao.component.TargetDescriptor;
import ro.cs.tao.component.enums.ProcessingComponentType;
import ro.cs.tao.component.validation.ValidationException;
import ro.cs.tao.eodata.naming.NamingRule;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.entity.beans.NamingRuleInfo;
import ro.cs.tao.services.entity.util.ServiceTransformUtils;
import ro.cs.tao.services.interfaces.ComponentService;
import ro.cs.tao.services.interfaces.GroupComponentService;
import ro.cs.tao.services.interfaces.NameTokenService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Cosmin Cara
 */
@Controller
@RequestMapping("/component")
public class ComponentController extends DataEntityController<ProcessingComponent, String, ComponentService> {

    @Autowired
    private GroupComponentService groupComponentService;

    @Autowired
    private NameTokenService nameTokenService;

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

    @RequestMapping(value = "/list", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> list(@RequestParam(name = "id") String idList) {
        if (idList == null || idList.isEmpty()) {
            return prepareResult("Invalid id list", ResponseStatus.FAILED);
        }
        String[] ids = idList.split(",");
        return prepareResult(service.list(Arrays.asList(ids)));
    }

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

    @RequestMapping(value = "/naming/", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getNamingRules() {
        return prepareResult(nameTokenService.list().stream().map(NamingRuleInfo::new).collect(Collectors.toList()));
    }

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

    @RequestMapping(value = "/naming/{id}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getNamingRuleDetails(@PathVariable("id") int id) {
        try {
            return prepareResult(nameTokenService.findById(id));
        } catch (PersistenceException e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/naming", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> saveRule(@RequestBody NamingRule rule) {
        try {
            return prepareResult(nameTokenService.save(rule));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/naming", method = RequestMethod.PUT, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> updateRule(@RequestBody NamingRule rule) {
        return saveRule(rule);
    }

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

    @RequestMapping(value = "/", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    @Override
    public ResponseEntity<ServiceResponse<?>> save(@RequestBody ProcessingComponent entity) {
        if (entity.getOwner() == null || entity.getOwner().isEmpty()) {
            entity.setOwner(SessionStore.currentContext().getPrincipal().getName());
        }
        return super.save(entity);
    }

    @RequestMapping(value = "/{id:.+}", method = RequestMethod.PUT, consumes = "application/json", produces = "application/json")
    @Override
    public ResponseEntity<ServiceResponse<?>> update(@PathVariable("id") String id, @RequestBody ProcessingComponent entity) {
        if (isCurrentUserAdmin()) {
            return super.update(id, entity);
        } else {
            return prepareResult("Not authorized", ResponseStatus.FAILED);
        }
    }

    @RequestMapping(value = "/{id:.+}", method = RequestMethod.DELETE, produces = "application/json")
    @Override
    public ResponseEntity<ServiceResponse<?>> delete(@PathVariable("id") String id) {
        if (isCurrentUserAdmin()) {
            return super.delete(id);
        } else {
            return prepareResult("Not authorized", ResponseStatus.FAILED);
        }
    }

    @RequestMapping(value = "/constraints", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> listConstraints() {
        List<String> objects = service.getAvailableConstraints();
        if (objects == null ) {
            objects = new ArrayList<>();
        }
        return prepareResult(objects);
    }

    @RequestMapping(value = "/tags", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> listTags() {
        List<Tag> objects = service.getComponentTags();
        if (objects == null ) {
            objects = new ArrayList<>();
        }
        return prepareResult(objects.stream().map(Tag::getText).collect(Collectors.toList()));
    }

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
                getPersistenceManager().saveProcessingComponent(component);
                response = prepareResult(component);
            } catch (ValidationException | PersistenceException vex) {
                response = handleException(vex);
            }
        } else {
            response =prepareResult("Empty request body", ResponseStatus.FAILED);
        }
        return response;
    }

}
