package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "OnlineResourceType"
)
@XmlSeeAlso({RequestMethodType.class})
public class OnlineResourceType {
   @XmlAttribute(
      name = "type",
      namespace = "http://www.w3.org/1999/xlink"
   )
   protected TypeType type;
   @XmlAttribute(
      name = "href",
      namespace = "http://www.w3.org/1999/xlink"
   )
   protected String href;
   @XmlAttribute(
      name = "role",
      namespace = "http://www.w3.org/1999/xlink"
   )
   protected String role;
   @XmlAttribute(
      name = "arcrole",
      namespace = "http://www.w3.org/1999/xlink"
   )
   protected String arcrole;
   @XmlAttribute(
      name = "title",
      namespace = "http://www.w3.org/1999/xlink"
   )
   protected String title;
   @XmlAttribute(
      name = "show",
      namespace = "http://www.w3.org/1999/xlink"
   )
   protected ShowType show;
   @XmlAttribute(
      name = "actuate",
      namespace = "http://www.w3.org/1999/xlink"
   )
   protected ActuateType actuate;

   public TypeType getType() {
      return this.type == null ? TypeType.SIMPLE : this.type;
   }

   public void setType(TypeType value) {
      this.type = value;
   }

   public String getHref() {
      return this.href;
   }

   public void setHref(String value) {
      this.href = value;
   }

   public String getRole() {
      return this.role;
   }

   public void setRole(String value) {
      this.role = value;
   }

   public String getArcrole() {
      return this.arcrole;
   }

   public void setArcrole(String value) {
      this.arcrole = value;
   }

   public String getTitle() {
      return this.title;
   }

   public void setTitle(String value) {
      this.title = value;
   }

   public ShowType getShow() {
      return this.show;
   }

   public void setShow(ShowType value) {
      this.show = value;
   }

   public ActuateType getActuate() {
      return this.actuate;
   }

   public void setActuate(ActuateType value) {
      this.actuate = value;
   }
}
