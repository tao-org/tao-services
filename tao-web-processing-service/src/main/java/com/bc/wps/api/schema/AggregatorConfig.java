package com.bc.wps.api.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "AggregatorConfig",
   propOrder = {"type", "percentage", "varName"}
)
public class AggregatorConfig {
   @XmlElement(
      name = "type"
   )
   protected String type;
   @XmlElement(
      name = "percentage"
   )
   protected String percentage;
   @XmlElement(
      name = "varName"
   )
   protected String varName;

   public String getType() {
      return this.type;
   }

   public void setType(String type) {
      this.type = type;
   }

   public String getPercentage() {
      return this.percentage;
   }

   public void setPercentage(String percentage) {
      this.percentage = percentage;
   }

   public String getVarName() {
      return this.varName;
   }

   public void setVarName(String varName) {
      this.varName = varName;
   }
}
