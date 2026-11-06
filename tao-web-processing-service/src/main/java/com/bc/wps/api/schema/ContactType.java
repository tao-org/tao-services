package com.bc.wps.api.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "ContactType",
   propOrder = {"phone", "address", "onlineResource", "hoursOfService", "contactInstructions"}
)
public class ContactType {
   @XmlElement(
      name = "Phone",
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected TelephoneType phone;
   @XmlElement(
      name = "Address",
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected AddressType address;
   @XmlElement(
      name = "OnlineResource",
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected OnlineResourceType onlineResource;
   @XmlElement(
      name = "HoursOfService",
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected String hoursOfService;
   @XmlElement(
      name = "ContactInstructions",
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected String contactInstructions;

   public TelephoneType getPhone() {
      return this.phone;
   }

   public void setPhone(TelephoneType value) {
      this.phone = value;
   }

   public AddressType getAddress() {
      return this.address;
   }

   public void setAddress(AddressType value) {
      this.address = value;
   }

   public OnlineResourceType getOnlineResource() {
      return this.onlineResource;
   }

   public void setOnlineResource(OnlineResourceType value) {
      this.onlineResource = value;
   }

   public String getHoursOfService() {
      return this.hoursOfService;
   }

   public void setHoursOfService(String value) {
      this.hoursOfService = value;
   }

   public String getContactInstructions() {
      return this.contactInstructions;
   }

   public void setContactInstructions(String value) {
      this.contactInstructions = value;
   }
}
