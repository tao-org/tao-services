package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "ProcessBriefType",
   propOrder = {"profile", "wsdl"}
)
@XmlSeeAlso({ProcessDescriptionType.class})
public class ProcessBriefType extends DescriptionType {
   @XmlElement(
      name = "Profile"
   )
   @XmlSchemaType(
      name = "anyURI"
   )
   protected List<String> profile;
   @XmlElement(
      name = "WSDL"
   )
   protected WSDL wsdl;
   @XmlAttribute(
      name = "processVersion",
      namespace = "http://www.opengis.net/wps/1.0.0",
      required = true
   )
   protected String processVersion;

   public List<String> getProfile() {
      if (this.profile == null) {
         this.profile = new ArrayList();
      }

      return this.profile;
   }

   public WSDL getWSDL() {
      return this.wsdl;
   }

   public void setWSDL(WSDL value) {
      this.wsdl = value;
   }

   public String getProcessVersion() {
      return this.processVersion;
   }

   public void setProcessVersion(String value) {
      this.processVersion = value;
   }
}
