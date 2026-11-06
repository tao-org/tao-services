package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "OutputDefinitionType",
   propOrder = {"identifier"}
)
@XmlSeeAlso({DocumentOutputDefinitionType.class})
public class OutputDefinitionType {
   @XmlElement(
      name = "Identifier",
      namespace = "http://www.opengis.net/ows/1.1",
      required = true
   )
   protected CodeType identifier;
   @XmlAttribute(
      name = "uom"
   )
   @XmlSchemaType(
      name = "anyURI"
   )
   protected String uom;
   @XmlAttribute(
      name = "mimeType"
   )
   protected String mimeType;
   @XmlAttribute(
      name = "encoding"
   )
   @XmlSchemaType(
      name = "anyURI"
   )
   protected String encoding;
   @XmlAttribute(
      name = "schema"
   )
   @XmlSchemaType(
      name = "anyURI"
   )
   protected String schema;

   public CodeType getIdentifier() {
      return this.identifier;
   }

   public void setIdentifier(CodeType value) {
      this.identifier = value;
   }

   public String getUom() {
      return this.uom;
   }

   public void setUom(String value) {
      this.uom = value;
   }

   public String getMimeType() {
      return this.mimeType;
   }

   public void setMimeType(String value) {
      this.mimeType = value;
   }

   public String getEncoding() {
      return this.encoding;
   }

   public void setEncoding(String value) {
      this.encoding = value;
   }

   public String getSchema() {
      return this.schema;
   }

   public void setSchema(String value) {
      this.schema = value;
   }
}
