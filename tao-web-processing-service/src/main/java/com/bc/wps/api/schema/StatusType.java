package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;
import javax.xml.datatype.XMLGregorianCalendar;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "StatusType",
   propOrder = {"processAccepted", "processStarted", "processPaused", "processSucceeded", "processFailed"}
)
public class StatusType {
   @XmlElement(
      name = "ProcessAccepted",
      namespace = "http://www.opengis.net/wps/1.0.0"
   )
   protected String processAccepted;
   @XmlElement(
      name = "ProcessStarted",
      namespace = "http://www.opengis.net/wps/1.0.0"
   )
   protected ProcessStartedType processStarted;
   @XmlElement(
      name = "ProcessPaused",
      namespace = "http://www.opengis.net/wps/1.0.0"
   )
   protected ProcessStartedType processPaused;
   @XmlElement(
      name = "ProcessSucceeded",
      namespace = "http://www.opengis.net/wps/1.0.0"
   )
   protected String processSucceeded;
   @XmlElement(
      name = "ProcessFailed",
      namespace = "http://www.opengis.net/wps/1.0.0"
   )
   protected ProcessFailedType processFailed;
   @XmlAttribute(
      name = "creationTime",
      required = true
   )
   @XmlSchemaType(
      name = "dateTime"
   )
   protected XMLGregorianCalendar creationTime;

   public String getProcessAccepted() {
      return this.processAccepted;
   }

   public void setProcessAccepted(String value) {
      this.processAccepted = value;
   }

   public ProcessStartedType getProcessStarted() {
      return this.processStarted;
   }

   public void setProcessStarted(ProcessStartedType value) {
      this.processStarted = value;
   }

   public ProcessStartedType getProcessPaused() {
      return this.processPaused;
   }

   public void setProcessPaused(ProcessStartedType value) {
      this.processPaused = value;
   }

   public String getProcessSucceeded() {
      return this.processSucceeded;
   }

   public void setProcessSucceeded(String value) {
      this.processSucceeded = value;
   }

   public ProcessFailedType getProcessFailed() {
      return this.processFailed;
   }

   public void setProcessFailed(ProcessFailedType value) {
      this.processFailed = value;
   }

   public XMLGregorianCalendar getCreationTime() {
      return this.creationTime;
   }

   public void setCreationTime(XMLGregorianCalendar value) {
      this.creationTime = value;
   }
}
