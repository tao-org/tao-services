package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "",
   propOrder = {"serviceType", "serviceTypeVersion", "profile", "fees", "accessConstraints"}
)
@XmlRootElement(
   name = "ServiceIdentification",
   namespace = "http://www.opengis.net/ows/1.1"
)
public class ServiceIdentification extends DescriptionType {
   @XmlElement(
      name = "ServiceType",
      required = true,
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected CodeType serviceType;
   @XmlElement(
      name = "ServiceTypeVersion",
      required = true,
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected List<String> serviceTypeVersion;
   @XmlElement(
      name = "Profile"
   )
   @XmlSchemaType(
      name = "anyURI"
   )
   protected List<String> profile;
   @XmlElement(
      name = "Fees",
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected String fees;
   @XmlElement(
      name = "AccessConstraints"
   )
   protected List<String> accessConstraints;

   public CodeType getServiceType() {
      return this.serviceType;
   }

   public void setServiceType(CodeType value) {
      this.serviceType = value;
   }

   public List<String> getServiceTypeVersion() {
      if (this.serviceTypeVersion == null) {
         this.serviceTypeVersion = new ArrayList();
      }

      return this.serviceTypeVersion;
   }

   public List<String> getProfile() {
      if (this.profile == null) {
         this.profile = new ArrayList();
      }

      return this.profile;
   }

   public String getFees() {
      return this.fees;
   }

   public void setFees(String value) {
      this.fees = value;
   }

   public List<String> getAccessConstraints() {
      if (this.accessConstraints == null) {
         this.accessConstraints = new ArrayList();
      }

      return this.accessConstraints;
   }
}
