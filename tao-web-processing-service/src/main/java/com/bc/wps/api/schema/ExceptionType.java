package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "ExceptionType",
   propOrder = {"exceptionText"}
)
public class ExceptionType {
   @XmlElement(
      name = "ExceptionText",
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected List<String> exceptionText;
   @XmlAttribute(
      name = "exceptionCode",
      required = true
   )
   protected String exceptionCode;
   @XmlAttribute(
      name = "locator"
   )
   protected String locator;

   public List<String> getExceptionText() {
      if (this.exceptionText == null) {
         this.exceptionText = new ArrayList();
      }

      return this.exceptionText;
   }

   public String getExceptionCode() {
      return this.exceptionCode;
   }

   public void setExceptionCode(String value) {
      this.exceptionCode = value;
   }

   public String getLocator() {
      return this.locator;
   }

   public void setLocator(String value) {
      this.locator = value;
   }
}
