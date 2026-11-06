package com.bc.wps.api.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "OutputDefinitionsType",
   propOrder = {"output"}
)
public class OutputDefinitionsType {
   @XmlElement(
      name = "Output",
      required = true
   )
   protected List<DocumentOutputDefinitionType> output;

   public List<DocumentOutputDefinitionType> getOutput() {
      if (this.output == null) {
         this.output = new ArrayList();
      }

      return this.output;
   }
}
