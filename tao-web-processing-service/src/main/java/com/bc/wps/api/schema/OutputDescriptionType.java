package com.bc.wps.api.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "OutputDescriptionType",
   propOrder = {"complexOutput", "literalOutput", "boundingBoxOutput"}
)
public class OutputDescriptionType extends DescriptionType {
   @XmlElement(
      name = "ComplexOutput"
   )
   protected SupportedComplexDataType complexOutput;
   @XmlElement(
      name = "LiteralOutput"
   )
   protected LiteralOutputType literalOutput;
   @XmlElement(
      name = "BoundingBoxOutput"
   )
   protected SupportedCRSsType boundingBoxOutput;

   public SupportedComplexDataType getComplexOutput() {
      return this.complexOutput;
   }

   public void setComplexOutput(SupportedComplexDataType value) {
      this.complexOutput = value;
   }

   public LiteralOutputType getLiteralOutput() {
      return this.literalOutput;
   }

   public void setLiteralOutput(LiteralOutputType value) {
      this.literalOutput = value;
   }

   public SupportedCRSsType getBoundingBoxOutput() {
      return this.boundingBoxOutput;
   }

   public void setBoundingBoxOutput(SupportedCRSsType value) {
      this.boundingBoxOutput = value;
   }
}
