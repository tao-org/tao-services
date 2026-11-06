package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "",
   propOrder = {"identifier"}
)
@XmlRootElement(
   name = "DescribeProcess"
)
public class DescribeProcess extends RequestBaseType {
   @XmlElement(
      name = "Identifier",
      namespace = "http://www.opengis.net/ows/1.1",
      required = true
   )
   protected List<CodeType> identifier;

   public List<CodeType> getIdentifier() {
      if (this.identifier == null) {
         this.identifier = new ArrayList();
      }

      return this.identifier;
   }
}
