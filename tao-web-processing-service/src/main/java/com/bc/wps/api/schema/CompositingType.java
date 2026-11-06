package com.bc.wps.api.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
   name = "CompositingType",
   propOrder = {}
)
public enum CompositingType {
   BINNING,
   MOSAICKING;
}
