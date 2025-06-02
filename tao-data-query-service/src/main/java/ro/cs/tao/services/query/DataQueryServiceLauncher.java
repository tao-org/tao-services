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
package ro.cs.tao.services.query;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
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
@Configuration
@ImportResource("classpath:data-query-service-context.xml")

public class DataQueryServiceLauncher extends ServiceLauncher {

    public static void main(String[] args) {
        SpringApplication.run(DataQueryServiceLauncher.class, args);
    }

    public DataQueryServiceLauncher() {
        super();
    }

    @Override
    public String serviceName() { return "Query Service"; }

    @Override
    protected List<ServiceEntry> exportedEntries() {
        final List<ServiceEntry> entries = new ArrayList<>();
        entries.add(new ServiceEntry("Search", "my/explorer", "./fragments/datasources-queries.fragment.html" , new Visibility[] { Visibility.USER, Visibility.ADMIN }, Section.DATASETS, 1));
        entries.add(new ServiceEntry("Product Sets", "my/queries", "./fragments/datasources-queries.fragment.html", new Visibility[] { Visibility.USER, Visibility.ADMIN }, Section.DATASETS, 4));
        entries.add(new ServiceEntry("Data Sources", "admin/datasources", "./fragments/datasource-admin.fragment.html", new Visibility[] { Visibility.ADMIN }, Section.ADMINISTRATION, 6));
        return entries;
    }
}