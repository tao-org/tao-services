/*
 * Copyright (C) 2017 CS ROMANIA
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

package ro.cs.tao.services.entity.demo;

import ro.cs.tao.component.ParameterDescriptor;
import ro.cs.tao.component.ProcessingComponent;
import ro.cs.tao.component.SourceDescriptor;
import ro.cs.tao.component.TargetDescriptor;
import ro.cs.tao.component.enums.ProcessingComponentVisibility;
import ro.cs.tao.component.template.Template;
import ro.cs.tao.component.template.TemplateType;
import ro.cs.tao.docker.Container;
import ro.cs.tao.eodata.enums.DataFormat;

import java.util.ArrayList;

public class GDALDemo extends DemoBase {
    private static final String GDAL_COPYRIGHT = "(C) Open Source Geospatial Foundation";
    private static final String GDAL_AUTHORS = "GDAL Development Team";
    private static final String GDAL_VERSION = "2.2.1";

    public static ProcessingComponent gdalTranslate(Container container) {
        ArrayList<ParameterDescriptor> parameters = new ArrayList<>();
        parameters.add(newParameter("format", "of",
                                    String.class,
                                    "GTiff",
                                    "The output format",
                                    "VRT", "GTiff", "NITF", "HFA", "ELAS", "PNG", "JPEG", "GIF", "XPM", "BMP", "PCRaster",
                                    "ILWIS", "SGI", "SRTMHGT", "GMT", "netCDF", "HDF4Image", "JP2OpenJPEG", "FIT", "RMF",
                                    "RST", "INGR", "MRF", "PNM", "ENVI", "Ehdr"));
        parameters.add(newParameter("resampling", "r",
                                    String.class,
                                    "nearest",
                                    "Resampling algorithm",
                                    "nearest", "bilinear", "cubic", "cubicspline", "lanczos", "average", "mode"));
        parameters.add(newParameter("noDataValue", "a_nodata",
                                    Integer.class,
                                    0,
                                    "No data value"));
        SourceDescriptor sourceDescriptor = newSourceDescriptor("src", DataFormat.RASTER);

        ProcessingComponent component = new ProcessingComponent();
        component.setId("gdal_translate.exe");
        component.setLabel("GDAL Translate");
        component.setDescription("Converts raster data between different formats");
        component.setVersion(GDAL_VERSION);
        component.setAuthors(GDAL_AUTHORS);
        component.setCopyright(GDAL_COPYRIGHT);
        component.setFileLocation(container.getApplications().stream().filter(a -> component.getId().equals(a.getName())).findFirst().get().getPath());
        component.setWorkingDirectory(".");
        component.setNodeAffinity("Any");
        component.setVisibility(ProcessingComponentVisibility.SYSTEM);
        component.addSource(sourceDescriptor);
        //String rootPath = ConfigurationManager.getInstance().getValue("product.location");
        TargetDescriptor targetDescriptor = newTargetDescriptor("dst", DataFormat.RASTER, "output_" + component.getId());
        component.addTarget(targetDescriptor);

        component.setParameterDescriptors(parameters);
        Template template = newTemplate("gdal_translate.vm", null, component, "\n");

        component.setTemplateType(TemplateType.VELOCITY);
        component.setTemplate(template);
        component.setActive(true);
        component.setContainerId(container.getId());
        return component;
    }
}
