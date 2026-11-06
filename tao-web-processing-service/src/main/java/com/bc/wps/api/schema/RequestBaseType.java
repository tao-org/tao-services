package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "RequestBaseType"
)
@XmlSeeAlso({DescribeProcess.class, Execute.class})
public class RequestBaseType {
   @XmlAttribute(
      name = "service",
      required = true
   )
   protected String service;
   @XmlAttribute(
      name = "version",
      required = true
   )
   protected String version;
   @XmlAttribute(
      name = "language"
   )
   protected String language;

   public String getService() {
      return this.service == null ? "WPS" : this.service;
   }

   public void setService(String value) {
      this.service = value;
   }

   public String getVersion() {
      return this.version == null ? "1.0.0" : this.version;
   }

   public void setVersion(String value) {
      this.version = value;
   }

   public String getLanguage() {
      return this.language;
   }

   public void setLanguage(String value) {
      this.language = value;
   }
}
