package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = ""
)
@XmlRootElement(
   name = "WSDL"
)
public class WSDL {
   @XmlAttribute(
      name = "href",
      namespace = "http://www.w3.org/1999/xlink",
      required = true
   )
   protected String href;

   public String getHref() {
      return this.href;
   }

   public void setHref(String value) {
      this.href = value;
   }
}
