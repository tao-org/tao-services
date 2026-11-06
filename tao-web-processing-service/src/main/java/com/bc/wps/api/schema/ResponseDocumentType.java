package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "ResponseDocumentType",
   propOrder = {"output"}
)
public class ResponseDocumentType {
   @XmlElement(
      name = "Output",
      namespace = "http://www.opengis.net/wps/1.0.0",
      required = true
   )
   protected List<DocumentOutputDefinitionType> output;
   @XmlAttribute(
      name = "storeExecuteResponse"
   )
   protected Boolean storeExecuteResponse;
   @XmlAttribute(
      name = "lineage"
   )
   protected Boolean lineage;
   @XmlAttribute(
      name = "status"
   )
   protected Boolean status;

   public List<DocumentOutputDefinitionType> getOutput() {
      if (this.output == null) {
         this.output = new ArrayList();
      }

      return this.output;
   }

   public boolean isStoreExecuteResponse() {
      return this.storeExecuteResponse == null ? false : this.storeExecuteResponse;
   }

   public void setStoreExecuteResponse(Boolean value) {
      this.storeExecuteResponse = value;
   }

   public boolean isLineage() {
      return this.lineage == null ? false : this.lineage;
   }

   public void setLineage(Boolean value) {
      this.lineage = value;
   }

   public boolean isStatus() {
      return this.status == null ? false : this.status;
   }

   public void setStatus(Boolean value) {
      this.status = value;
   }
}
