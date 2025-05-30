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
package ro.cs.tao.services.entity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;
import ro.cs.tao.services.commons.ServiceLauncher;
import ro.cs.tao.services.commons.menu.Section;
import ro.cs.tao.services.commons.menu.ServiceEntry;
import ro.cs.tao.services.commons.menu.Visibility;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Cosmin Cara
 */
@SpringBootApplication()
@ImportResource({"classpath:tao-data-services-context.xml", "classpath*:tao-persistence-context.xml" })
public class DataServicesLauncher extends ServiceLauncher {

    public static void main(String[] args) {
        SpringApplication.run(DataServicesLauncher.class, args);
    }

    public DataServicesLauncher() {
        super();
    }

    @Override
    public String serviceName() { return "Data Entities Service"; }

    @Override
    protected List<ServiceEntry> exportedEntries() {
        final List<ServiceEntry> entries = new ArrayList<>();
        entries.add(new ServiceEntry("Sites", Visibility.USER, Section.DATASETS, 2));
        entries.add(new ServiceEntry("Shared Datasets", Visibility.USER, Section.DATASETS, 5));
        entries.add(new ServiceEntry("Workflows", Visibility.USER, Section.WORKFLOWS, 1));
        entries.add(new ServiceEntry("Components", Visibility.USER, Section.COMPONENTS, 1));
        entries.add(new ServiceEntry("Containers", Visibility.USER, Section.COMPONENTS, 2));
        entries.add(new ServiceEntry("Toolbox Containers", Visibility.ADMIN, Section.ADMINISTRATION, 2));
        entries.add(new ServiceEntry("Processing Modules", Visibility.ADMIN, Section.ADMINISTRATION, 3));
        entries.add(new ServiceEntry("Configuration", Visibility.ADMIN, Section.ADMINISTRATION, 4));
        entries.add(new ServiceEntry("Topology", Visibility.ADMIN, Section.ADMINISTRATION, 8));
        return entries;
    }
}
