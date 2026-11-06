package com.bc.wps.api.exceptions;

public class MissingParameterValueException extends WpsServiceException {
   private final String missingParameter;

   public MissingParameterValueException(String message, Throwable cause, String missingParameter) {
      super("The value of parameter '" + missingParameter + "' is missing.");
      this.missingParameter = missingParameter;
   }

   public MissingParameterValueException(Throwable cause, String missingParameter) {
      super("The value of parameter '" + missingParameter + "' is missing.");
      this.missingParameter = missingParameter;
   }

   public MissingParameterValueException(String missingParameter) {
      super("The value of parameter '" + missingParameter + "' is missing.");
      this.missingParameter = missingParameter;
   }

   public String getMissingParameter() {
      return this.missingParameter;
   }
}
