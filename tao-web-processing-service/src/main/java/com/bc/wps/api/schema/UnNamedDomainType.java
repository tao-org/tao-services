package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "UnNamedDomainType",
   propOrder = {"allowedValues", "anyValue", "noValues", "valuesReference", "defaultValue", "meaning", "dataType", "uom", "referenceSystem", "metadata"}
)
@XmlSeeAlso({DomainType.class})
public class UnNamedDomainType {
   @XmlElement(
      name = "AllowedValues"
   )
   protected AllowedValues allowedValues;
   @XmlElement(
      name = "AnyValue"
   )
   protected AnyValue anyValue;
   @XmlElement(
      name = "NoValues"
   )
   protected NoValues noValues;
   @XmlElement(
      name = "ValuesReference"
   )
   protected ValuesReference valuesReference;
   @XmlElement(
      name = "DefaultValue"
   )
   protected ValueType defaultValue;
   @XmlElement(
      name = "Meaning"
   )
   protected DomainMetadataType meaning;
   @XmlElement(
      name = "DataType"
   )
   protected DomainMetadataType dataType;
   @XmlElement(
      name = "UOM"
   )
   protected DomainMetadataType uom;
   @XmlElement(
      name = "ReferenceSystem"
   )
   protected DomainMetadataType referenceSystem;
   @XmlElement(
      name = "Metadata"
   )
   protected List<MetadataType> metadata;

   public AllowedValues getAllowedValues() {
      return this.allowedValues;
   }

   public void setAllowedValues(AllowedValues value) {
      this.allowedValues = value;
   }

   public AnyValue getAnyValue() {
      return this.anyValue;
   }

   public void setAnyValue(AnyValue value) {
      this.anyValue = value;
   }

   public NoValues getNoValues() {
      return this.noValues;
   }

   public void setNoValues(NoValues value) {
      this.noValues = value;
   }

   public ValuesReference getValuesReference() {
      return this.valuesReference;
   }

   public void setValuesReference(ValuesReference value) {
      this.valuesReference = value;
   }

   public ValueType getDefaultValue() {
      return this.defaultValue;
   }

   public void setDefaultValue(ValueType value) {
      this.defaultValue = value;
   }

   public DomainMetadataType getMeaning() {
      return this.meaning;
   }

   public void setMeaning(DomainMetadataType value) {
      this.meaning = value;
   }

   public DomainMetadataType getDataType() {
      return this.dataType;
   }

   public void setDataType(DomainMetadataType value) {
      this.dataType = value;
   }

   public DomainMetadataType getUOM() {
      return this.uom;
   }

   public void setUOM(DomainMetadataType value) {
      this.uom = value;
   }

   public DomainMetadataType getReferenceSystem() {
      return this.referenceSystem;
   }

   public void setReferenceSystem(DomainMetadataType value) {
      this.referenceSystem = value;
   }

   public List<MetadataType> getMetadata() {
      if (this.metadata == null) {
         this.metadata = new ArrayList();
      }

      return this.metadata;
   }
}
