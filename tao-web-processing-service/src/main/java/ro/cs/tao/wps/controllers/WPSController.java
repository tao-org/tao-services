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

import com.bc.wps.WpsRequestContextImpl;
import com.bc.wps.api.WpsServiceInstance;
import com.bc.wps.api.exceptions.MissingParameterValueException;
import com.bc.wps.api.exceptions.NoApplicableCodeException;
import com.bc.wps.api.exceptions.WpsServiceException;
import com.bc.wps.api.schema.*;
import com.bc.wps.exceptions.InvalidRequestException;
import com.bc.wps.responses.ExceptionResponse;
import com.bc.wps.utilities.JaxbHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.component.TargetDescriptor;
import ro.cs.tao.component.WPSComponent;
import ro.cs.tao.component.WPSComponentBean;
import ro.cs.tao.component.WebServiceAuthentication;
import ro.cs.tao.component.enums.ProcessingComponentVisibility;
import ro.cs.tao.datasource.beans.Parameter;
import ro.cs.tao.docker.Container;
import ro.cs.tao.docker.ContainerType;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.entity.beans.WebServiceBean;
import ro.cs.tao.services.entity.util.ServiceTransformUtils;
import ro.cs.tao.services.interfaces.*;
import ro.cs.tao.services.model.workflow.WorkflowInfo;
import ro.cs.tao.utils.StringUtilities;
import ro.cs.tao.wps.impl.WPSClient;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/wps")
public class WPSController extends BaseController {

    private final static Set<String> allowedRequests = new HashSet<String>() {{
        add("GetCapabilities"); add("DescribeProcess");
    }};

    private final Logger logger = Logger.getLogger(getClass().getName());
    @Autowired
    private WpsServiceInstance wpsServiceInstance;
    @Autowired
    private ContainerService containerService;
    @Autowired
    private WebServiceAuthenticationService webServiceAuthenticationService;
    @Autowired
    private WPSComponentService wpsComponentService;
    @Autowired
    private WorkflowService workflowService;

