package com.bc.wps.api.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import java.math.BigInteger;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "SupportedComplexDataInputType"
)
public class SupportedComplexDataInputType extends SupportedComplexDataType {
   @XmlAttribute(
      name = "maximumMegabytes"
   )
   protected BigInteger maximumMegabytes;

   public BigInteger getMaximumMegabytes() {
      return this.maximumMegabytes;
   }

   public void setMaximumMegabytes(BigInteger value) {
      this.maximumMegabytes = value;
   }
}
