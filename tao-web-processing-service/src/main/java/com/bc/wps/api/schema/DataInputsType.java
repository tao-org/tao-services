package com.bc.wps.api.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "DataInputsType",
   propOrder = {"input"}
)
public class DataInputsType {
   @XmlElement(
      name = "Input",
      namespace = "http://www.opengis.net/wps/1.0.0",
      required = true
   )
   protected List<InputType> input;

   public List<InputType> getInput() {
      if (this.input == null) {
         this.input = new ArrayList();
      }

      return this.input;
   }
}
