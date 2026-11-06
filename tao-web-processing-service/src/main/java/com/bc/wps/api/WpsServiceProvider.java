package com.bc.wps.api;

public interface WpsServiceProvider {
   String getId();

   String getName();

   String getDescription();

   WpsServiceInstance createServiceInstance(WpsServerContext var1);
}
