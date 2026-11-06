package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "WPSCapabilitiesType",
   propOrder = {"processOfferings", "languages", "wsdl"}
)
public class WPSCapabilitiesType extends CapabilitiesBaseType {
   @XmlElement(
      name = "ProcessOfferings",
      required = true
   )
   protected ProcessOfferings processOfferings;
   @XmlElement(
      name = "Languages",
      required = true
   )
   protected Languages languages;
   @XmlElement(
      name = "WSDL"
   )
   protected WSDL wsdl;
   @XmlAttribute(
      name = "service",
      required = true
   )
   @XmlSchemaType(
      name = "anySimpleType"
   )
   protected String service;
   @XmlAttribute(
      name = "lang",
      namespace = "http://www.w3.org/XML/1998/namespace",
      required = true
   )
   protected String lang;

   public ProcessOfferings getProcessOfferings() {
      return this.processOfferings;
   }

   public void setProcessOfferings(ProcessOfferings value) {
      this.processOfferings = value;
   }

   public Languages getLanguages() {
      return this.languages;
   }

   public void setLanguages(Languages value) {
      this.languages = value;
   }

   public WSDL getWSDL() {
      return this.wsdl;
   }

   public void setWSDL(WSDL value) {
      this.wsdl = value;
   }

   public String getService() {
      return this.service == null ? "WPS" : this.service;
   }

   public void setService(String value) {
      this.service = value;
   }

   public String getLang() {
      return this.lang;
   }

   public void setLang(String value) {
      this.lang = value;
   }
}
