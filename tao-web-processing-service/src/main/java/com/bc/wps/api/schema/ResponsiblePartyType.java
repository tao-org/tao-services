package com.bc.wps.api.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "ResponsiblePartyType",
   propOrder = {"individualName", "organisationName", "positionName", "contactInfo", "role"}
)
public class ResponsiblePartyType {
   @XmlElement(
      name = "IndividualName"
   )
   protected String individualName;
   @XmlElement(
      name = "OrganisationName"
   )
   protected String organisationName;
   @XmlElement(
      name = "PositionName"
   )
   protected String positionName;
   @XmlElement(
      name = "ContactInfo"
   )
   protected ContactType contactInfo;
   @XmlElement(
      name = "Role",
      required = true
   )
   protected CodeType role;

   public String getIndividualName() {
      return this.individualName;
   }

   public void setIndividualName(String value) {
      this.individualName = value;
   }

   public String getOrganisationName() {
      return this.organisationName;
   }

   public void setOrganisationName(String value) {
      this.organisationName = value;
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
