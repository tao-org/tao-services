package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "",
   propOrder = {"valueOrRange"}
)
@XmlRootElement(
   name = "AllowedValues"
)
public class AllowedValues {
   @XmlElements({@XmlElement(
   name = "Value",
   namespace = "http://www.opengis.net/ows/1.1",
   type = ValueType.class
), @XmlElement(
   name = "Range",
   namespace = "http://www.opengis.net/ows/1.1",
   type = RangeType.class
)})
   protected List<Object> valueOrRange;

   public List<Object> getValueOrRange() {
      if (this.valueOrRange == null) {
         this.valueOrRange = new ArrayList();
      }

      return this.valueOrRange;
   }
}
