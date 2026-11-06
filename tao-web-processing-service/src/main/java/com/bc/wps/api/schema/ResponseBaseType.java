package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "ResponseBaseType"
)
@XmlSeeAlso({ProcessDescriptions.class, ExecuteResponse.class})
public class ResponseBaseType {
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
      name = "lang",
      namespace = "http://www.w3.org/XML/1998/namespace",
      required = true
   )
   protected String lang;

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

   public String getLang() {
      return this.lang;
   }

   public void setLang(String value) {
      this.lang = value;
   }
}
