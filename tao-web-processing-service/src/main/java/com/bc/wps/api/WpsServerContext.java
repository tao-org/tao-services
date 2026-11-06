package com.bc.wps.api;

public interface WpsServerContext {
   String getRequestUrl();

   String getHostAddress();

   int getPort();
}
