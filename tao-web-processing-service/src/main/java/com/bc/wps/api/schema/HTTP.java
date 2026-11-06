package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "",
   propOrder = {"get", "post"}
)
@XmlRootElement(
   name = "HTTP"
)
public class HTTP {
   @XmlElement(
      name = "Get",
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected RequestMethodType get;
   @XmlElement(
      name = "Post",
      namespace = "http://www.opengis.net/ows/1.1"
   )
   protected RequestMethodType post;

   public RequestMethodType getGet() {
      return this.get;
   }

   public void setGet(RequestMethodType get) {
      this.get = get;
   }

   public RequestMethodType getPost() {
      return this.post;
   }

   public void setPost(RequestMethodType post) {
      this.post = post;
   }
}
