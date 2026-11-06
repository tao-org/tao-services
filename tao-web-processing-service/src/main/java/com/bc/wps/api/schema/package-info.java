@XmlSchema(
   namespace = "http://www.opengis.net/wps/1.0.0",
   elementFormDefault = XmlNsForm.UNQUALIFIED,
   xmlns = {@XmlNs(
   prefix = "wps",
   namespaceURI = "http://www.opengis.net/wps/1.0.0"
), @XmlNs(
   prefix = "ows",
   namespaceURI = "http://www.opengis.net/ows/1.1"
), @XmlNs(
   prefix = "ns1",
   namespaceURI = "http://www.w3.org/2001/XMLSchema"
), @XmlNs(
   prefix = "ns2",
   namespaceURI = "http://www.w3.org/2001/XMLSchema-instance"
), @XmlNs(
   prefix = "bc",
   namespaceURI = "http://www.brockmann-consult.de/bc-wps/calwpsL3Parameters-schema.xsd"
), @XmlNs(
   prefix = "xlink",
   namespaceURI = "http://www.w3.org/1999/xlink"
)}
)
package com.bc.wps.api.schema;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
