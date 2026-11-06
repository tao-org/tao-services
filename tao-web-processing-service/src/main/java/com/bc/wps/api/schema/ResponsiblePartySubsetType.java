package com.bc.wps.api.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "ResponsiblePartySubsetType",
   propOrder = {"individualName", "positionName", "contactInfo", "role"}
)
public class ResponsiblePartySubsetType {
   @XmlElement(
      name = "IndividualName",
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected String individualName;
   @XmlElement(
      name = "PositionName",
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected String positionName;
   @XmlElement(
      name = "ContactInfo",
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected ContactType contactInfo;
   @XmlElement(
      name = "Role",
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected CodeType role;

   public String getIndividualName() {
      return this.individualName;
   }

   public void setIndividualName(String value) {
      this.individualName = value;
   }

   public String getPositionName() {
      return this.positionName;
   }

   public void setPositionName(String value) {
      this.positionName = value;
   }

   public ContactType getContactInfo() {
      return this.contactInfo;
   }

   public void setContactInfo(ContactType value) {
      this.contactInfo = value;
   }

   public CodeType getRole() {
      return this.role;
   }

   public void setRole(CodeType value) {
      this.role = value;
   }
}
