package com.bc.wps.api.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "AddressType",
   propOrder = {"deliveryPoint", "city", "administrativeArea", "postalCode", "country", "electronicMailAddress"}
)
public class AddressType {
   @XmlElement(
      name = "DeliveryPoint",
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected List<String> deliveryPoint;
   @XmlElement(
      name = "City",
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected String city;
   @XmlElement(
      name = "AdministrativeArea",
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected String administrativeArea;
   @XmlElement(
      name = "PostalCode",
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected String postalCode;
   @XmlElement(
      name = "Country",
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected String country;
   @XmlElement(
      name = "ElectronicMailAddress",
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected List<String> electronicMailAddress;

   public List<String> getDeliveryPoint() {
      if (this.deliveryPoint == null) {
         this.deliveryPoint = new ArrayList();
      }

      return this.deliveryPoint;
   }

   public String getCity() {
      return this.city;
   }

   public void setCity(String value) {
      this.city = value;
   }

   public String getAdministrativeArea() {
      return this.administrativeArea;
   }

   public void setAdministrativeArea(String value) {
      this.administrativeArea = value;
   }

   public String getPostalCode() {
      return this.postalCode;
   }

   public void setPostalCode(String value) {
      this.postalCode = value;
   }

   public String getCountry() {
      return this.country;
   }

   public void setCountry(String value) {
      this.country = value;
   }

   public List<String> getElectronicMailAddress() {
      if (this.electronicMailAddress == null) {
         this.electronicMailAddress = new ArrayList();
      }

      return this.electronicMailAddress;
   }
}
