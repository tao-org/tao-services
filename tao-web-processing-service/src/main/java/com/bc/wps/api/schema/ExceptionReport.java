package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "",
   propOrder = {"exception"}
)
@XmlRootElement(
   name = "ExceptionReport",
   namespace = "http://www.opengis.net/ows/1.1"
)
public class ExceptionReport {
   @XmlElement(
      name = "Exception",
      namespace = "http://www.opengis.net/ows/1.1",
      required = true
   )
   protected List<ExceptionType> exception;
   @XmlAttribute(
      name = "version",
      required = true
   )
   protected String version;
   @XmlAttribute(
      name = "lang",
      namespace = "http://www.w3.org/XML/1998/namespace"
   )
   protected String lang;

   public List<ExceptionType> getException() {
      if (this.exception == null) {
         this.exception = new ArrayList();
      }

      return this.exception;
   }

   public String getVersion() {
      return this.version;
   }

   public void setVersion(String value) {
      this.version = value;
   }

   public String getLang() {
      return this.lang;
   }

   public void setLang(String value) {
      this.lang = value;
   }
}
