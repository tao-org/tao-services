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
package ro.cs.tao.services.monitoring.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.execution.monitor.RuntimeInfo;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.Notification;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.MonitoringService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @author Cosmin Cara
 */
@Controller
@RequestMapping("/monitor")
public class MonitoringController extends BaseController {

    @Autowired
    private MonitoringService<Notification> monitoringService;

    @RequestMapping(value = "/", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getAllSnapshot() {
        return prepareResult(monitoringService.getNodesSnapshot(),
                             LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss")));
    }

    @RequestMapping(value = "/master", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getMasterSnapshot() {
        final RuntimeInfo snapshot = monitoringService.getMasterSnapshot();
        if (snapshot == null) {
            return prepareResult("No information available for master node", ResponseStatus.FAILED);
        }
        return prepareResult(snapshot, LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss")));
    }

    @RequestMapping(value = "/{host:.+}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getNodeSnapshot(@PathVariable("host") String host) {
        final RuntimeInfo snapshot = monitoringService.getNodeSnapshot(host);
        if (snapshot == null) {
            return prepareResult("No information available for node ['" + host + "']", ResponseStatus.FAILED);
        }
        return prepareResult(snapshot);
    }

    @RequestMapping(value = "/notification/", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getLiveNotifications() {
        return prepareResult(monitoringService.getLiveNotifications());
    }

    @RequestMapping(value = "/notification/unread", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getUnreadNotifications() {
        return prepareResult(monitoringService.getUnreadNotifications(SessionStore.currentContext().getPrincipal().getName()));
    }

    @RequestMapping(value = "/notification/{page}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getNotifications(@RequestHeader(value = "user") String user,
                                              @PathVariable("page") int page) {
        return prepareResult(monitoringService.getNotifications(user, page));
    }

    @RequestMapping(value = "/notification/ack", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> acknowledgeNotifications(@RequestBody List<Notification> notifications) {
        return prepareResult(monitoringService.acknowledgeNotification(notifications));
    }

    @RequestMapping(value = "/status", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getNodesOnlineStatus() {
        return prepareResult(monitoringService.getNodesOnlineStatus());
    }

}
