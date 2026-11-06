package com.bc.wps.api.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "AcceptVersionsType",
   propOrder = {"version"}
)
public class AcceptVersionsType {
   @XmlElement(
      name = "Version",
      required = true,
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected List<String> version;

   public List<String> getVersion() {
      if (this.version == null) {
         this.version = new ArrayList();
      }

      return this.version;
   }
}
