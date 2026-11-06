package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "",
   propOrder = {"_default", "supported"}
)
@XmlRootElement(
   name = "Languages"
)
public class Languages {
   @XmlElement(
      name = "Default",
      namespace = "http://www.opengis.net/wps/1.0.0",
      required = true
   )
   protected Default _default;
   @XmlElement(
      name = "Supported",
      namespace = "http://www.opengis.net/wps/1.0.0",
      required = true
   )
   protected LanguagesType supported;

   public Default getDefault() {
      return this._default;
   }

   public void setDefault(Default value) {
      this._default = value;
   }

   public LanguagesType getSupported() {
      return this.supported;
   }

   public void setSupported(LanguagesType value) {
      this.supported = value;
   }

   @XmlAccessorType(XmlAccessType.FIELD)
   @XmlType(
      name = "",
      propOrder = {"language"}
   )
   public static class Default {
      @XmlElement(
         name = "Language",
         namespace = "http://www.opengis.net/ows/1.1",
         required = true
      )
      @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
      @XmlSchemaType(
         name = "language"
      )
      protected String language;

      public String getLanguage() {
         return this.language;
      }

      public void setLanguage(String value) {
         this.language = value;
      }
   }
}
