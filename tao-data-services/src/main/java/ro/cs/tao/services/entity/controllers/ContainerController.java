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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ro.cs.tao.docker.Container;
import ro.cs.tao.services.interfaces.ContainerService;
import ro.cs.tao.topology.TopologyManager;

import java.util.List;

/**
 * @author Cosmin Cara
 */
@Controller
@RequestMapping("/docker")
public class ContainerController extends DataEntityController<Container, ContainerService> {
    @Override
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ResponseEntity<List<Container>> list() {
        List<Container> objects = TopologyManager.getInstance().getAvailableDockerImages();
        if (objects == null || objects.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(objects, HttpStatus.OK);
    }
}
