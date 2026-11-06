package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "",
   propOrder = {"dcp", "parameter", "constraint", "metadata"}
)
@XmlRootElement(
   name = "Operation"
)
public class Operation {
   @XmlElement(
      name = "DCP",
      namespace = "http://www.opengis.net/ows/1.1",
      required = true
   )
   protected List<DCP> dcp;
   @XmlElement(
      name = "Parameter"
   )
   protected List<DomainType> parameter;
   @XmlElement(
      name = "Constraint"
   )
   protected List<DomainType> constraint;
   @XmlElement(
      name = "Metadata"
   )
   protected List<MetadataType> metadata;
   @XmlAttribute(
      name = "name",
      required = true
   )
   protected String name;

   public List<DCP> getDCP() {
      if (this.dcp == null) {
         this.dcp = new ArrayList();
      }

      return this.dcp;
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

   public List<MetadataType> getMetadata() {
      if (this.metadata == null) {
         this.metadata = new ArrayList();
      }

      return this.metadata;
   }

   public String getName() {
      return this.name;
   }

   public void setName(String value) {
      this.name = value;
   }
}
