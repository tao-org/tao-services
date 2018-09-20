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

package ro.cs.tao.wps.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.interfaces.WebProcessingService;
import ro.cs.tao.wps.beans.ExecutionRequest;

import java.util.Map;

@Controller
@RequestMapping("/wps")
public class WPSController extends BaseController {

    @Autowired
    private WebProcessingService webProcessingService;

    @RequestMapping(value = "/getCapabilities", method = RequestMethod.GET)
    public ResponseEntity<?> getCapabilities() {
        return new ResponseEntity<>(webProcessingService.getCapabilities(), HttpStatus.OK);
    }

    @RequestMapping(value = "/describeProcess", method = RequestMethod.GET)
    public ResponseEntity<?> describeProcess(@RequestParam("id") long id) {
        return new ResponseEntity<>(webProcessingService.describeProcess(id), HttpStatus.OK);
    }

    @RequestMapping(value = "/execute", method = RequestMethod.POST)
    public ResponseEntity<?> execute(@RequestBody ExecutionRequest executionRequest) {
        long workflowId = executionRequest.getWorkflowId();
        Map<String, Map<String, String>> parameters = executionRequest.getParameters();
        long result = webProcessingService.execute(workflowId, parameters);
        return new ResponseEntity<>(String.format("Started job with id %s", result), HttpStatus.OK);
    }
}
