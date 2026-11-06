package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "CRSsType",
   propOrder = {"crs"}
)
public class CRSsType {
   @XmlElement(
      name = "CRS",
      namespace = "",
      required = true
   )
   @XmlSchemaType(
      name = "anyURI"
   )
   protected List<String> crs;

   public List<String> getCRS() {
      if (this.crs == null) {
         this.crs = new ArrayList();
      }

      return this.crs;
   }
}
