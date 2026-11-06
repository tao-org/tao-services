package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "LiteralOutputType",
   propOrder = {"dataType", "uoMs"}
)
@XmlSeeAlso({LiteralInputType.class})
public class LiteralOutputType {
   @XmlElement(
      name = "DataType",
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected DomainMetadataType dataType;
   @XmlElement(
      name = "UOMs",
      namespace = ""
   )
   protected SupportedUOMsType uoMs;

   public DomainMetadataType getDataType() {
      return this.dataType;
   }

   public void setDataType(DomainMetadataType value) {
      this.dataType = value;
   }

   public SupportedUOMsType getUOMs() {
      return this.uoMs;
   }

   public void setUOMs(SupportedUOMsType value) {
      this.uoMs = value;
   }
}
