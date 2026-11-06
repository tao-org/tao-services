package com.bc.wps.api.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "SupportedUOMsType",
   propOrder = {"_default", "supported"}
)
public class SupportedUOMsType {
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
   protected UOMsType supported;

   public Default getDefault() {
      return this._default;
   }

   public void setDefault(Default value) {
      this._default = value;
   }

   public UOMsType getSupported() {
      return this.supported;
   }

   public void setSupported(UOMsType value) {
      this.supported = value;
   }

   @XmlAccessorType(XmlAccessType.FIELD)
   @XmlType(
      name = "",
      propOrder = {"uom"}
   )
   public static class Default {
      @XmlElement(
         name = "UOM",
         namespace = "http://www.opengis.net/ows/1.1",
         required = true
      )
      protected DomainMetadataType uom;

      public DomainMetadataType getUOM() {
         return this.uom;
      }

      public void setUOM(DomainMetadataType value) {
         this.uom = value;
      }
   }
}
