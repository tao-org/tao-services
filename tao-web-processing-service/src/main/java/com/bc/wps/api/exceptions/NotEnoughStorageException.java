package com.bc.wps.api.exceptions;

public class NotEnoughStorageException extends WpsServiceException {
   public NotEnoughStorageException(String message, Throwable cause) {
      super(message, cause);
   }

   public NotEnoughStorageException(Throwable cause) {
      super(cause);
   }
}
