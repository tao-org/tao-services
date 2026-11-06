package com.bc.wps.responses;

import com.bc.wps.api.exceptions.*;
import com.bc.wps.api.schema.ExceptionReport;
import com.bc.wps.api.schema.ExceptionType;
import com.bc.wps.api.schema.ProcessFailedType;
import org.apache.commons.lang.StringUtils;

public class ExceptionResponse {
   private ExceptionReport exceptionReport;

   public ExceptionResponse() {
      new ProcessFailedType();
      this.exceptionReport = new ExceptionReport();
      this.exceptionReport.setLang("en");
      this.exceptionReport.setVersion("1.0.0");
   }

   public ExceptionReport getExceptionResponse(Exception exception) {
      if (exception instanceof InvalidParameterValueException) {
         return this.getInvalidParameterExceptionResponse((InvalidParameterValueException)exception);
      } else if (exception instanceof MissingParameterValueException) {
         return this.getMissingParameterExceptionResponse((MissingParameterValueException)exception);
      } else if (exception instanceof NotEnoughStorageException) {
         return this.getNotEnoughStorageExceptionResponse((NotEnoughStorageException)exception);
      } else if (exception instanceof NoApplicableCodeException) {
         return this.getNoApplicableCodeExceptionResponse((NoApplicableCodeException)exception);
      } else if (exception instanceof OptionNotSupportedException) {
         return this.getOptionNotSupportedExceptionResponse((OptionNotSupportedException)exception);
      } else {
         return exception instanceof XmlSchemaFaultException ? this.getXmlSchemaFaultExceptionResponse((XmlSchemaFaultException)exception) : this.getGeneralExceptionReport(exception);
      }
   }

   public String getJaxbExceptionResponse() {
      return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<ExceptionReport version=\"version\" xml:lang=\"Lang\">\n    <Exception exceptionCode=\"NoApplicableCode\">\n        <ExceptionText>Unable to generate the exception XML : JAXB Exception.</ExceptionText>\n    </Exception>\n</ExceptionReport>\n";
   }

   private ExceptionReport getInvalidParameterExceptionResponse(InvalidParameterValueException invalidParameterException) {
      String message = invalidParameterException.getMessage();
      String locator = invalidParameterException.getInvalidParameter();
      return this.createReport(message, "InvalidParameterValue", locator);
   }

   private ExceptionReport getMissingParameterExceptionResponse(MissingParameterValueException missingParameterValueException) {
      String message = missingParameterValueException.getMessage();
      String locator = missingParameterValueException.getMissingParameter();
      return this.createReport(message, "MissingParameterValue", locator);
   }

   private ExceptionReport getNoApplicableCodeExceptionResponse(NoApplicableCodeException noApplicableCodeException) {
      String message = noApplicableCodeException.getMessage();
      return this.createReport(message, "NoApplicableCode", (String)null);
   }

   private ExceptionReport getNotEnoughStorageExceptionResponse(NotEnoughStorageException notEnoughStorageException) {
      String message = notEnoughStorageException.getMessage();
      return this.createReport(message, "NotEnoughStorage", (String)null);
   }

   private ExceptionReport getOptionNotSupportedExceptionResponse(OptionNotSupportedException optionNotSupportedException) {
      String message = optionNotSupportedException.getMessage();
      String identifier = optionNotSupportedException.getIdentifierOfOptionNotSupported();
      return this.createReport(message, "OptionNotSupported", identifier);
   }

   private ExceptionReport getXmlSchemaFaultExceptionResponse(XmlSchemaFaultException exception) {
      String message = exception.getMessage();
      String parentElement = exception.getParentElement();
      return this.createReport(message, "InvalidParameterValue", parentElement);
   }

   private ExceptionReport getGeneralExceptionReport(Exception exception) {
      Throwable cause = exception.getCause();
      String message = exception.getMessage();
      if (cause != null) {
         message = message + " : " + cause.getMessage();
      } else {
         message = exception.getClass() + " : " + message;
      }

      return this.createReport(message, "NoApplicableCode", (String)null);
   }

   private ExceptionReport createReport(String message, String exceptionCode, String locator) {
      ExceptionType exceptionResponse = new ExceptionType();
      exceptionResponse.getExceptionText().add(message);
      exceptionResponse.setExceptionCode(exceptionCode);
      if (StringUtils.isNotBlank(locator)) {
         exceptionResponse.setLocator(locator);
      }

      this.exceptionReport.getException().add(exceptionResponse);
      return this.exceptionReport;
   }
}
