package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "ValuesReferenceType"
)
public class ValuesReferenceType {
   @XmlAttribute(
      name = "reference",
      namespace = "http://www.opengis.net/ows/1.1"
   )
   @XmlSchemaType(
      name = "anyURI"
   )
   protected String reference;
   @XmlAttribute(
      name = "valuesForm"
   )
   @XmlSchemaType(
      name = "anyURI"
   )
   protected String valuesForm;

   public String getReference() {
      return this.reference;
   }

   public void setReference(String value) {
      this.reference = value;
   }

   public String getValuesForm() {
      return this.valuesForm;
   }

   public void setValuesForm(String value) {
      this.valuesForm = value;
   }
}
