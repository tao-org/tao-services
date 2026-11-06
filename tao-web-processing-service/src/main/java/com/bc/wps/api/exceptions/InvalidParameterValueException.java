package com.bc.wps.api.exceptions;

public class InvalidParameterValueException extends WpsServiceException {
   public InvalidParameterValueException(String message, Throwable cause, String invalidParameter) {
      super(message + " : parameter '" + invalidParameter + "' has an invalid value.", invalidParameter, cause);
   }

   public InvalidParameterValueException(Throwable cause, String invalidParameter) {
      super("Parameter '" + invalidParameter + "' has an invalid value.", invalidParameter, cause);
   }

   public InvalidParameterValueException(String invalidParameter) {
      super("Parameter '" + invalidParameter + "' has an invalid value.", invalidParameter);
   }

   public String getInvalidParameter() {
      return super.getLocator();
   }
}
