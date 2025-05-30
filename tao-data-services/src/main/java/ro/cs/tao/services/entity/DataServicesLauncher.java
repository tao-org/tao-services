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
        entries.add(new ServiceEntry("Sites", "my/sites", "./fragments/sites.fragment.html", Visibility.USER, Section.DATASETS, 2));
        entries.add(new ServiceEntry("Shared Datasets", "my/sharedDatasets", "./fragments/shared-datasets.fragment.html", Visibility.USER, Section.DATASETS, 5));
        entries.add(new ServiceEntry("Workflows", "my/workflows", "./fragments/workflows-admin2.fragment.html", Visibility.USER, Section.WORKFLOWS, 1));
        entries.add(new ServiceEntry("Components", "my/components", "./fragments/components-user.fragment.html", Visibility.USER, Section.COMPONENTS, 1));
        entries.add(new ServiceEntry("Containers", "my/containers", "./fragments/containers-user.fragment.html", Visibility.USER, Section.COMPONENTS, 2));
        entries.add(new ServiceEntry("Toolbox Containers", "admin/containers" ,"./fragments/containers-admin2.fragment.html", Visibility.ADMIN, Section.ADMINISTRATION, 2));
        entries.add(new ServiceEntry("Processing Modules", "admin/components", "./fragments/component-admin2.fragment.html", Visibility.ADMIN, Section.ADMINISTRATION, 3));
        entries.add(new ServiceEntry("Configuration", "admin/config", "./fragments/configuration.fragment.html", Visibility.ADMIN, Section.ADMINISTRATION, 4));
        entries.add(new ServiceEntry("Topology", "admin/topology", "./fragments/topology-admin2.fragment.html", Visibility.ADMIN, Section.ADMINISTRATION, 8));
        return entries;
    }
}
