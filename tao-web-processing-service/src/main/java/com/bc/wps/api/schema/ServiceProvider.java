package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "",
   propOrder = {"providerName", "providerSite", "serviceContact"}
)
@XmlRootElement(
   name = "ServiceProvider"
)
public class ServiceProvider {
   @XmlElement(
      name = "ProviderName",
      namespace = "http://www.opengis.net/ows/1.1",
      required = true
   )
   protected String providerName;
   @XmlElement(
      name = "ProviderSite",
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected OnlineResourceType providerSite;
   @XmlElement(
      name = "ServiceContact",
      required = true,
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected ResponsiblePartySubsetType serviceContact;

   public String getProviderName() {
      return this.providerName;
   }

   public void setProviderName(String value) {
      this.providerName = value;
   }

   public OnlineResourceType getProviderSite() {
      return this.providerSite;
   }

   public void setProviderSite(OnlineResourceType value) {
      this.providerSite = value;
   }

   public ResponsiblePartySubsetType getServiceContact() {
      return this.serviceContact;
   }

   public void setServiceContact(ResponsiblePartySubsetType value) {
      this.serviceContact = value;
   }
}
