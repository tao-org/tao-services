package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "",
   propOrder = {"process"}
)
@XmlRootElement(
   name = "ProcessOfferings"
)
public class ProcessOfferings {
   @XmlElement(
      name = "Process",
      namespace = "http://www.opengis.net/wps/1.0.0",
      required = true
   )
   protected List<ProcessBriefType> process;

   public List<ProcessBriefType> getProcess() {
      if (this.process == null) {
         this.process = new ArrayList();
      }

      return this.process;
   }
}
