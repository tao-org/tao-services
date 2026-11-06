package com.bc.wps.api.exceptions;

public class NoApplicableCodeException extends WpsServiceException {
   public NoApplicableCodeException(String message, Throwable cause) {
      super(message, cause);
   }

   public NoApplicableCodeException(Throwable cause) {
      super(cause);
   }
}
