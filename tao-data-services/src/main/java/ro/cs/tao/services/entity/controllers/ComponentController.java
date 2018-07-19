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

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import ro.cs.tao.component.ProcessingComponent;
import ro.cs.tao.component.SourceDescriptor;
import ro.cs.tao.component.TargetDescriptor;
import ro.cs.tao.component.enums.ProcessingComponentType;
import ro.cs.tao.component.validation.ValidationException;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.interfaces.ComponentService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Cosmin Cara
 */
@Controller
@RequestMapping("/component")
public class ComponentController extends DataEntityController<ProcessingComponent, ComponentService> {

    @RequestMapping(value = "/user", method = RequestMethod.GET)
    public ResponseEntity<?> listUserComponents(@RequestParam("type") ProcessingComponentType componentType) {
        ResponseEntity<?> response = null;
        String userName = SessionStore.currentContext().getPrincipal().getName();
        switch (componentType) {
            case EXECUTABLE:
                response = new ResponseEntity<>(service.getUserProcessingComponents(userName),
                                                HttpStatus.OK);
                break;
            case SCRIPT:
                response = new ResponseEntity<>(service.getUserScriptComponents(userName),
                                                HttpStatus.OK);
                break;
        }
        return response;
    }

    @Override
    public ResponseEntity<?> save(ProcessingComponent entity) {
        entity.setOwner(SessionStore.currentContext().getPrincipal().getName());
        return super.save(entity);
    }

    @Override
    public ResponseEntity<?> update(String id, ProcessingComponent entity) {
        if (isCurrentUserAdmin()) {
            return super.update(id, entity);
        } else {
            return prepareResult("Not authorized", ResponseStatus.FAILED);
        }
    }

    @Override
    public ResponseEntity<?> delete(String id) throws PersistenceException {
        if (isCurrentUserAdmin()) {
            return super.delete(id);
        } else {
            return prepareResult("Not authorized", ResponseStatus.FAILED);
        }
    }

    @RequestMapping(value = "/constraints", method = RequestMethod.GET)
    public ResponseEntity<?> listConstraints() {
        final List<String> objects = service.getAvailableConstraints();
        if (objects == null || objects.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(objects, HttpStatus.OK);
    }

    @RequestMapping(value = "/import", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<?> importFrom(@RequestBody ProcessingComponent component) {
        ResponseEntity<?> responseEntity;
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
                this.persistenceManager.saveProcessingComponent(component);
                responseEntity = new ResponseEntity<>(component, HttpStatus.OK);
            } catch (ValidationException vex) {
                responseEntity = new ResponseEntity<>(new ArrayList<>(vex.getAdditionalInfo().keySet()),
                                                      HttpStatus.OK);
            } catch (PersistenceException e) {
                responseEntity = handleException(e);
            }
        } else {
            responseEntity = new ResponseEntity<>("No body", HttpStatus.BAD_REQUEST);
        }
        return responseEntity;
    }

}
