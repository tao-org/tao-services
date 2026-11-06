package com.bc.wps.api.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "ComplexDataCombinationsType",
   propOrder = {"format"}
)
public class ComplexDataCombinationsType {
   @XmlElement(
      name = "Format",
      required = true
   )
   protected List<ComplexDataDescriptionType> format;

   public List<ComplexDataDescriptionType> getFormat() {
      if (this.format == null) {
         this.format = new ArrayList();
      }

      return this.format;
   }
}
