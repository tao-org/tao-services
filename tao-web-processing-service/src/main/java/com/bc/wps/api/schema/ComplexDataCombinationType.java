package com.bc.wps.api.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "ComplexDataCombinationType",
   propOrder = {"format"}
)
public class ComplexDataCombinationType {
   @XmlElement(
      name = "Format",
      required = true
   )
   protected ComplexDataDescriptionType format;

   public ComplexDataDescriptionType getFormat() {
      return this.format;
   }

   public void setFormat(ComplexDataDescriptionType value) {
      this.format = value;
   }
}
