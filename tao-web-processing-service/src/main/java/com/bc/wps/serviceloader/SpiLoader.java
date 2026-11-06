package com.bc.wps.serviceloader;

import com.bc.wps.api.WpsServerContext;
import com.bc.wps.api.WpsServiceInstance;
import com.bc.wps.api.WpsServiceProvider;
import com.bc.wps.api.exceptions.WpsRuntimeException;
import java.util.ServiceLoader;

public class SpiLoader {
   public static WpsServiceInstance getWpsServiceProvider(WpsServerContext context, String applicationName) {
      for(WpsServiceProvider wpsProvider : ServiceLoader.load(WpsServiceProvider.class)) {
         if (wpsProvider.getId().equalsIgnoreCase(applicationName)) {
            return wpsProvider.createServiceInstance(context);
         }
      }

      throw new WpsRuntimeException("Application '" + applicationName + "' not found.");
   }
}
