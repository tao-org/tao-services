package com.bc.wps.api.exceptions;

public class WpsRuntimeException extends RuntimeException {
   public WpsRuntimeException(String message, Throwable cause) {
      super(message, cause);
   }

   public WpsRuntimeException(String message) {
      super(message);
   }
}
