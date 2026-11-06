package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "CapabilitiesBaseType",
   namespace = "http://www.opengis.net/ows/1.1",
   propOrder = {"serviceIdentification", "serviceProvider", "operationsMetadata"}
)
@XmlSeeAlso({WPSCapabilitiesType.class})
public class CapabilitiesBaseType {
   @XmlElement(
      name = "ServiceIdentification",
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected ServiceIdentification serviceIdentification;
   @XmlElement(
      name = "ServiceProvider",
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected ServiceProvider serviceProvider;
   @XmlElement(
      name = "OperationsMetadata",
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected OperationsMetadata operationsMetadata;
   @XmlAttribute(
      name = "version",
      required = true
   )
   protected String version;
   @XmlAttribute(
      name = "updateSequence"
   )
   protected String updateSequence;

   public ServiceIdentification getServiceIdentification() {
      return this.serviceIdentification;
   }

   public void setServiceIdentification(ServiceIdentification value) {
      this.serviceIdentification = value;
   }

   public ServiceProvider getServiceProvider() {
      return this.serviceProvider;
   }

   public void setServiceProvider(ServiceProvider value) {
      this.serviceProvider = value;
   }

   public OperationsMetadata getOperationsMetadata() {
      return this.operationsMetadata;
   }

   public void setOperationsMetadata(OperationsMetadata value) {
      this.operationsMetadata = value;
   }

   public String getVersion() {
      return this.version;
   }

   public void setVersion(String value) {
      this.version = value;
   }

   public String getUpdateSequence() {
      return this.updateSequence;
   }

   public void setUpdateSequence(String value) {
      this.updateSequence = value;
   }
}
