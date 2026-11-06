package com.bc.wps.api.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "VariableConfig",
   propOrder = {"name", "expr"}
)
public class VariableConfig {
   @XmlElement(
      name = "name",
      nillable = false,
      required = true
   )
   protected String name;
   @XmlElement(
      name = "expr",
      nillable = false,
      required = true
   )
   protected String expr;

   public String getName() {
      return this.name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getExpr() {
      return this.expr;
   }

   public void setExpr(String expr) {
      this.expr = expr;
   }
}
