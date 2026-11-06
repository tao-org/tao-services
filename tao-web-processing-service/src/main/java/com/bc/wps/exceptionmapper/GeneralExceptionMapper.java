package com.bc.wps.exceptionmapper;

import com.bc.wps.responses.ExceptionResponse;
import com.bc.wps.utilities.JaxbHelper;
import com.bc.wps.utilities.WpsLogger;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class GeneralExceptionMapper implements ExceptionMapper<Exception> {
   private static final Logger LOG = WpsLogger.getLogger();

   public Response toResponse(Exception exception) {
      LOG.log(Level.SEVERE, "An Exception has been caught.", exception);
      ExceptionResponse exceptionResponse = new ExceptionResponse();

      String exceptionString;
      try {
         exceptionString = JaxbHelper.marshal(exceptionResponse.getExceptionResponse(exception));
      } catch (JAXBException var6) {
         LOG.log(Level.SEVERE, "Unable to marshall the WPS response", exception);
         ExceptionResponse jaxbExceptionResponse = new ExceptionResponse();
         exceptionString = jaxbExceptionResponse.getJaxbExceptionResponse();
      }

      return Response.serverError().entity(exceptionString).build();
   }
}
