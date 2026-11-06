package com.bc.wps.api.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "UOMsType",
   propOrder = {"uom"}
)
public class UOMsType {
   @XmlElement(
      name = "UOM",
      namespace = "http://www.opengis.net/ows/1.1",
      required = true
   )
   protected List<DomainMetadataType> uom;

   public List<DomainMetadataType> getUOM() {
      if (this.uom == null) {
         this.uom = new ArrayList();
      }

      return this.uom;
   }
}
