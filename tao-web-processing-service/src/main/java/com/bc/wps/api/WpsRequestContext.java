package com.bc.wps.api;

public interface WpsRequestContext {
   String getUserName();

   WpsServerContext getServerContext();

   String getHeaderField(String var1);
}
