package com.bc.wps.api.schema;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(
   name = "typeType"
)
@XmlEnum
public enum TypeType {
   @XmlEnumValue("simple")
   SIMPLE("simple"),
   @XmlEnumValue("extended")
   EXTENDED("extended"),
   @XmlEnumValue("title")
   TITLE("title"),
   @XmlEnumValue("resource")
   RESOURCE("resource"),
   @XmlEnumValue("locator")
   LOCATOR("locator"),
   @XmlEnumValue("arc")
   ARC("arc");

   private final String value;

   private TypeType(String v) {
      this.value = v;
   }

   public String value() {
      return this.value;
   }

   public static TypeType fromValue(String v) {
      for(TypeType c : values()) {
         if (c.value.equals(v)) {
            return c;
         }
      }

      throw new IllegalArgumentException(v);
   }
}
