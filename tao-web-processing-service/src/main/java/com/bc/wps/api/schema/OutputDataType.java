package com.bc.wps.api.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "OutputDataType",
   propOrder = {"reference", "data"}
)
public class OutputDataType extends DescriptionType {
   @XmlElement(
      name = "Reference",
      namespace = "http://www.opengis.net/wps/1.0.0"
   )
   protected OutputReferenceType reference;
   @XmlElement(
      name = "Data",
      namespace = "http://www.opengis.net/wps/1.0.0"
   )
   protected DataType data;

   public OutputReferenceType getReference() {
      return this.reference;
   }

   public void setReference(OutputReferenceType value) {
      this.reference = value;
   }

   public DataType getData() {
      return this.data;
   }

   public void setData(DataType value) {
      this.data = value;
   }
}
