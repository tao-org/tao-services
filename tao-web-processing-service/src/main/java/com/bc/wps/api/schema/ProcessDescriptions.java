package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "",
   propOrder = {"processDescription"}
)
@XmlRootElement(
   name = "ProcessDescriptions"
)
public class ProcessDescriptions extends ResponseBaseType {
   @XmlElement(
      name = "ProcessDescription",
      required = true
   )
   protected List<ProcessDescriptionType> processDescription;

   public List<ProcessDescriptionType> getProcessDescription() {
      if (this.processDescription == null) {
         this.processDescription = new ArrayList();
      }

      return this.processDescription;
   }
}
