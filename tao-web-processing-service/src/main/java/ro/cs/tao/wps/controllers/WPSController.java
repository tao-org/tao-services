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
import com.bc.wps.api.WpsServiceInstance;
import com.bc.wps.api.exceptions.WpsRuntimeException;
import com.bc.wps.utilities.PropertiesWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.services.commons.BaseController;

import javax.servlet.http.HttpServletRequest;
import java.io.*;

@Controller
@RequestMapping("/wps")
public class WPSController extends BaseController {

    static {
        try {
            PropertiesWrapper.loadConfigFile("tao-wps.properties");
        } catch (IOException exception) {
            throw new WpsRuntimeException("Unable to load tao-wps.properties file", exception);
        }
    }

    @Autowired
    private WpsServiceInstance wpsServiceInstance;
    private WpsFrontendConnector wpsFrontendConnector = new WpsFrontendConnector(true);

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> requestGet(@RequestParam("Service") final String service,
                                        @RequestParam("Request") final String requestType,
                                        @RequestParam(name = "AcceptVersions", required = false) final String acceptedVersion,
                                        @RequestParam(name = "Language", required = false) final String language,
                                        @RequestParam(name = "Identifier", required = false) final String processId,
                                        @RequestParam(name = "Version", required = false) final String version,
                                        @RequestParam(name = "JobId", required = false) final String jobId,
                                        HttpServletRequest httpRequest) {
        ResponseEntity<?> result;
        if (!isWPS(service)) {
            result = serviceUnavailable(service, "No such Service: ");
        } else {
            switch (requestType) {
                case "GetCapabilities":
                case "DescribeProcess":
                case "GetStatus":
                    // @todo discuss with Norman ... these parameters are not needed if httpRequest is a parameter
                    final String wpsService = wpsFrontendConnector.getWpsService(
                            service, requestType, acceptedVersion, language,
                            processId, version, jobId, httpRequest, wpsServiceInstance);
                    result = createResponseOk(wpsService);
                    break;
                default:
                    result = serviceUnavailable(requestType, "Unknown request type: ");
            }
        }
        return result;
    }

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<?> requestPost(HttpServletRequest httpRequest) {
        try {
            final String body = getXmlFrom(httpRequest).trim();
            if (!isExecuteRequest(body)) {
                return serviceUnavailable(body, "Unknown request type: \n");
            }
            final String result = wpsFrontendConnector.postExecuteService(body, httpRequest, wpsServiceInstance);
            return createResponseOk(result);
        } catch (Exception e) {
            return serviceUnavailable(e.getMessage(), "");
        }
    }

    private ResponseEntity<String> createResponseOk(String respBody) {
        respBody = removeBcNamespace(respBody);
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        return new ResponseEntity<>(respBody, headers, HttpStatus.OK);
    }

    private String getXmlFrom(HttpServletRequest executionRequest) throws IOException {
        try (final InputStream is = executionRequest.getInputStream();
             final InputStreamReader isr = new InputStreamReader(is);
             final LineNumberReader lnr = new LineNumberReader(isr)) {

            final StringWriter stringWriter = new StringWriter();
            final PrintWriter pw = new PrintWriter(stringWriter);
            String line;
            while ((line = lnr.readLine()) != null) {
                pw.println(line);
            }
            pw.flush();
            return stringWriter.toString();
        }
    }

    private ResponseEntity<String> serviceUnavailable(String service, String prefix) {
        return new ResponseEntity<>(prefix + "\"" + service + "\"", HttpStatus.SERVICE_UNAVAILABLE);
    }

    private boolean isWPS(@RequestParam("Service") String service) {
        return "WPS".equals(service);
    }

    private boolean isExecuteRequest(String body) {
        return body != null && body.endsWith("Execute>");
    }

    private String removeBcNamespace(String xml) {
        return xml.replace("xmlns:bc=\"http://www.brockmann-consult.de/bc-wps/calwpsL3Parameters-schema.xsd\"", "");
    }
}
