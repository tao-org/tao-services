package com.bc.wps.utilities;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class UrlUtils {
   public static String parseParameter(String queryString, String parameterName) throws UnsupportedEncodingException {
      queryString = URLDecoder.decode(queryString, "UTF-8");
      if (queryString.startsWith("?")) {
         queryString = queryString.substring(1);
      }

      String[] keyValues = queryString.split("&");

      for(String keyValue : keyValues) {
         if (keyValue.startsWith(parameterName)) {
            String[] split = keyValue.split("=");
            return split[1];
         }
      }

      return "";
   }
}
