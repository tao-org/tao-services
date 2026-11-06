package com.bc.wps.api.exceptions;

public class OptionNotSupportedException extends WpsServiceException {
   public OptionNotSupportedException(String message, String identifierOfOptionNotSupported) {
      super(message, identifierOfOptionNotSupported);
   }

   public String getIdentifierOfOptionNotSupported() {
      return this.getLocator();
   }
}
