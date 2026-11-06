package com.bc.wps.api.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "TelephoneType",
   propOrder = {"voice", "facsimile"}
)
public class TelephoneType {
   @XmlElement(
      name = "Voice",
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected List<String> voice;
   @XmlElement(
      name = "Facsimile",
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected List<String> facsimile;

   public List<String> getVoice() {
      if (this.voice == null) {
         this.voice = new ArrayList();
      }

      return this.voice;
   }

   public List<String> getFacsimile() {
      if (this.facsimile == null) {
         this.facsimile = new ArrayList();
      }

      return this.facsimile;
   }
}
