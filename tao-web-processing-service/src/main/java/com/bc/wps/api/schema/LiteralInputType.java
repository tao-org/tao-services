package com.bc.wps.api.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "LiteralInputType",
   propOrder = {"allowedValues", "anyValue", "valuesReference", "defaultValue"}
)
public class LiteralInputType extends LiteralOutputType {
   @XmlElement(
      name = "AllowedValues",
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected AllowedValues allowedValues;
   @XmlElement(
      name = "AnyValue",
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected AnyValue anyValue;
   @XmlElement(
      name = "ValuesReference",
      namespace = ""
   )
   protected ValuesReferenceType valuesReference;
   @XmlElement(
      name = "DefaultValue"
   )
   protected String defaultValue;

   public AllowedValues getAllowedValues() {
      return this.allowedValues;
   }

   public void setAllowedValues(AllowedValues value) {
      this.allowedValues = value;
   }

   public AnyValue getAnyValue() {
      return this.anyValue;
   }

   public void setAnyValue(AnyValue value) {
      this.anyValue = value;
   }

   public ValuesReferenceType getValuesReference() {
      return this.valuesReference;
   }

   public void setValuesReference(ValuesReferenceType value) {
      this.valuesReference = value;
   }

   public String getDefaultValue() {
      return this.defaultValue;
   }

   public void setDefaultValue(String value) {
      this.defaultValue = value;
   }
}
