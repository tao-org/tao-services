package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "",
   propOrder = {"process", "status", "dataInputs", "outputDefinitions", "processOutputs"}
)
@XmlRootElement(
   name = "ExecuteResponse"
)
public class ExecuteResponse extends ResponseBaseType {
   @XmlElement(
      name = "Process",
      namespace = "http://www.opengis.net/wps/1.0.0",
      required = true
   )
   protected ProcessBriefType process;
   @XmlElement(
      name = "Status",
      namespace = "http://www.opengis.net/wps/1.0.0",
      required = true
   )
   protected StatusType status;
   @XmlElement(
      name = "DataInputs",
      namespace = "http://www.opengis.net/wps/1.0.0"
   )
   protected DataInputsType dataInputs;
   @XmlElement(
      name = "OutputDefinitions",
      namespace = "http://www.opengis.net/wps/1.0.0"
   )
   protected OutputDefinitionsType outputDefinitions;
   @XmlElement(
      name = "ProcessOutputs",
      namespace = "http://www.opengis.net/wps/1.0.0"
   )
   protected ProcessOutputs processOutputs;
   @XmlAttribute(
      name = "serviceInstance",
      required = true
   )
   @XmlSchemaType(
      name = "anyURI"
   )
   protected String serviceInstance;
   @XmlAttribute(
      name = "statusLocation"
   )
   @XmlSchemaType(
      name = "anyURI"
   )
   protected String statusLocation;

   public ProcessBriefType getProcess() {
      return this.process;
   }

   public void setProcess(ProcessBriefType value) {
      this.process = value;
   }

   public StatusType getStatus() {
      return this.status;
   }

   public void setStatus(StatusType value) {
      this.status = value;
   }

   public DataInputsType getDataInputs() {
      return this.dataInputs;
   }

   public void setDataInputs(DataInputsType value) {
      this.dataInputs = value;
   }

   public OutputDefinitionsType getOutputDefinitions() {
      return this.outputDefinitions;
   }

   public void setOutputDefinitions(OutputDefinitionsType value) {
      this.outputDefinitions = value;
   }

   public ProcessOutputs getProcessOutputs() {
      return this.processOutputs;
   }

   public void setProcessOutputs(ProcessOutputs value) {
      this.processOutputs = value;
   }

   public String getServiceInstance() {
      return this.serviceInstance;
   }

   public void setServiceInstance(String value) {
      this.serviceInstance = value;
   }

   public String getStatusLocation() {
      return this.statusLocation;
   }

   public void setStatusLocation(String value) {
      this.statusLocation = value;
   }

   @XmlAccessorType(XmlAccessType.FIELD)
   @XmlType(
      name = "",
      propOrder = {"output"}
   )
   public static class ProcessOutputs {
      @XmlElement(
         name = "Output",
         namespace = "http://www.opengis.net/wps/1.0.0",
         required = true
      )
      protected List<OutputDataType> output;

      public List<OutputDataType> getOutput() {
         if (this.output == null) {
            this.output = new ArrayList();
         }

         return this.output;
      }
   }
}
