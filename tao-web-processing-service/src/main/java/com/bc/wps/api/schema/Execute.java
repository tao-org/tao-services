package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "",
   propOrder = {"identifier", "dataInputs", "responseForm"}
)
@XmlRootElement(
   name = "Execute"
)
public class Execute extends RequestBaseType {
   @XmlElement(
      name = "Identifier",
      namespace = "http://www.opengis.net/ows/1.1",
      required = true
   )
   protected CodeType identifier;
   @XmlElement(
      name = "DataInputs",
      namespace = "http://www.opengis.net/wps/1.0.0"
   )
   protected DataInputsType dataInputs;
   @XmlElement(
      name = "ResponseForm",
      namespace = "http://www.opengis.net/wps/1.0.0"
   )
   protected ResponseFormType responseForm;

   public CodeType getIdentifier() {
      return this.identifier;
   }

   public void setIdentifier(CodeType value) {
      this.identifier = value;
   }

   public DataInputsType getDataInputs() {
      return this.dataInputs;
   }

   public void setDataInputs(DataInputsType value) {
      this.dataInputs = value;
   }

   public ResponseFormType getResponseForm() {
      return this.responseForm;
   }

   public void setResponseForm(ResponseFormType value) {
      this.responseForm = value;
   }
}
