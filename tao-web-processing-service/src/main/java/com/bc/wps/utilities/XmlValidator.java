package com.bc.wps.utilities;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import sax.Counter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;

public class XmlValidator {
   public static void validate(File xmlFile) {
      try {
         Counter counter = new Counter();
         XMLReader parser = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
         parser.setFeature("http://apache.org/xml/features/validation/schema-full-checking", true);
         parser.setFeature("http://xml.org/sax/features/namespaces", true);
         parser.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
         parser.setFeature("http://xml.org/sax/features/validation", true);
         parser.setFeature("http://apache.org/xml/features/validation/schema", true);
         parser.setFeature("http://apache.org/xml/features/honour-all-schemaLocations", true);
         parser.setErrorHandler(counter);
         parser.setContentHandler(counter);
         parser.parse(new InputSource(new FileInputStream(xmlFile)));
      } catch (IOException | SAXException exception) {
         System.out.println("Error in parsing the xml : " + ((Exception)exception).getMessage());
      }

   }

   public static void validateString(String xmlString) {
      try {
         Counter counter = new Counter();
         XMLReader parser = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
         parser.setFeature("http://apache.org/xml/features/validation/schema-full-checking", true);
         parser.setFeature("http://xml.org/sax/features/namespaces", true);
         parser.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
         parser.setFeature("http://xml.org/sax/features/validation", true);
         parser.setFeature("http://apache.org/xml/features/validation/schema", true);
         parser.setFeature("http://apache.org/xml/features/honour-all-schemaLocations", true);
         parser.setErrorHandler(counter);
         parser.setContentHandler(counter);
         parser.parse(new InputSource(new StringReader(xmlString)));
      } catch (IOException | SAXException exception) {
         System.out.println("Error in parsing the xml : " + ((Exception)exception).getMessage());
      }

   }
}
