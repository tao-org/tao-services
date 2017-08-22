/*
 *
 *  * Copyright (C) 2017 CS ROMANIA
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *  *
 *
 */

/*
 *
 *  * Copyright (C) 2017 CS ROMANIA
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *  *
 *
 */

import ro.cs.tao.datasource.common.DataQuery;
import ro.cs.tao.datasource.common.DataSource;
import ro.cs.tao.datasource.common.QueryException;
import ro.cs.tao.datasource.common.parameter.QueryParameter;
import ro.cs.tao.datasource.remote.scihub.SciHubDataQuery;
import ro.cs.tao.datasource.remote.scihub.SciHubDataSource;
import ro.cs.tao.datasource.remote.scihub.SentinelDownloader;
import ro.cs.tao.datasource.util.Polygon2D;
import ro.cs.tao.eodata.EOData;
import ro.cs.tao.spi.ServiceRegistry;
import ro.cs.tao.spi.ServiceRegistryManager;

import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * @author Cosmin Cara
 */
public class DataSourceTest {

    public static void main(String[] args) {
        //SciHub_Sentinel1_Test();

        SciHub_Sentinel2_Test();
    }

    public static void SciHub_Sentinel2_Test() {
        try {
            ServiceRegistry<DataSource> serviceRegistry =
                    ServiceRegistryManager.getInstance().getServiceRegistry(DataSource.class);
            Logger logger = LogManager.getLogManager().getLogger("");
            for (Handler handler : logger.getHandlers()) {
                handler.setLevel(Level.INFO);
            }
            DataSource<EOData, SciHubDataQuery> dataSource =
                    serviceRegistry.getService(SciHubDataSource.class.getName());
                    //new SciHubDataSource();
            dataSource.setCredentials("kraftek", "cei7pitici.");

            DataQuery<EOData> query = dataSource.createQuery();
            query.addParameter("platformName", "Sentinel-2");
            QueryParameter begin = query.createParameter("beginPosition", Date.class);
            begin.setMinValue(Date.from(LocalDateTime.of(2016, 2, 1, 0, 0, 0, 0)
                                                .atZone(ZoneId.systemDefault())
                                                .toInstant()));
            begin.setMaxValue(Date.from(LocalDateTime.of(2017, 2, 1, 0, 0, 0, 0)
                                                .atZone(ZoneId.systemDefault())
                                                .toInstant()));
            query.addParameter(begin);
            Polygon2D aoi = Polygon2D.fromWKT("POLYGON((22.8042573604346 43.8379609098684," +
                                                      "24.83885442747927 43.8379609098684," +
                                                      "24.83885442747927 44.795645304033826," +
                                                      "22.8042573604346 44.795645304033826," +
                                                      "22.8042573604346 43.8379609098684))");

            query.addParameter("footprint", aoi);

            query.addParameter("cloudcoverpercentage", 100.);
            query.setPageSize(50);
            query.setMaxResults(83);
            List<EOData> results = query.execute();
            results.forEach(r -> {
                System.out.println("ID=" + r.getId());
                System.out.println("NAME=" + r.getName());
                System.out.println("LOCATION=" + r.getLocation().toString());
                System.out.println("FOOTPRINT=" + r.getGeometry().toText());
                System.out.println("Attributes ->");
                Arrays.stream(r.getAttributes())
                        .forEach(a -> System.out.println("\tName='" + a.getName() +
                                                                 "', value='" + a.getValue() + "'"));
            });
        } catch (QueryException e) {
            e.printStackTrace();
        }
    }

    public static void SciHub_Sentinel1_Test() {
        try {
            Logger logger = LogManager.getLogManager().getLogger("");
            for (Handler handler : logger.getHandlers()) {
                handler.setLevel(Level.INFO);
            }
            DataSource<EOData, SciHubDataQuery> dataSource = new SciHubDataSource();
            dataSource.setCredentials("kraftek", "cei7pitici.");

            DataQuery<EOData> query = dataSource.createQuery();
            query.addParameter("platformName", "Sentinel-1");
            QueryParameter begin = query.createParameter("beginPosition", Date.class);
            begin.setMinValue(Date.from(LocalDateTime.of(2017, 5, 30, 0, 0, 0, 0)
                                                .atZone(ZoneId.systemDefault())
                                                .toInstant()));
            begin.setMaxValue(Date.from(LocalDateTime.of(2017, 6, 1, 0, 0, 0, 0)
                                                .atZone(ZoneId.systemDefault())
                                                .toInstant()));
            query.addParameter(begin);
            query.addParameter("polarisationMode", "VV");
            query.addParameter("sensorOperationalMode", "IW");
            query.addParameter("productType", "SLC");
            query.setPageSize(50);
            query.setMaxResults(83);
            SentinelDownloader downloader = new SentinelDownloader("E:\\NewFormat");
            List<EOData> results = query.execute();
            //downloader.download(results);
            results.forEach(r -> {
                System.out.println("ID=" + r.getId());
                System.out.println("NAME=" + r.getName());
                System.out.println("LOCATION=" + r.getLocation().toString());
                System.out.println("FOOTPRINT=" + r.getGeometry().toText());
                System.out.println("Attributes ->");
                Arrays.stream(r.getAttributes())
                        .forEach(a -> System.out.println("\tName='" + a.getName() +
                                                                 "', value='" + a.getValue() + "'"));
            });
        } catch (URISyntaxException | QueryException e) {
            e.printStackTrace();
        }
    }
}
