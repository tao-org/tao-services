package com.bc.wps.api.schema;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "",
   propOrder = {"planetaryGrid", "numRows", "compositingType", "superSampling", "maskExpr", "variables", "aggregators", "postProcessor", "minDataHour", "metadataAggregatorName", "startDateTime", "periodDuration", "timeFilterMethod", "outputFile"}
)
@XmlRootElement(
   name = "parameters",
   namespace = "http://www.brockmann-consult.de/bc-wps/calwpsL3Parameters-schema.xsd"
)
public class L3Parameters {
   @XmlElement(
      name = "planetaryGrid",
      defaultValue = "org.esa.beam.binning.support.SEAGrid",
      namespace = "http://www.brockmann-consult.de/bc-wps/calwpsL3Parameters-schema.xsd"
   )
   protected String planetaryGrid;
   @XmlElement(
      name = "numRows",
      defaultValue = "2160",
      namespace = "http://www.brockmann-consult.de/bc-wps/calwpsL3Parameters-schema.xsd"
   )
   protected int numRows;
   @XmlElement(
      name = "compositingType",
      namespace = "http://www.brockmann-consult.de/bc-wps/calwpsL3Parameters-schema.xsd"
   )
   protected CompositingType compositingType;
   @XmlElement(
      name = "superSampling",
      defaultValue = "1",
      namespace = "http://www.brockmann-consult.de/bc-wps/calwpsL3Parameters-schema.xsd"
   )
   protected Integer superSampling;
   @XmlElement(
      name = "maskExpr",
      namespace = "http://www.brockmann-consult.de/bc-wps/calwpsL3Parameters-schema.xsd"
   )
   protected String maskExpr;
   @XmlElement(
      name = "variables",
      namespace = "http://www.brockmann-consult.de/bc-wps/calwpsL3Parameters-schema.xsd"
   )
   protected Variables variables;
   @XmlElement(
      name = "aggregators",
      namespace = "http://www.brockmann-consult.de/bc-wps/calwpsL3Parameters-schema.xsd"
   )
   protected Aggregators aggregators;
   @XmlElement(
      name = "postProcessor",
      namespace = "http://www.brockmann-consult.de/bc-wps/calwpsL3Parameters-schema.xsd"
   )
   protected CellProcessorConfig postProcessor;
   @XmlElement(
      name = "minDataHour",
      namespace = "http://www.brockmann-consult.de/bc-wps/calwpsL3Parameters-schema.xsd"
   )
   protected Double minDataHour;
   @XmlElement(
      name = "metadataAggregatorName",
      defaultValue = "FIRST_HISTORY",
      namespace = "http://www.brockmann-consult.de/bc-wps/calwpsL3Parameters-schema.xsd"
   )
   protected String metadataAggregatorName;
   @XmlElement(
      name = "startDateTime",
      namespace = "http://www.brockmann-consult.de/bc-wps/calwpsL3Parameters-schema.xsd"
   )
   protected String startDateTime;
   @XmlElement(
      name = "periodDuration",
      namespace = "http://www.brockmann-consult.de/bc-wps/calwpsL3Parameters-schema.xsd"
   )
   protected Double periodDuration;
   @XmlElement(
      name = "timeFilterMethod",
      defaultValue = "NONE",
      namespace = "http://www.brockmann-consult.de/bc-wps/calwpsL3Parameters-schema.xsd"
   )
   protected TimeFilterMethod timeFilterMethod;
   @XmlElement(
      name = "outputFile",
      namespace = "http://www.brockmann-consult.de/bc-wps/calwpsL3Parameters-schema.xsd"
   )
   protected String outputFile;

   public String getPlanetaryGrid() {
      return this.planetaryGrid;
   }

   public void setPlanetaryGrid(String planetaryGrid) {
      this.planetaryGrid = planetaryGrid;
   }

   public int getNumRows() {
      return this.numRows;
   }

   public void setNumRows(int numRows) {
      this.numRows = numRows;
   }

   public CompositingType getCompositingType() {
      return this.compositingType;
   }

   public void setCompositingType(CompositingType compositingType) {
      this.compositingType = compositingType;
   }

   public Integer getSuperSampling() {
      return this.superSampling;
   }

   public void setSuperSampling(Integer superSampling) {
      this.superSampling = superSampling;
   }

   public String getMaskExpr() {
      return this.maskExpr;
   }

   public void setMaskExpr(String maskExpr) {
      this.maskExpr = maskExpr;
   }

   public Variables getVariables() {
      return this.variables;
   }

   public void setVariables(Variables variables) {
      this.variables = variables;
   }

   public Aggregators getAggregators() {
      return this.aggregators;
   }

   public void setAggregators(Aggregators aggregators) {
      this.aggregators = aggregators;
   }

   public CellProcessorConfig getPostProcessorConfig() {
      return this.postProcessor;
   }

   public void setPostProcessorConfig(CellProcessorConfig postProcessor) {
      this.postProcessor = postProcessor;
   }

   public Double getMinDataHour() {
      return this.minDataHour;
   }

   public void setMinDataHour(Double minDataHour) {
      this.minDataHour = minDataHour;
   }

   public String getMetadataAggregatorName() {
      return this.metadataAggregatorName;
   }

   public void setMetadataAggregatorName(String metadataAggregatorName) {
      this.metadataAggregatorName = metadataAggregatorName;
   }

   public String getStartDateTime() {
      return this.startDateTime;
   }

   public void setStartDateTime(String startDateTime) {
      this.startDateTime = startDateTime;
   }

   public Double getPeriodDuration() {
      return this.periodDuration;
   }

   public void setPeriodDuration(Double periodDuration) {
      this.periodDuration = periodDuration;
   }

   public TimeFilterMethod getTimeFilterMethod() {
      return this.timeFilterMethod;
   }

   public void setTimeFilterMethod(TimeFilterMethod timeFilterMethod) {
      this.timeFilterMethod = timeFilterMethod;
   }

   public String getOutputFile() {
      return this.outputFile;
   }

   public void setOutputFile(String outputFile) {
      this.outputFile = outputFile;
   }
}
