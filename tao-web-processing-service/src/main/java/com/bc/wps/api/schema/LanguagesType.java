package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "LanguagesType",
   propOrder = {"language"}
)
public class LanguagesType {
   @XmlElement(
      name = "Language",
      namespace = "http://www.opengis.net/ows/1.1",
      required = true
   )
   @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
   @XmlSchemaType(
      name = "language"
   )
   protected List<String> language;

   public List<String> getLanguage() {
      if (this.language == null) {
         this.language = new ArrayList();
      }

      return this.language;
   }
}
