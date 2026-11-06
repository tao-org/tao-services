package com.bc.wps.api.utils;

import com.bc.wps.api.schema.*;

import java.math.BigInteger;
import java.util.List;

public class InputDescriptionTypeBuilder {
   private CodeType identifier = new CodeType();
   private LanguageStringType title = new LanguageStringType();
   private LanguageStringType abstractValue = new LanguageStringType();
   private LiteralInputType literalInputType = new LiteralInputType();
   private BigInteger minOccurs;
   private BigInteger maxOccurs;

   private InputDescriptionTypeBuilder() {
      this.minOccurs = BigInteger.ZERO;
      this.maxOccurs = BigInteger.ONE;
   }

   public static InputDescriptionTypeBuilder create() {
      return new InputDescriptionTypeBuilder();
   }

   public InputDescriptionType build() {
      if (this.literalInputType.getAllowedValues() == null) {
         this.literalInputType.setAnyValue(new AnyValue());
      }

      return new InputDescriptionType(this);
   }

   public InputDescriptionTypeBuilder withIdentifier(String identifierText) {
      this.identifier.setValue(identifierText);
      return this;
   }

   public InputDescriptionTypeBuilder withTitle(String title) {
      this.title.setValue(title);
      return this;
   }

   public InputDescriptionTypeBuilder withAbstract(String abstractText) {
      this.abstractValue.setValue(abstractText);
      return this;
   }

   public InputDescriptionTypeBuilder setMinOccurs(BigInteger minOccurs) {
      this.minOccurs = minOccurs;
      return this;
   }

   public InputDescriptionTypeBuilder setMaxOccurs(BigInteger maxOccurs) {
      this.maxOccurs = maxOccurs;
      return this;
   }

   public InputDescriptionTypeBuilder withDefaultValue(String defaultValue) {
      this.literalInputType.setDefaultValue(defaultValue);
      return this;
   }

   public InputDescriptionTypeBuilder withDataType(String dataType) {
      DomainMetadataType dataTypeValue = new DomainMetadataType();
      dataTypeValue.setValue(dataType);
      this.literalInputType.setDataType(dataTypeValue);
      return this;
   }

   public InputDescriptionTypeBuilder withAllowedValues(List<Object> allowedValuesList) {
      AllowedValues allowedValues = new AllowedValues();

      for(Object allowedValue : allowedValuesList) {
         allowedValues.getValueOrRange().add(allowedValue);
      }

      this.literalInputType.setAllowedValues(allowedValues);
      return this;
   }

   public CodeType getIdentifier() {
      return this.identifier;
   }

   public LanguageStringType getTitle() {
      return this.title;
   }

   public LanguageStringType getAbstractValue() {
      return this.abstractValue;
   }

   public LiteralInputType getLiteralInputType() {
      return this.literalInputType;
   }

   public BigInteger getMinOccurs() {
      return this.minOccurs;
   }

   public BigInteger getMaxOccurs() {
      return this.maxOccurs;
   }
}
