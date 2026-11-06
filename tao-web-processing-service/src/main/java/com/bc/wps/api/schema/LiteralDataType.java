package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "LiteralDataType",
   propOrder = {"value"}
)
public class LiteralDataType {
   @XmlValue
   protected String value;
   @XmlAttribute(
      name = "dataType"
   )
   @XmlSchemaType(
      name = "anyURI"
   )
   protected String dataType;
   @XmlAttribute(
      name = "uom"
   )
   @XmlSchemaType(
      name = "anyURI"
   )
   protected String uom;

   public String getValue() {
      return this.value;
   }

   public void setValue(String value) {
      this.value = value;
   }

   public String getDataType() {
      return this.dataType;
   }

   public void setDataType(String value) {
      this.dataType = value;
   }

   public String getUom() {
      return this.uom;
   }

   public void setUom(String value) {
      this.uom = value;
   }
}
