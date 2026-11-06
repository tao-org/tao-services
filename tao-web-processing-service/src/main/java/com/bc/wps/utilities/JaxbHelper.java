package com.bc.wps.utilities;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.io.StringWriter;

public class JaxbHelper {
   public static String marshal(Object object) throws JAXBException {
      return marshalWithSchemaLocation(object, (String)null);
   }

   public static String marshalWithSchemaLocation(Object object, String schemaLocation) throws JAXBException {
      StringWriter stringWriter = new StringWriter();
      Marshaller jaxbMarshaller = getMarshaller(object);
      jaxbMarshaller.setProperty("jaxb.formatted.output", true);
      if (schemaLocation != null) {
         jaxbMarshaller.setProperty("jaxb.schemaLocation", schemaLocation);
      }

      jaxbMarshaller.marshal(object, stringWriter);
      return stringWriter.toString();
   }

   private static Marshaller getMarshaller(Object object) throws JAXBException {
      JAXBContext jaxbContext = JAXBContext.newInstance(new Class[]{object.getClass()});
      return jaxbContext.createMarshaller();
   }

   public static Object unmarshal(InputStream inputStream, Object objectFactory) throws JAXBException {
      JAXBContext jaxbContext = JAXBContext.newInstance(new Class[]{objectFactory.getClass()});
      Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
      return jaxbUnmarshaller.unmarshal(inputStream);
   }
}
