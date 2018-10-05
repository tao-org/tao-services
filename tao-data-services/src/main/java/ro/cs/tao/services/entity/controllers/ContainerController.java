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

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import ro.cs.tao.SortDirection;
import ro.cs.tao.docker.Container;
import ro.cs.tao.messaging.Message;
import ro.cs.tao.messaging.Messaging;
import ro.cs.tao.messaging.Topics;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.ContainerService;
import ro.cs.tao.topology.TopologyManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Cosmin Cara
 */
@Controller
@RequestMapping("/docker")
public class ContainerController extends DataEntityController<Container, String, ContainerService<MultipartFile>> {

    @Override
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ResponseEntity<ServiceResponse<?>> list(Optional<Integer> pageNumber,
                                                   Optional<Integer> pageSize,
                                                   Optional<String> sortByField,
                                                   Optional<SortDirection> sortDirection) {
        List<Container> objects = TopologyManager.getInstance().getAvailableDockerImages();
        if (objects == null) {
            objects = new ArrayList<>();
        }
        return prepareResult(objects);
    }

    @PostMapping("/upload")
    public ResponseEntity<ServiceResponse<?>> upload(@RequestParam("file") MultipartFile dockerFile,
                                                     @RequestParam("name") String shortName,
                                                     @RequestParam("desc") String description) {
        asyncExecute(() -> {
            try {
                service.registerContainer(dockerFile, shortName, description);
            } catch (Exception ex) {
                handleException(ex);
            }
        }, this::registrationCallback);
        return prepareResult("Docker image registration started", ResponseStatus.SUCCEEDED);
    }

    private void registrationCallback(Exception ex) {
        final Message message = new Message();
        final String topic;
        if (ex != null) {
            message.setData("Docker image registration failed. Reason: " + ex.getMessage());
            topic = Topics.ERROR;
        } else {
            message.setData("Docker image registration completed");
            topic = Topics.INFORMATION;
        }
        Messaging.send(SessionStore.currentContext().getPrincipal(), topic, message);
    }
}
