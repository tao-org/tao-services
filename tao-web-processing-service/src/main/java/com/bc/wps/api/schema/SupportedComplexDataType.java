package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "SupportedComplexDataType",
   propOrder = {"_default", "supported"}
)
@XmlSeeAlso({SupportedComplexDataInputType.class})
public class SupportedComplexDataType {
   @XmlElement(
      name = "Default",
      required = true
   )
   protected ComplexDataCombinationType _default;
   @XmlElement(
      name = "Supported",
      required = true
   )
   protected ComplexDataCombinationsType supported;

   public ComplexDataCombinationType getDefault() {
      return this._default;
   }

   public void setDefault(ComplexDataCombinationType value) {
      this._default = value;
   }

   public ComplexDataCombinationsType getSupported() {
      return this.supported;
   }

   public void setSupported(ComplexDataCombinationsType value) {
      this.supported = value;
   }
}
