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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ro.cs.tao.component.ProcessingComponent;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.services.interfaces.ComponentService;

import java.util.List;

/**
 * @author Cosmin Cara
 */
@Controller
@RequestMapping("/component")
public class ComponentController extends DataEntityController<ProcessingComponent, ComponentService> {

    @Autowired
    private PersistenceManager persistenceManager;

    @RequestMapping(value = "/constraints", method = RequestMethod.GET)
    public ResponseEntity<?> listConstraints() {
        final List<String> objects = service.getAvailableConstraints();
        if (objects == null || objects.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(objects, HttpStatus.OK);
    }

    @RequestMapping(value = "/import", method = RequestMethod.POST, consumes = "application/json")
    public ResponseEntity<?> importFrom(@RequestBody ProcessingComponent component) throws PersistenceException {
        if (component != null) {
            String componentId = component.getId();
            if (componentId != null) {
                try {
                    component = this.persistenceManager.getProcessingComponentById(componentId);
                } catch (PersistenceException e) {
                    component = this.persistenceManager.saveProcessingComponent(component);
                }
            } else {
                component = this.persistenceManager.updateProcessingComponent(component);
            }
            return new ResponseEntity<>(component, HttpStatus.OK);
        } else {
            return new ResponseEntity<>("No body", HttpStatus.BAD_REQUEST);
        }
    }

}
