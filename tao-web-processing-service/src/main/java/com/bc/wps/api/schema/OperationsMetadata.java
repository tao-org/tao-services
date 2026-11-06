package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "",
   propOrder = {"operation", "parameter", "constraint", "extendedCapabilities"}
)
@XmlRootElement(
   name = "OperationsMetadata"
)
public class OperationsMetadata {
   @XmlElement(
      name = "Operation",
      namespace = "http://www.opengis.net/ows/1.1",
      required = true
   )
   protected List<Operation> operation;
   @XmlElement(
      name = "Parameter"
   )
   protected List<DomainType> parameter;
   @XmlElement(
      name = "Constraint"
   )
   protected List<DomainType> constraint;
   @XmlElement(
      name = "ExtendedCapabilities"
   )
   protected Object extendedCapabilities;

   public List<Operation> getOperation() {
      if (this.operation == null) {
         this.operation = new ArrayList();
      }

      return this.operation;
   }

   public List<DomainType> getParameter() {
      if (this.parameter == null) {
         this.parameter = new ArrayList();
      }

      return this.parameter;
   }

   public List<DomainType> getConstraint() {
      if (this.constraint == null) {
         this.constraint = new ArrayList();
      }

      return this.constraint;
   }

   public Object getExtendedCapabilities() {
      return this.extendedCapabilities;
   }

   public void setExtendedCapabilities(Object value) {
      this.extendedCapabilities = value;
   }
}
