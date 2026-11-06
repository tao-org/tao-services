package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "",
   propOrder = {"value"}
)
@XmlRootElement(
   name = "ValuesReference"
)
public class ValuesReference {
   @XmlValue
   protected String value;
   @XmlAttribute(
      name = "reference",
      namespace = "http://www.opengis.net/ows/1.1",
      required = true
   )
   @XmlSchemaType(
      name = "anyURI"
   )
   protected String reference;

   public String getValue() {
      return this.value;
   }

   public void setValue(String value) {
      this.value = value;
   }

   public String getReference() {
      return this.reference;
   }

   public void setReference(String value) {
      this.reference = value;
   }
}
