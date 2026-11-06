package com.bc.wps.api.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "ResponseFormType",
   propOrder = {"responseDocument", "rawDataOutput"}
)
public class ResponseFormType {
   @XmlElement(
      name = "ResponseDocument",
      namespace = "http://www.opengis.net/wps/1.0.0"
   )
   protected ResponseDocumentType responseDocument;
   @XmlElement(
      name = "RawDataOutput",
      namespace = "http://www.opengis.net/wps/1.0.0"
   )
   protected OutputDefinitionType rawDataOutput;

   public ResponseDocumentType getResponseDocument() {
      return this.responseDocument;
   }

   public void setResponseDocument(ResponseDocumentType value) {
      this.responseDocument = value;
   }

   public OutputDefinitionType getRawDataOutput() {
      return this.rawDataOutput;
   }

   public void setRawDataOutput(OutputDefinitionType value) {
      this.rawDataOutput = value;
   }
}
