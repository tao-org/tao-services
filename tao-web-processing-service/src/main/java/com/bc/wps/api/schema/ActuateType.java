package com.bc.wps.api.schema;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(
   name = "actuateType"
)
@XmlEnum
public enum ActuateType {
   @XmlEnumValue("onLoad")
   ON_LOAD("onLoad"),
   @XmlEnumValue("onRequest")
   ON_REQUEST("onRequest"),
   @XmlEnumValue("other")
   OTHER("other"),
   @XmlEnumValue("none")
   NONE("none");

   private final String value;

   private ActuateType(String v) {
      this.value = v;
   }

   public String value() {
      return this.value;
   }

   public static ActuateType fromValue(String v) {
      for(ActuateType c : values()) {
         if (c.value.equals(v)) {
            return c;
         }
      }

      throw new IllegalArgumentException(v);
   }
}
