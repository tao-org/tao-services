package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "",
   propOrder = {"acceptVersions"}
)
@XmlRootElement(
   name = "GetCapabilities"
)
public class GetCapabilities {
   @XmlElement(
      name = "AcceptVersions"
   )
   protected AcceptVersionsType acceptVersions;
   @XmlAttribute(
      name = "service",
      required = true
   )
   protected String service;
   @XmlAttribute(
      name = "language"
   )
   protected String language;

   public AcceptVersionsType getAcceptVersions() {
      return this.acceptVersions;
   }

   public void setAcceptVersions(AcceptVersionsType value) {
      this.acceptVersions = value;
   }

   public String getService() {
      return this.service == null ? "WPS" : this.service;
   }

   public void setService(String value) {
      this.service = value;
   }

   public String getLanguage() {
      return this.language;
   }

   public void setLanguage(String value) {
      this.language = value;
   }
}
