package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "ComplexDataDescriptionType",
   propOrder = {"mimeType", "encoding", "schema"}
)
public class ComplexDataDescriptionType {
   @XmlElement(
      name = "MimeType",
      required = true
   )
   protected String mimeType;
   @XmlElement(
      name = "Encoding"
   )
   @XmlSchemaType(
      name = "anyURI"
   )
   protected String encoding;
   @XmlElement(
      name = "Schema"
   )
   @XmlSchemaType(
      name = "anyURI"
   )
   protected String schema;

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
