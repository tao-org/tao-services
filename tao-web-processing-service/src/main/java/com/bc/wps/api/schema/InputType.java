package com.bc.wps.api.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "InputType",
   propOrder = {"identifier", "title", "_abstract", "reference", "data"}
)
public class InputType {
   @XmlElement(
      name = "Identifier",
      namespace = "http://www.opengis.net/ows/1.1",
      required = true
   )
   protected CodeType identifier;
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
   @XmlElement(
      name = "Reference",
      namespace = "http://www.opengis.net/wps/1.0.0"
   )
   protected InputReferenceType reference;
   @XmlElement(
      name = "Data",
      namespace = "http://www.opengis.net/wps/1.0.0"
   )
   protected DataType data;

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

   public InputReferenceType getReference() {
      return this.reference;
   }

   public void setReference(InputReferenceType value) {
      this.reference = value;
   }

   public DataType getData() {
      return this.data;
   }

   public void setData(DataType value) {
      this.data = value;
   }
}
