package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "SupportedCRSsType",
   propOrder = {"_default", "supported"}
)
public class SupportedCRSsType {
   @XmlElement(
      name = "Default",
      namespace = "",
      required = true
   )
   protected Default _default;
   @XmlElement(
      name = "Supported",
      namespace = "",
      required = true
   )
   protected CRSsType supported;

   public Default getDefault() {
      return this._default;
   }

   public void setDefault(Default value) {
      this._default = value;
   }

   public CRSsType getSupported() {
      return this.supported;
   }

   public void setSupported(CRSsType value) {
      this.supported = value;
   }

   @XmlAccessorType(XmlAccessType.FIELD)
   @XmlType(
      name = "",
      propOrder = {"crs"}
   )
   public static class Default {
      @XmlElement(
         name = "CRS",
         namespace = "",
         required = true
      )
      @XmlSchemaType(
         name = "anyURI"
      )
      protected String crs;

      public String getCRS() {
         return this.crs;
      }

      public void setCRS(String value) {
         this.crs = value;
      }
   }
}
