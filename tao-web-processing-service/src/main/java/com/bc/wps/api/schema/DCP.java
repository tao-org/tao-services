package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "",
   propOrder = {"http"}
)
@XmlRootElement(
   name = "DCP"
)
public class DCP {
   @XmlElement(
      name = "HTTP",
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected HTTP http;

   public HTTP getHTTP() {
      return this.http;
   }

   public void setHTTP(HTTP value) {
      this.http = value;
   }
}
