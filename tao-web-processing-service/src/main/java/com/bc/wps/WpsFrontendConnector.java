package com.bc.wps;

import com.bc.wps.api.WpsServiceInstance;
import com.bc.wps.api.exceptions.*;
import com.bc.wps.api.schema.*;
import com.bc.wps.exceptions.InvalidRequestException;
import com.bc.wps.responses.ExceptionResponse;
import com.bc.wps.utilities.JaxbHelper;
import com.bc.wps.utilities.UrlUtils;
import com.bc.wps.utilities.WpsLogger;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WpsFrontendConnector {
   private static final Logger LOG = WpsLogger.getLogger();
   private static final String REQUEST_ID = "requestId";
   private static final String UTF_8 = "UTF-8";
   private static final String TEMP_DIRECTORY = "tmp";
   private static final String REQUEST_FILE_PREFIX = "request-";
   private final boolean removeBcNamespace;

   public WpsFrontendConnector() {
      this(false);
   }

   public WpsFrontendConnector(boolean removeBcNamespace) {
      this.removeBcNamespace = removeBcNamespace;
   }

   public String getWpsService(String service, String requestType, String acceptedVersion, String language, String processIdentifier, String version, String jobId, HttpServletRequest servletRequest, WpsServiceInstance wpsServiceProvider) {
      Cookie[] cookies = servletRequest.getCookies();
      if (cookies != null) {
         for(Cookie cookie : cookies) {
            if ("requestId".equalsIgnoreCase(cookie.getName())) {
               return this.postExecuteService(requestType, servletRequest, wpsServiceProvider);
            }

            if ("queryString".equalsIgnoreCase(cookie.getName())) {
               String value = cookie.getValue();

               try {
                  if (service == null) {
                     String parsed = UrlUtils.parseParameter(value, "Service");
                     if (StringUtils.isNotBlank(parsed)) {
                        service = parsed;
                        LOG.info("Setting service to " + parsed);
                     }
                  }

                  if (requestType == null) {
                     String parsed = UrlUtils.parseParameter(value, "Request");
                     if (StringUtils.isNotBlank(parsed)) {
                        requestType = parsed;
                        LOG.info("Setting requestType to " + parsed);
                     }
                  }

                  if (version == null) {
                     String parsed = UrlUtils.parseParameter(value, "Version");
                     if (StringUtils.isNotBlank(parsed)) {
                        version = parsed;
                        LOG.info("Setting version to " + parsed);
                     }
                  }

                  if (processIdentifier == null) {
                     String parsed = UrlUtils.parseParameter(value, "Identifier");
                     if (StringUtils.isNotBlank(parsed)) {
                        processIdentifier = parsed;
                        LOG.info("Setting processId to " + parsed);
                     }
                  }

                  if (jobId == null) {
                     String parsed = UrlUtils.parseParameter(value, "JobId");
                     if (StringUtils.isNotBlank(parsed)) {
                        jobId = parsed;
                        LOG.info("Setting jobId to " + parsed);
                     }
                  }
               } catch (UnsupportedEncodingException exception) {
                  LOG.log(Level.SEVERE, "Unable to process the WPS request", exception);
                  ExceptionResponse exceptionResponse = new ExceptionResponse();
                  ExceptionReport exceptionReport = exceptionResponse.getExceptionResponse(exception);
                  return this.getExceptionString(exceptionReport);
               }
            }
         }
      }

      try {
         this.performUrlParameterValidation(service, requestType);
         WpsRequestContextImpl requestContext = new WpsRequestContextImpl(servletRequest);
         switch (requestType) {
            case "GetCapabilities":
               Capabilities capabilities = wpsServiceProvider.getCapabilities(requestContext);
               return this.removeBcNamespace(JaxbHelper.marshalWithSchemaLocation(capabilities, "http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsGetCapabilities_response.xsd"));
            case "DescribeProcess":
               String describeProcessExceptionXml = this.performDescribeProcessParameterValidation(processIdentifier, version);
               if (StringUtils.isNotBlank(describeProcessExceptionXml)) {
                  return describeProcessExceptionXml;
               }

               List<ProcessDescriptionType> processDescriptionTypes = wpsServiceProvider.describeProcess(requestContext, processIdentifier);
               ProcessDescriptions processDescriptions = this.constructProcessDescriptionXml(processDescriptionTypes);
               return this.removeBcNamespace(JaxbHelper.marshalWithSchemaLocation(processDescriptions, "http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsDescribeProcess_response.xsd"));
            case "GetStatus":
               if (StringUtils.isBlank(jobId)) {
                  throw new MissingParameterValueException("JobId");
               } else {
                  ExecuteResponse executeResponse = wpsServiceProvider.getStatus(requestContext, jobId);
                  if (executeResponse == null) {
                     throw new NoApplicableCodeException("Unknown jobId: '" + jobId + "'", (Throwable)null);
                  }

                  return this.removeBcNamespace(JaxbHelper.marshalWithSchemaLocation(executeResponse, "http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsExecute_response.xsd"));
               }
            default:
               throw new InvalidParameterValueException("Request");
         }
      } catch (WpsServiceException exception) {
         LOG.log(Level.SEVERE, "Unable to process the WPS request", exception);
         ExceptionResponse exceptionResponse = new ExceptionResponse();
         ExceptionReport exceptionReport = exceptionResponse.getExceptionResponse(exception);
         return this.getExceptionString(exceptionReport);
      } catch (JAXBException exception) {
         LOG.log(Level.SEVERE, "Unable to marshall the WPS response", exception);
         ExceptionResponse exceptionResponse = new ExceptionResponse();
         return exceptionResponse.getJaxbExceptionResponse();
      }
   }

   public String postExecuteService(String request, HttpServletRequest servletRequest, WpsServiceInstance wpsServiceProvider) {
      Cookie[] cookies = servletRequest.getCookies();
      Path tempFilePath = null;
      if (cookies != null) {
         for(Cookie cookie : cookies) {
            if ("requestId".equalsIgnoreCase(cookie.getName())) {
               String id = cookie.getValue();
               String filename = File.separator + "tmp" + File.separator + "request-" + id;
               System.out.println("Reading request from file " + filename);

               try {
                  tempFilePath = Paths.get(filename);
                  LOG.info(tempFilePath.toAbsolutePath().toString());
                  byte[] encoded = Files.readAllBytes(tempFilePath);
                  request = new String(encoded, "UTF-8");
                  request = cleanRequest(request);
               } catch (IOException e) {
                  throw new IllegalStateException(e);
               }
            }
         }
      }

      Execute execute = this.getExecute(request);

      try {
         this.performXmlParameterValidation(execute);
         WpsRequestContextImpl requestContext = new WpsRequestContextImpl(servletRequest);
         ExecuteResponse executeResponse = wpsServiceProvider.doExecute(requestContext, execute);
         this.deleteTemporaryFile(tempFilePath);
         return this.removeBcNamespace(JaxbHelper.marshalWithSchemaLocation(executeResponse, "http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsExecute_response.xsd"));
      } catch (IOException | WpsServiceException exception) {
         LOG.log(Level.SEVERE, "Unable to process the WPS request", exception);
         ExceptionResponse exceptionResponse = new ExceptionResponse();
         ExceptionReport exceptionReport = exceptionResponse.getExceptionResponse(exception);

         try {
            this.deleteTemporaryFile(tempFilePath);
         } catch (IOException var15) {
         }

         return this.getExceptionString(exceptionReport);
      } catch (JAXBException exception) {
         LOG.log(Level.SEVERE, "Unable to marshall the WPS response", exception);
         ExceptionResponse exceptionResponse = new ExceptionResponse();

         try {
            this.deleteTemporaryFile(tempFilePath);
         } catch (IOException var14) {
         }

         return exceptionResponse.getJaxbExceptionResponse();
      }
   }

   static String cleanRequest(String request) {
      request = "<?xml" + request.split("<\\?xml")[1];
      request = request.split("wps:Execute>")[0] + "wps:Execute>";
      return request;
   }

   private String removeBcNamespace(String xml) {
      return this.removeBcNamespace ? xml.replace("xmlns:bc=\"http://www.brockmann-consult.de/bc-wps/calwpsL3Parameters-schema.xsd\" ", "") : xml;
   }

   private void deleteTemporaryFile(Path tempFilePath) throws IOException {
      if (tempFilePath != null && Files.exists(tempFilePath, new LinkOption[0])) {
         LOG.log(Level.INFO, "deleting temporary file '" + tempFilePath + "'.");
         Files.delete(tempFilePath);
      }

   }

   private String performDescribeProcessParameterValidation(String processorId, String version) throws MissingParameterValueException {
      if (StringUtils.isBlank(version)) {
         throw new MissingParameterValueException("Version");
      } else if (StringUtils.isBlank(processorId)) {
         throw new MissingParameterValueException("Identifier");
      } else {
         return null;
      }
   }

   private void performUrlParameterValidation(String service, String requestType) throws MissingParameterValueException, InvalidParameterValueException {
      if (StringUtils.isBlank(service)) {
         throw new MissingParameterValueException("Service");
      } else if (StringUtils.isBlank(requestType)) {
         throw new MissingParameterValueException("Request");
      } else if (!service.equals("WPS")) {
         throw new InvalidParameterValueException("Service");
      }
   }

   private void performXmlParameterValidation(Execute execute) throws WpsServiceException {
      String service = execute.getService();
      String version = execute.getVersion();
      CodeType identifier = execute.getIdentifier();
      if (StringUtils.isBlank(service)) {
         throw new MissingParameterValueException("Service");
      } else if (!"WPS".equals(service)) {
         throw new InvalidParameterValueException("Service");
      } else if (StringUtils.isBlank(version)) {
         throw new MissingParameterValueException("Version");
      } else if (!"1.0.0".equals(version)) {
         throw new InvalidParameterValueException("Version");
      } else if (identifier == null) {
         throw new XmlSchemaFaultException("Identifier", "Execute");
      } else if (StringUtils.isBlank(identifier.getValue())) {
         throw new MissingParameterValueException("Identifier");
      }
   }

   private Execute getExecute(String request) {
      InputStream requestInputStream = new ByteArrayInputStream(request.getBytes());

      try {
         return (Execute)JaxbHelper.unmarshal(requestInputStream, new Execute());
      } catch (ClassCastException exception) {
         throw new InvalidRequestException("Invalid Execute request. Please see the WPS 1.0.0 guideline for the right Execute request structure.", exception);
      } catch (JAXBException var5) {
         throw new InvalidRequestException("Invalid Execute request. " + (var5.getMessage() != null ? var5.getMessage() : var5.getCause().getMessage()), var5);
      }
   }

   private String getExceptionString(ExceptionReport exceptionReport) {
      String retVal;
      try {
         retVal = JaxbHelper.marshalWithSchemaLocation(exceptionReport, "http://www.opengis.net/ows/1.1 http://schemas.opengis.net/ows/1.1.0/owsExceptionReport.xsd");
      } catch (JAXBException exception) {
         LOG.log(Level.SEVERE, "Unable to marshal the WPS exception.", exception);
         ExceptionResponse exceptionResponse = new ExceptionResponse();
         retVal = exceptionResponse.getJaxbExceptionResponse();
      }

      return this.removeBcNamespace(retVal);
   }

   private ProcessDescriptions constructProcessDescriptionXml(List<ProcessDescriptionType> processes) {
      ProcessDescriptions processDescriptions = this.createBasicProcessDescriptions();

      for(ProcessDescriptionType process : processes) {
         processDescriptions.getProcessDescription().add(process);
      }

      return processDescriptions;
   }

   private ProcessDescriptions createBasicProcessDescriptions() {
      ProcessDescriptions processDescriptions = new ProcessDescriptions();
      processDescriptions.setService("WPS");
      processDescriptions.setVersion("1.0.0");
      processDescriptions.setLang("en");
      return processDescriptions;
   }
}
