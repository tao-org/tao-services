package com.bc.wps.api.schema;

import com.bc.wps.api.utils.CapabilitiesBuilder;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "",
   propOrder = {"processOfferings", "languages", "wsdl"}
)
@XmlRootElement(
   name = "Capabilities"
)
public class Capabilities extends CapabilitiesBaseType {
   @XmlElement(
      name = "ProcessOfferings",
      namespace = "http://www.opengis.net/wps/1.0.0",
      required = true
   )
   protected ProcessOfferings processOfferings;
   @XmlElement(
      name = "Languages",
      namespace = "http://www.opengis.net/wps/1.0.0",
      required = true
   )
   protected Languages languages;
   @XmlElement(
      name = "WSDL",
      namespace = "http://www.opengis.net/wps/1.0.0"
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

   public Capabilities() {
   }

   public Capabilities(CapabilitiesBuilder builder) {
      this.operationsMetadata = builder.getOperationsMetadata();
      this.serviceIdentification = builder.getServiceIdentification();
      this.serviceProvider = builder.getServiceProvider();
      this.processOfferings = builder.getProcessOfferings();
      this.languages = builder.getLanguages();
      this.service = "WPS";
      this.version = "1.0.0";
      this.lang = "en";
   }

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
