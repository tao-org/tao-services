package com.bc.wps;

import com.bc.wps.api.WpsServerContext;
import javax.servlet.http.HttpServletRequest;

public class WpsServerContextImpl implements WpsServerContext {
   private final HttpServletRequest servletRequest;

   public WpsServerContextImpl(HttpServletRequest servletRequest) {
      this.servletRequest = servletRequest;
   }

   public String getRequestUrl() {
      return this.servletRequest.getRequestURL().toString();
   }

   public String getHostAddress() {
      return this.servletRequest.getServerName();
   }

   public int getPort() {
      return this.servletRequest.getServerPort();
   }
}
