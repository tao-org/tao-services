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

import com.bc.wps.WpsFrontendConnector;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.wps.beans.ExecutionRequest;
import ro.cs.tao.wps.impl.WebProcessingServiceImpl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Controller
@RequestMapping("/wps")
public class WPSController extends BaseController {

//    private WebProcessingServiceImpl wps = new WebProcessingServiceImpl();
    private WpsFrontendConnector wpsFrontendConnector = new WpsFrontendConnector();
    private Logger logger = Logger.getLogger(this.getClass().getName());

    //    @GetMapping(headers = {"Accept:text/html,application/xhtml+xml,application/xml"})
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> requestGet(@RequestParam("Service") final String service,
                                        @RequestParam("Request") final String requestType,
                                        @RequestParam(value = "AcceptVersions",required = false) final String acceptedVersion,
                                        @RequestParam(value = "Language", required = false) final String language,
                                        @RequestParam(value = "Identifier", required = false) final String processId,
                                        @RequestParam(value = "Version", required = false) final String version,
                                        @RequestParam(value = "JobId", required = false) final String jobId,
                                        HttpServletRequest httpRequest, HttpServletResponse response) {
        if (!isWPS(service)) {
            return serviceUnavailable(service, "No such Service: ");
        }
        final String applicationName = "TAO";
        if ("GetCapabilities".equals(requestType) || "DescribeProcess".equals(requestType)) {
            try {
                // @todo discuss with Norman ... these parameters are not needed if httpRequest is a parameter
                final String wpsService = wpsFrontendConnector.getWpsService(applicationName, service, requestType, acceptedVersion, language, processId, version, jobId, httpRequest);
                writeToResponce(response, wpsService);
                return ResponseEntity.accepted().build();
            } catch (IOException e) {
                final String msg = "IO Exception while writing response.";
                logger.log(Level.SEVERE, msg, e);
                return new ResponseEntity<>(msg, HttpStatus.SERVICE_UNAVAILABLE);
            }
        } else {
            return serviceUnavailable(requestType, "Unknown request type: ");
        }
    }

    private void writeToResponce(HttpServletResponse response, String wpsService) throws IOException {
            final PrintWriter writer = response.getWriter();
            writer.print(wpsService);
            response.setContentType(MediaType.APPLICATION_XML_VALUE);
            response.setDateHeader("Date", new Date().getTime());
            response.setHeader(HttpHeaders.TRANSFER_ENCODING, "chunked");
    }

//    @RequestMapping(value = "/getCapabilities", method = RequestMethod.GET)
//    public ResponseEntity<?> getCapabilities() {
//        return new ResponseEntity<>(webProcessingService.getCapabilities(), HttpStatus.OK);
//    }

    //    @RequestMapping(value = "/describeProcess", method = RequestMethod.GET)
    public ResponseEntity<?> describeProcess(@RequestParam("id") long id) {
//        return new ResponseEntity<>(wps.describeProcess(id), HttpStatus.OK);
        return null;
    }

    @PostMapping
    public ResponseEntity<?> requestPost(@RequestBody ExecutionRequest executionRequest) {
        long workflowId = executionRequest.getWorkflowId();
        Map<String, Map<String, String>> parameters = executionRequest.getParameters();
        long result = 289137689175L;
//        long result = webProcessingService.execute(workflowId, parameters);
        return new ResponseEntity<>(String.format("Started job with id %s", result), HttpStatus.OK);
    }

    private ResponseEntity<String> serviceUnavailable(String service, String prefix) {
        return new ResponseEntity<>(prefix + "\"" + service + "\"", HttpStatus.SERVICE_UNAVAILABLE);
    }

    private boolean isWPS(@RequestParam("Service") String service) {
        return "WPS".equals(service);
    }
}