package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "DescriptionType",
   propOrder = {"identifier", "title", "_abstract", "metadata"}
)
@XmlSeeAlso({ProcessBriefType.class, InputDescriptionType.class, OutputDataType.class, OutputDescriptionType.class})
public class DescriptionType {
   @XmlElement(
      name = "Identifier",
      namespace = "http://www.opengis.net/ows/1.1",
      required = true
   )
   protected CodeType identifier;
   @XmlElement(
      name = "Title",
      namespace = "http://www.opengis.net/ows/1.1",
      required = true
   )
   protected LanguageStringType title;
   @XmlElement(
      name = "Abstract",
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected LanguageStringType _abstract;
   @XmlElement(
      name = "Metadata",
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected List<MetadataType> metadata;

   public CodeType getIdentifier() {
      return this.identifier;
   }

   public void setIdentifier(CodeType value) {
      this.identifier = value;
   }

   public LanguageStringType getTitle() {
      return this.title;
   }

   public void setTitle(LanguageStringType value) {
      this.title = value;
   }

   public LanguageStringType getAbstract() {
      return this._abstract;
   }

   public void setAbstract(LanguageStringType value) {
      this._abstract = value;
   }

   public List<MetadataType> getMetadata() {
      if (this.metadata == null) {
         this.metadata = new ArrayList();
      }

      return this.metadata;
   }
}
