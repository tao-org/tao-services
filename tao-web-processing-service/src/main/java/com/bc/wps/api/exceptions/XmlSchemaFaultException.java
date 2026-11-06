package com.bc.wps.api.exceptions;

public class XmlSchemaFaultException extends WpsServiceException {
   public XmlSchemaFaultException(String missedElement, String parentElemet) {
      super(createMessage(missedElement, parentElemet), parentElemet);
   }

   public String getParentElement() {
      return this.getLocator();
   }

   private static String createMessage(String missedElement, String parentElemet) {
      return "XML schema fault: Element '" + missedElement + "' missed in element '" + parentElemet + "'.";
   }
}
