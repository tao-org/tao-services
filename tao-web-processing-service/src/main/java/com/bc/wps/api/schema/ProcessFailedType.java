package com.bc.wps.api.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "ProcessFailedType",
   propOrder = {"exceptionReport"}
)
public class ProcessFailedType {
   @XmlElement(
      name = "ExceptionReport",
      namespace = "http://www.opengis.net/ows/1.1",
      required = true
   )
   protected ExceptionReport exceptionReport;

   public ExceptionReport getExceptionReport() {
      return this.exceptionReport;
   }

   public void setExceptionReport(ExceptionReport value) {
      this.exceptionReport = value;
   }
}
