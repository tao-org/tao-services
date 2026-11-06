package com.bc.wps.api.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "RequestMethodType",
   namespace = "http://www.opengis.net/ows/1.1",
   propOrder = {"constraint"}
)
public class RequestMethodType extends OnlineResourceType {
   @XmlElement(
      name = "Constraint"
   )
   protected List<DomainType> constraint;

   public List<DomainType> getConstraint() {
      if (this.constraint == null) {
         this.constraint = new ArrayList();
      }

      return this.constraint;
   }
}
