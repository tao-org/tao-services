package com.bc.wps.api.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "DataType",
   propOrder = {"complexData", "literalData", "boundingBoxData"}
)
public class DataType {
   @XmlElement(
      name = "ComplexData",
      namespace = "http://www.opengis.net/wps/1.0.0"
   )
   protected ComplexDataType complexData;
   @XmlElement(
      name = "LiteralData",
      namespace = "http://www.opengis.net/wps/1.0.0"
   )
   protected LiteralDataType literalData;
   @XmlElement(
      name = "BoundingBoxData",
      namespace = "http://www.opengis.net/wps/1.0.0"
   )
   protected BoundingBoxType boundingBoxData;

   public ComplexDataType getComplexData() {
      return this.complexData;
   }

   public void setComplexData(ComplexDataType value) {
      this.complexData = value;
   }

   public LiteralDataType getLiteralData() {
      return this.literalData;
   }

   public void setLiteralData(LiteralDataType value) {
      this.literalData = value;
   }

   public BoundingBoxType getBoundingBoxData() {
      return this.boundingBoxData;
   }

   public void setBoundingBoxData(BoundingBoxType value) {
      this.boundingBoxData = value;
   }
}
