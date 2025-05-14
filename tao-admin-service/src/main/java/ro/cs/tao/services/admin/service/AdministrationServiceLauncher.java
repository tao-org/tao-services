/*
 * Copyright (C) 2018 CS ROMANIA
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package ro.cs.tao.services.admin.service;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import ro.cs.tao.services.commons.ServiceLauncher;

/**
 * Configuration class responsible for initializing and launching the
 * User Administration Service within the application. This class is a
 * Spring Boot entry point that sets up the necessary context using
 * pre-defined XML configuration.
 *
 * The class implements the {@code ServiceLauncher} interface, providing
 * a unique name for the service it represents.
 *
 * Features:
 * - Loads additional Spring configuration defined in an external XML file.
 * - Implements the {@code serviceName()} method to provide the service's name.
 * - Defines the main method to bootstrap the Spring Boot application.
 *
 * Annotations:
 * - {@code @Configuration}: Indicates that this class contains one or more
 *   Spring bean definitions.
 * - {@code @ImportResource}: Loads an external XML Spring configuration file
 *   into the application context.
 */
@Configuration
@ImportResource("classpath:admin-service-context.xml")
public class AdministrationServiceLauncher implements ServiceLauncher {

    public static void main(String[] args) {
        SpringApplication.run(AdministrationServiceLauncher.class, args);
    }

    @Override
    public String serviceName() { return "User Administration Service"; }
}
