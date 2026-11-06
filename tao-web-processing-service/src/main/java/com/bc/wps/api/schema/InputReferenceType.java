package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "InputReferenceType",
   propOrder = {"header", "body", "bodyReference"}
)
public class InputReferenceType {
   @XmlElement(
      name = "Header"
   )
   protected List<Header> header;
   @XmlElement(
      name = "Body"
   )
   protected Object body;
   @XmlElement(
      name = "BodyReference"
   )
   protected BodyReference bodyReference;
   @XmlAttribute(
      name = "href",
      namespace = "http://www.w3.org/1999/xlink",
      required = true
   )
   protected String href;
   @XmlAttribute(
      name = "method"
   )
   protected String method;
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

   public List<Header> getHeader() {
      if (this.header == null) {
         this.header = new ArrayList();
      }

      return this.header;
   }

   public Object getBody() {
      return this.body;
   }

   public void setBody(Object value) {
      this.body = value;
   }

   public BodyReference getBodyReference() {
      return this.bodyReference;
   }

   public void setBodyReference(BodyReference value) {
      this.bodyReference = value;
   }

   public String getHref() {
      return this.href;
   }

   public void setHref(String value) {
      this.href = value;
   }

   public String getMethod() {
      return this.method == null ? "GET" : this.method;
   }

   public void setMethod(String value) {
      this.method = value;
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

   @XmlAccessorType(XmlAccessType.FIELD)
   @XmlType(
      name = ""
   )
   public static class BodyReference {
      @XmlAttribute(
         name = "href",
         namespace = "http://www.w3.org/1999/xlink",
         required = true
      )
      protected String href;

      public String getHref() {
         return this.href;
      }

      public void setHref(String value) {
         this.href = value;
      }
   }

   @XmlAccessorType(XmlAccessType.FIELD)
   @XmlType(
      name = ""
   )
   public static class Header {
      @XmlAttribute(
         name = "key",
         required = true
      )
      protected String key;
      @XmlAttribute(
         name = "value",
         required = true
      )
      protected String value;

      public String getKey() {
         return this.key;
      }

      public void setKey(String value) {
         this.key = value;
      }

      public String getValue() {
         return this.value;
      }

      public void setValue(String value) {
         this.value = value;
      }
   }
}
