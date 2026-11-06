package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "OutputReferenceType"
)
public class OutputReferenceType {
   @XmlAttribute(
      name = "href",
      required = true
   )
   @XmlSchemaType(
      name = "anyURI"
   )
   protected String href;
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

   public String getHref() {
      return this.href;
   }

   public void setHref(String value) {
      this.href = value;
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
