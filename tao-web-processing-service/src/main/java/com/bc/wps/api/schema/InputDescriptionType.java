package com.bc.wps.api.schema;

import com.bc.wps.api.utils.InputDescriptionTypeBuilder;

import javax.xml.bind.annotation.*;
import java.math.BigInteger;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "InputDescriptionType",
   propOrder = {"complexData", "literalData", "boundingBoxData"}
)
public class InputDescriptionType extends DescriptionType {
   @XmlElement(
      name = "ComplexData"
   )
   protected SupportedComplexDataInputType complexData;
   @XmlElement(
      name = "LiteralData"
   )
   protected LiteralInputType literalData;
   @XmlElement(
      name = "BoundingBoxData"
   )
   protected SupportedCRSsType boundingBoxData;
   @XmlAttribute(
      name = "minOccurs",
      required = true
   )
   @XmlSchemaType(
      name = "nonNegativeInteger"
   )
   protected BigInteger minOccurs;
   @XmlAttribute(
      name = "maxOccurs",
      required = true
   )
   @XmlSchemaType(
      name = "positiveInteger"
   )
   protected BigInteger maxOccurs;

   public InputDescriptionType(InputDescriptionTypeBuilder builder) {
      this.identifier = builder.getIdentifier();
      this.title = builder.getTitle();
      this._abstract = builder.getAbstractValue();
      this.literalData = builder.getLiteralInputType();
      this.minOccurs = builder.getMinOccurs();
      this.maxOccurs = builder.getMaxOccurs();
   }

   public InputDescriptionType() {
   }

   public SupportedComplexDataInputType getComplexData() {
      return this.complexData;
   }

   public void setComplexData(SupportedComplexDataInputType value) {
      this.complexData = value;
   }

   public LiteralInputType getLiteralData() {
      return this.literalData;
   }

   public void setLiteralData(LiteralInputType value) {
      this.literalData = value;
   }

   public SupportedCRSsType getBoundingBoxData() {
      return this.boundingBoxData;
   }

   public void setBoundingBoxData(SupportedCRSsType value) {
      this.boundingBoxData = value;
   }

   public BigInteger getMinOccurs() {
      return this.minOccurs;
   }

   public void setMinOccurs(BigInteger value) {
      this.minOccurs = value;
   }

   public BigInteger getMaxOccurs() {
      return this.maxOccurs;
   }

   public void setMaxOccurs(BigInteger value) {
      this.maxOccurs = value;
   }
}
