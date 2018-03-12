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
package ro.cs.tao.services.monitoring.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.messaging.Message;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.ServiceError;
import ro.cs.tao.services.interfaces.MonitoringService;
import ro.cs.tao.services.model.monitoring.Snapshot;

import java.util.List;

/**
 * @author Cosmin Cara
 */
@Controller
@RequestMapping("/monitor")
public class MonitoringController extends BaseController {

    @Autowired
    private MonitoringService monitoringService;

    @RequestMapping(value = "/master", method = RequestMethod.GET)
    public ResponseEntity<?> getMasterSnapshot() {
        final Snapshot snapshot = monitoringService.getMasterSnapshot();
        if (snapshot == null) {
            return new ResponseEntity<>(new ServiceError("No information available for master node"),
                                        HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(snapshot, HttpStatus.OK);
    }

    @RequestMapping(value = "/{host:.+}", method = RequestMethod.GET)
    public ResponseEntity<?> getNodeSnapshot(@PathVariable("host") String host) {
        final Snapshot snapshot = monitoringService.getNodeSnapshot(host);
        if (snapshot == null) {
            return new ResponseEntity<>(new ServiceError("No information available for node ['" + host + "']"),
                                        HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(snapshot, HttpStatus.OK);
    }

    @RequestMapping(value = "/notification/", method = RequestMethod.GET)
    public ResponseEntity<?> getLiveNotifications() {
        return new ResponseEntity<>(monitoringService.getLiveNotifications(), HttpStatus.OK);
    }

    @RequestMapping(value = "/notification/{page}", method = RequestMethod.GET)
    public ResponseEntity<?> getNotifications(@RequestHeader(value = "user") String user,
                                              @PathVariable("page") int page) {
        return new ResponseEntity<>(monitoringService.getNotifications(user, page), HttpStatus.OK);
    }

    @RequestMapping(value = "/notification/ack", method = RequestMethod.POST)
    public ResponseEntity<?> acknowledgeNotifications(@RequestBody List<Message> notifications) {
        return new ResponseEntity<>(monitoringService.acknowledgeNotification(notifications), HttpStatus.OK);
    }

}
