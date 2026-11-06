package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "ProcessStartedType",
   propOrder = {"value"}
)
public class ProcessStartedType {
   @XmlValue
   protected String value;
   @XmlAttribute(
      name = "percentCompleted"
   )
   protected Integer percentCompleted;

   public String getValue() {
      return this.value;
   }

   public void setValue(String value) {
      this.value = value;
   }

   public Integer getPercentCompleted() {
      return this.percentCompleted;
   }

   public void setPercentCompleted(Integer value) {
      this.percentCompleted = value;
   }
}
