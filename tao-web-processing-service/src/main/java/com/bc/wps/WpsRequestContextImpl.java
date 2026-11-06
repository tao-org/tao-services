package com.bc.wps;

import com.bc.wps.api.WpsRequestContext;
import com.bc.wps.api.WpsServerContext;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;

public class WpsRequestContextImpl implements WpsRequestContext {
   private final HttpServletRequest servletRequest;

   public WpsRequestContextImpl(HttpServletRequest servletRequest) {
      this.servletRequest = servletRequest;
   }

   public String getUserName() {
      String userName = this.servletRequest.getUserPrincipal().getName();
      return StringUtils.isBlank(userName) ? "" : userName;
   }

   public WpsServerContext getServerContext() {
      return new WpsServerContextImpl(this.servletRequest);
   }

   public String getHeaderField(String key) {
      return this.servletRequest.getHeader(key);
   }
}
