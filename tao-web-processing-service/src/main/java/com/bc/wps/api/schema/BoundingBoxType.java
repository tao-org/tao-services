package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "BoundingBoxType",
   propOrder = {"lowerCorner", "upperCorner"}
)
@XmlSeeAlso({WGS84BoundingBoxType.class})
public class BoundingBoxType {
   @XmlList
   @XmlElement(
      name = "LowerCorner",
      namespace = "http://www.opengis.net/ows/1.1",
      type = Double.class
   )
   @XmlSchemaType(
      name = "anySimpleType"
   )
   protected List<Double> lowerCorner;
   @XmlList
   @XmlElement(
      name = "UpperCorner",
      namespace = "http://www.opengis.net/ows/1.1",
      type = Double.class
   )
   @XmlSchemaType(
      name = "anySimpleType"
   )
   protected List<Double> upperCorner;
   @XmlAttribute(
      name = "crs",
      namespace = "http://www.w3.org/2001/XMLSchema"
   )
   @XmlSchemaType(
      name = "anyURI"
   )
   protected String crs;
   @XmlAttribute(
      name = "dimensions"
   )
   @XmlSchemaType(
      name = "positiveInteger"
   )
   protected BigInteger dimensions;

   public List<Double> getLowerCorner() {
      if (this.lowerCorner == null) {
         this.lowerCorner = new ArrayList();
      }

      return this.lowerCorner;
   }

   public List<Double> getUpperCorner() {
      if (this.upperCorner == null) {
         this.upperCorner = new ArrayList();
      }

      return this.upperCorner;
   }

   public String getCrs() {
      return this.crs;
   }

   public void setCrs(String value) {
      this.crs = value;
   }

   public BigInteger getDimensions() {
      return this.dimensions;
   }

   public void setDimensions(BigInteger value) {
      this.dimensions = value;
   }
}
