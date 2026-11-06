package com.bc.wps.listener;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class ContainerInitAndDestroy implements ServletContextListener {
   public void contextInitialized(ServletContextEvent servletContextEvent) {
      System.out.println("*****************************************");
      System.out.println("********* Starting BC WPS server ********");
      System.out.println("*****************************************");
   }

   public void contextDestroyed(ServletContextEvent servletContextEvent) {
      System.out.println("********************************************");
      System.out.println("********* Stopping BC WPS server ***********");
      System.out.println("********************************************");
   }
}
