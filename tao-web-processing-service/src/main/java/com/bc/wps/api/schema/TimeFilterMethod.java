package com.bc.wps.api.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "TimeFilterMethod",
   propOrder = {}
)
public enum TimeFilterMethod {
   NONE,
   TIME_RANGE,
   SPATIOTEMPORAL_DATA_DAY;
}
