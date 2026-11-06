package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "DocumentOutputDefinitionType",
   propOrder = {"title", "_abstract"}
)
public class DocumentOutputDefinitionType extends OutputDefinitionType {
   @XmlElement(
      name = "Title",
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected LanguageStringType title;
   @XmlElement(
      name = "Abstract",
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected LanguageStringType _abstract;
   @XmlAttribute(
      name = "asReference"
   )
   protected Boolean asReference;

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

   public boolean isAsReference() {
      return this.asReference == null ? false : this.asReference;
   }

   public void setAsReference(Boolean value) {
      this.asReference = value;
   }
}
