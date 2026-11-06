package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "RangeType",
   propOrder = {"minimumValue", "maximumValue", "spacing"}
)
public class RangeType {
   @XmlElement(
      name = "MinimumValue"
   )
   protected ValueType minimumValue;
   @XmlElement(
      name = "MaximumValue"
   )
   protected ValueType maximumValue;
   @XmlElement(
      name = "Spacing"
   )
   protected ValueType spacing;
   @XmlAttribute(
      name = "rangeClosure",
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected List<String> rangeClosure;

   public ValueType getMinimumValue() {
      return this.minimumValue;
   }

   public void setMinimumValue(ValueType value) {
      this.minimumValue = value;
   }

   public ValueType getMaximumValue() {
      return this.maximumValue;
   }

   public void setMaximumValue(ValueType value) {
      this.maximumValue = value;
   }

   public ValueType getSpacing() {
      return this.spacing;
   }

   public void setSpacing(ValueType value) {
      this.spacing = value;
   }

   public List<String> getRangeClosure() {
      if (this.rangeClosure == null) {
         this.rangeClosure = new ArrayList();
      }

      return this.rangeClosure;
   }
}
