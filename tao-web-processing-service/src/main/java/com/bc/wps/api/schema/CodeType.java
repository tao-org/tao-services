package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "CodeType",
   propOrder = {"value"}
)
public class CodeType {
   @XmlValue
   protected String value;
   @XmlAttribute(
      name = "codeSpace"
   )
   @XmlSchemaType(
      name = "anyURI"
   )
   protected String codeSpace;

   public String getValue() {
      return this.value;
   }

   public void setValue(String value) {
      this.value = value;
   }

   public String getCodeSpace() {
      return this.codeSpace;
   }

   public void setCodeSpace(String value) {
      this.codeSpace = value;
   }
}
