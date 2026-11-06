package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "ProcessDescriptionType",
   propOrder = {"dataInputs", "processOutputs"}
)
public class ProcessDescriptionType extends ProcessBriefType {
   @XmlElement(
      name = "DataInputs"
   )
   protected DataInputs dataInputs;
   @XmlElement(
      name = "ProcessOutputs",
      required = true
   )
   protected ProcessOutputs processOutputs;
   @XmlAttribute(
      name = "storeSupported"
   )
   protected Boolean storeSupported;
   @XmlAttribute(
      name = "statusSupported"
   )
   protected Boolean statusSupported;

   public DataInputs getDataInputs() {
      return this.dataInputs;
   }

   public void setDataInputs(DataInputs value) {
      this.dataInputs = value;
   }

   public ProcessOutputs getProcessOutputs() {
      return this.processOutputs;
   }

   public void setProcessOutputs(ProcessOutputs value) {
      this.processOutputs = value;
   }

   public boolean isStoreSupported() {
      return this.storeSupported == null ? false : this.storeSupported;
   }

   public void setStoreSupported(Boolean value) {
      this.storeSupported = value;
   }

   public boolean isStatusSupported() {
      return this.statusSupported == null ? false : this.statusSupported;
   }

   public void setStatusSupported(Boolean value) {
      this.statusSupported = value;
   }

   @XmlAccessorType(XmlAccessType.FIELD)
   @XmlType(
      name = "",
      propOrder = {"input"}
   )
   public static class DataInputs {
      @XmlElement(
         name = "Input",
         required = true
      )
      protected List<InputDescriptionType> input;

      public List<InputDescriptionType> getInput() {
         if (this.input == null) {
            this.input = new ArrayList();
         }

         return this.input;
      }
   }

   @XmlAccessorType(XmlAccessType.FIELD)
   @XmlType(
      name = "",
      propOrder = {"output"}
   )
   public static class ProcessOutputs {
      @XmlElement(
         name = "Output",
         required = true
      )
      protected List<OutputDescriptionType> output;

      public List<OutputDescriptionType> getOutput() {
         if (this.output == null) {
            this.output = new ArrayList();
         }

         return this.output;
      }
   }
}
