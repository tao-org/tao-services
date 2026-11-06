package com.bc.wps.api.schema;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(
   name = "showType"
)
@XmlEnum
public enum ShowType {
   @XmlEnumValue("new")
   NEW("new"),
   @XmlEnumValue("replace")
   REPLACE("replace"),
   @XmlEnumValue("embed")
   EMBED("embed"),
   @XmlEnumValue("other")
   OTHER("other"),
   @XmlEnumValue("none")
   NONE("none");

   private final String value;

   private ShowType(String v) {
      this.value = v;
   }

   public String value() {
      return this.value;
   }

   public static ShowType fromValue(String v) {
      for(ShowType c : values()) {
         if (c.value.equals(v)) {
            return c;
         }
      }

      throw new IllegalArgumentException(v);
   }
}
