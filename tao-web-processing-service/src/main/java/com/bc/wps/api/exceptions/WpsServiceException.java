package com.bc.wps.api.exceptions;

public class WpsServiceException extends Exception {
   private String locator;

   public WpsServiceException(String message, String locator, Throwable cause) {
      this(message, cause);
      this.locator = locator;
   }

   public WpsServiceException(String message, Throwable cause) {
      super(message, cause);
   }

   public WpsServiceException(Throwable cause, String locator) {
      this(cause);
      this.locator = locator;
   }

   public WpsServiceException(Throwable cause) {
      super(cause);
   }

   public WpsServiceException(String message, String locator) {
      this(message);
      this.locator = locator;
   }

   public WpsServiceException(String message) {
      super(message);
   }

   public String getLocator() {
      return this.locator;
   }
}