    @RequestMapping(value = {"/list","/list/{type}"}, method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> list(@PathVariable(required = false) String type) {
        try {
            ContainerType containerType = ContainerType.WPS;
            if (type != null && type.equalsIgnoreCase("STAC")) {
                containerType = ContainerType.STAC;
            }

            final List<Container> containers = containerService.listByType(containerType);
            final List<WebServiceBean> results = new ArrayList<>();
            for (Container container : containers) {
                results.add(ServiceTransformUtils.toBean(container,
                                                         webServiceAuthenticationService.findById(container.getId())));
            }
            return prepareResult(results);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/{id:.+}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> getById(@PathVariable("id") String id) {
        try {
            final Container container = containerService.findById(id);
            final WebServiceAuthentication auth = webServiceAuthenticationService.findById(id);
            return prepareResult(ServiceTransformUtils.toBean(container, auth));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<?> save(@RequestBody WebServiceBean bean) {
        try {
            if (bean == null) {
                throw new IllegalArgumentException("Empty body");
            }
            if (bean.getType() != ContainerType.WPS) {
                throw new IllegalArgumentException("Wrong container type");
            }
            if (StringUtilities.isNullOrEmpty(bean.getId())) {
                bean.setId(UUID.randomUUID().toString());
            }
            Container container = ServiceTransformUtils.getContainerPart(bean);
            if (StringUtilities.isNullOrEmpty(container.getId())) {
                container.setId(UUID.randomUUID().toString());
            }
            WebServiceAuthentication auth = ServiceTransformUtils.getAuthenticationPart(bean);
            container = containerService.save(container);
            auth.setId(container.getId());
            auth = webServiceAuthenticationService.save(auth);
            return prepareResult(ServiceTransformUtils.toBean(container, auth));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/", method = RequestMethod.PUT, produces = "application/json")
    public ResponseEntity<?> update(@RequestBody WebServiceBean bean) {
        try {
            if (bean == null) {
                throw new IllegalArgumentException("Empty body");
            }
            if (bean.getType() != ContainerType.WPS) {
                throw new IllegalArgumentException("Wrong container type");
            }
            if (StringUtilities.isNullOrEmpty(bean.getId())) {
                throw new IllegalArgumentException("Wrong HTTP verb");
            }
            Container container = ServiceTransformUtils.getContainerPart(bean);
            if (container.getTag() == null) {
                container.setTag("WPS");
            }
            WebServiceAuthentication auth = ServiceTransformUtils.getAuthenticationPart(bean);
            container = containerService.update(container);
            auth = webServiceAuthenticationService.update(auth);
            return prepareResult(ServiceTransformUtils.toBean(container, auth));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = "/{id:.+}", method = RequestMethod.DELETE, produces = "application/json")
    public ResponseEntity<?> delete(@PathVariable("id") String id) {
        ResponseEntity<ServiceResponse<?>> responseEntity;
        try {
            webServiceAuthenticationService.delete(id);
            containerService.delete(id);
            responseEntity = prepareResult("Entity deleted", ResponseStatus.SUCCEEDED);
        } catch (PersistenceException e) {
            responseEntity = handleException(e);
        }
        return responseEntity;
    }

    @RequestMapping(value = "/inspect", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<?> inspectRemote(@RequestParam(name = "request") String requestType,
                                           @RequestParam(name = "remoteAddress") String endpoint,
                                           @RequestParam(name = "capability", required = false) String capability,
                                           @RequestParam(name = "authentication", required = false) String authentication,
                                           @RequestParam(name = "save", required = false) Boolean save) {
        try {
            try {
                final URL url = new URL(endpoint);
            } catch (MalformedURLException mex) {
                throw new InvalidRequestException("[remoteAddress] Malformed URL", mex);
            }
            if (!allowedRequests.contains(requestType)) {
                throw new IllegalArgumentException("[request] Unsupported value");
            }
            final WebServiceAuthentication auth = new ObjectMapper().readerFor(WebServiceAuthentication.class).readValue(authentication);
            final WPSClient client = new WPSClient(endpoint, auth, currentPrincipal());
            if ("GetCapabilities".equalsIgnoreCase(requestType)) {
                Container capabilities = client.getCapabilities();
                if (Boolean.TRUE.equals(save)) {
                    capabilities = containerService.save(capabilities);
                }
                return prepareResult(capabilities);
            } else {
                if (StringUtils.isEmpty(capability)) {
                    throw new IllegalArgumentException("[capability] Must supply a value");
                }
                Container container = containerService.listByType(ContainerType.WPS).stream()
                                                      .filter(c -> endpoint.equals(c.getApplicationPath()))
                                                      .findFirst().orElse(null);
                if (container == null) {
                    throw new IllegalArgumentException("WPS service not previously registered");
                }
                final WebProcessingService.ProcessInfo<WorkflowInfo, TargetDescriptor> processInfo = client.describeProcess(capability);
                WPSComponent component = new WPSComponent();
                component.setId(endpoint + "~" + capability);
                component.setService(container);
                component.setCapabilityName(capability);
                component.setLabel(processInfo.getCapabilityInfo().getName());
                component.setDescription(processInfo.getCapabilityInfo().getPath());
                component.setRemoteAddress(endpoint);
                component.setVisibility(ProcessingComponentVisibility.USER);
                //component.setOwner(processInfo.getCapabilityInfo().getUserName());
                component.setOwner(currentUser());
                component.setVersion("WPS 1.0");
                final Map<String, List<Parameter>> parameters = processInfo.getParameters();
                if (parameters.size() > 0) {
                    Iterator<Map.Entry<String, List<Parameter>>> iterator = parameters.entrySet().iterator();
                    List<Parameter> params = new ArrayList<>();
                    while (iterator.hasNext()) {
                        Map.Entry<String, List<Parameter>> entry = iterator.next();
                        List<Parameter> groupParams = entry.getValue();
                        groupParams.forEach(p -> p.setName(entry.getKey() + "~" + p.getName()));
                        params.addAll(groupParams);
                    }
                    component.setParameters(params.stream().map(Parameter::toParameterDescriptor).collect(Collectors.toList()));
                }
                component.setTargets(processInfo.getOutputs());
                if (Boolean.TRUE.equals(save)) {
                    component = wpsComponentService.save(component);
                }
                return prepareResult(new WPSComponentBean(component));
            }
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @RequestMapping(params = {"service=WPS", "request=GetCapabilities"}, method = RequestMethod.GET)
    public ResponseEntity<?> capabilities(HttpServletRequest request, HttpServletResponse response) {
        try {
            final Object wpsObject = wpsServiceInstance.getCapabilities(new WpsRequestContextImpl(request));
            final String schemaLocation = "http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsGetCapabilities_response.xsd";
            final String resultAsString = marshalWithSchemaLocation(wpsObject, schemaLocation);
            writeToResponce(response, resultAsString);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return handleWpsException(e, response);
        }
    }

    @RequestMapping(params = {"service=WPS", "request=DescribeProcess", "version=1.0.0"}, method = RequestMethod.GET)
    public ResponseEntity<?> describeProcess(@RequestParam(name = "identifier") final String processId,
                                             HttpServletRequest request, HttpServletResponse response) {
        try {
            if (StringUtils.isBlank(processId)) {
                throw new MissingParameterValueException("identifier");
            }
            final List<ProcessDescriptionType> processDescriptionTypes = wpsServiceInstance.describeProcess(new WpsRequestContextImpl(request), processId);
            final Object wpsObject = createProcessDescription(processDescriptionTypes);
            final String schemaLocation = "http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsDescribeProcess_response.xsd";
            final String resultAsString = marshalWithSchemaLocation(wpsObject, schemaLocation);
            writeToResponce(response, resultAsString);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return handleWpsException(e, response);
        }
    }

    @RequestMapping(params = {"service=WPS", "request=GetStatus"}, method = RequestMethod.GET)
    public ResponseEntity<?> status(@RequestParam(name = "jobId", required = false) final String jobId,
                                    HttpServletRequest request, HttpServletResponse response) {
        try {
            if (StringUtils.isBlank(jobId)) {
                throw new MissingParameterValueException("jobId");
            }
            final Object wpsObject = wpsServiceInstance.getStatus(new WpsRequestContextImpl(request), jobId);
            final String schemaLocation = "http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsExecute_response.xsd";
            final String resultAsString = marshalWithSchemaLocation(wpsObject, schemaLocation);
            writeToResponce(response, resultAsString);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return handleWpsException(e, response);
        }
    }

    // handles all undefined get requests
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> requestGet(HttpServletRequest httpRequest, HttpServletResponse response) {
        final String line = getRequestLine(httpRequest);
        final String message = "No such Service: " + line;
        WpsServiceException e = new NoApplicableCodeException(message, null);
        return handleWpsException(e, response);
    }

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<?> requestPost(HttpServletRequest httpRequest, HttpServletResponse response) {
        try {
            final String body = getRequestBody(httpRequest).trim();
            if (!isExecuteRequest(body)) {
                throw new NoApplicableCodeException("Unknown request type: \n" + body, null);
            }
            final Execute execute = unmarshalExecute(body);
            final ExecuteResponse executeResponse = wpsServiceInstance.doExecute(new WpsRequestContextImpl(httpRequest), execute);
            final String schemaLocation = "http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsExecute_response.xsd";
            final String resultAsString = marshalWithSchemaLocation(executeResponse, schemaLocation);
            writeToResponce(response, resultAsString);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return handleWpsException(e, response);
        }
    }

    private Execute unmarshalExecute(String request) {
        InputStream requestInputStream = new ByteArrayInputStream(request.getBytes());
        try {
            return (Execute) JaxbHelper.unmarshal(requestInputStream, new Execute());
        } catch (ClassCastException exception) {
            throw new InvalidRequestException("Invalid Execute request. Please see the WPS 1.0.0 guideline " +
                                              "for the right Execute request structure.",
                                              exception);
        } catch (JAXBException exception) {
            throw new InvalidRequestException(
                    "Invalid Execute request. "
                    + (exception.getMessage() != null ? exception.getMessage() : exception.getCause().getMessage()),
                    exception);
        }
    }

    private ProcessDescriptions createProcessDescription(List<ProcessDescriptionType> processDescriptionTypes) throws WpsServiceException {
        ProcessDescriptions processDescriptions = new ProcessDescriptions();
        processDescriptions.setService("WPS");
        processDescriptions.setVersion("1.0.0");
        processDescriptions.setLang("en");

        for (ProcessDescriptionType process : processDescriptionTypes) {
            processDescriptions.getProcessDescription().add(process);
        }
        return processDescriptions;
    }

    private String getRequestBody(HttpServletRequest request) throws IOException {
        try (final BufferedReader reader = request.getReader()) {
            final StringWriter stringWriter = new StringWriter();
            final PrintWriter pw = new PrintWriter(stringWriter);
            String line;
            while ((line = reader.readLine()) != null) {
                pw.println(line);
            }
            pw.flush();
            return stringWriter.toString();
        }
    }

    private String getRequestLine(HttpServletRequest httpRequest) {
        StringBuffer request = httpRequest.getRequestURL();
        final Map<String, String[]> parameterMap = httpRequest.getParameterMap();
        int numKey = -1;
        for (Map.Entry<String, String[]> stringEntry : parameterMap.entrySet()) {
            numKey++;
            request.append(numKey == 0 ? "?" : "&")
                    .append(stringEntry.getKey())
                    .append("=");
            int numVal = -1;
            final String[] values = stringEntry.getValue();
            for (String value : values) {
                numVal++;
                request.append(numVal > 0 ? "," : "")
                        .append(value);
            }
        }
        return request.toString();
    }

    private boolean isExecuteRequest(String body) {
        return body != null && body.endsWith("Execute>");
    }

    private ResponseEntity<?> handleWpsException(Exception e, HttpServletResponse response) {
        if (e instanceof JAXBException) {
            return handleJaxbException((JAXBException) e, response);
        }
        logger.log(Level.SEVERE, "Unable to process the WPS request", e);
        ExceptionResponse exceptionResponse = new ExceptionResponse();
        ExceptionReport exceptionReport = exceptionResponse.getExceptionResponse(e);
        final String exceptionString = getExceptionString(exceptionReport);
        writeToResponce(response, exceptionString);
        return ResponseEntity.ok().build();
    }

    private ResponseEntity<?> handleJaxbException(JAXBException e, HttpServletResponse response) {
        logger.log(Level.SEVERE, "Unable to marshall the WPS response", e);
        ExceptionResponse exceptionResponse = new ExceptionResponse();
        final String jaxbExceptionResponse = exceptionResponse.getJaxbExceptionResponse();
        writeToResponce(response, jaxbExceptionResponse);
        return ResponseEntity.ok().build();
    }

    private void writeToResponce(HttpServletResponse response, String body) {
        final PrintWriter writer;
        response.setContentType(MediaType.APPLICATION_XML_VALUE);
        response.setDateHeader("Date", new Date().getTime());
        response.setHeader(HttpHeaders.TRANSFER_ENCODING, "chunked");
        try {
            writer = response.getWriter();
            writer.print(body);
        } catch (IOException ignore) {
        }
    }

    private String getExceptionString(ExceptionReport exceptionReport) {
        String retVal;
        try {
            retVal = JaxbHelper.marshalWithSchemaLocation(exceptionReport, "http://www.opengis.net/ows/1.1 " +
                                                                           "http://schemas.opengis.net/ows/1.1.0/owsExceptionReport.xsd");
        } catch (JAXBException exception) {
            logger.log(Level.SEVERE, "Unable to marshal the WPS exception.", exception);
            ExceptionResponse exceptionResponse = new ExceptionResponse();
            retVal = exceptionResponse.getJaxbExceptionResponse();
        }
        return removeBcNamespace(retVal);
    }

    private String marshalWithSchemaLocation(Object object, String schemaLocation) throws JAXBException {
        final String xml = JaxbHelper.marshalWithSchemaLocation(object, schemaLocation);
        return removeBcNamespace(xml);
    }

    private String removeBcNamespace(String xml) {
        return xml.replace("xmlns:bc=\"http://www.brockmann-consult.de/bc-wps/calwpsL3Parameters-schema.xsd\"", "");
    }
}
