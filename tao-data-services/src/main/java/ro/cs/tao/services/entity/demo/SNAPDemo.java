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
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.eodata.enums.DataFormat;

import java.nio.file.Paths;
import java.util.ArrayList;

public class SNAPDemo extends DemoBase {

    public static ProcessingComponent s2rep() {
        ArrayList<ParameterDescriptor> parameters = new ArrayList<>();
        parameters.add(newParameter("downsampling", "Pdownsampling",
                String.class,
                "First",
                "The method used for aggregation (downsampling to a coarser resolution)",
                "First", "Min", "Max", "Mean", "Median"));
        parameters.add(newParameter("nirFactor", "PnirFactor",
                Float.class,
                1f,
                "The value of the NIR source band is multiplied by this value"));
        parameters.add(newParameter("nirSourceBand", "PnirSourceBand",
                String.class,
                "B8",
                "The near-infrared band for the S2REP computation. If not provided, the operator will try to find the best fitting band"));
        parameters.add(newParameter("redB4Factor", "PredB4Factor",
                Float.class,
                1f,
                "The value of the red source band (B4) is multiplied by this value"));
        parameters.add(newParameter("redB5Factor", "PredB5Factor",
                Float.class,
                1f,
                "The value of the red source band (B5) is multiplied by this value"));
        parameters.add(newParameter("redB6Factor", "PredB6Factor",
                Float.class,
                1f,
                "The value of the red source band (B6) is multiplied by this value"));
        parameters.add(newParameter("redSourceBand4", "PredSourceBand4",
                String.class,
                "B4",
                "The red band (B4) for the S2REP computation. If not provided, the operator will try to find the best fitting band"));
        parameters.add(newParameter("redSourceBand5", "PredSourceBand5",
                String.class,
                "B5",
                "The red band (B5) for the S2REP computation. If not provided, the operator will try to find the best fitting band"));
        parameters.add(newParameter("redSourceBand6", "PredSourceBand6",
                String.class,
                "B6",
                "The red band (B6) for the S2REP computation. If not provided, the operator will try to find the best fitting band"));
        parameters.add(newParameter("resampleType", "PresampleType",
                String.class,
                "Lowest resolution",
                "If selected bands differ in size, the resample method used before computing the index",
                "None", "Lowest resolution", "Highest resolution"));
        parameters.add(newParameter("upsampling", "Pupsampling",
                String.class,
                "Nearest",
                "The method used for interpolation (upsampling to a finer resolution)",
                "Nearest", "Bilinear", "Bicubic"));
        SourceDescriptor sourceDescriptor = newSourceDescriptor("Ssource", DataFormat.RASTER);

        ProcessingComponent component = new ProcessingComponent();
        component.setId("snap-s2rep");
        component.setLabel("Sentinel-2 red-edge position index");
        component.setDescription("Sentinel-2 red-edge position index");
        component.setVersion("6.0.0");
        component.setAuthors("SNAP Team");
        component.setCopyright("(C) SNAP Team");
        component.setFileLocation("gpt.exe");
        component.setWorkingDirectory(".");
        component.setNodeAffinity("Any");
        component.setVisibility(ProcessingComponentVisibility.SYSTEM);
        component.addSource(sourceDescriptor);
        String rootPath = ConfigurationManager.getInstance().getValue("product.location");
        TargetDescriptor targetDescriptor = newTargetDescriptor("t", DataFormat.RASTER,
                Paths.get(rootPath).resolve("output_" + component.getId() + ".tiff").toUri().toString());
        component.addTarget(targetDescriptor);

        component.setParameterDescriptors(parameters);

        Template template = newTemplate("snap-s2rep.vm", "S2repOp", component, "=");

        component.setTemplateType(TemplateType.VELOCITY);
        component.setTemplate(template);
        component.setActive(true);
        return component;
    }

    public static ProcessingComponent ndvi() {
        ArrayList<ParameterDescriptor> parameters = new ArrayList<>();
        parameters.add(newParameter("nirFactor", "PnirFactor",
                Float.class,
                1f,
                "The value of the NIR source band is multiplied by this value"));
        parameters.add(newParameter("nirSourceBand", "PnirSourceBand",
                String.class,
                "B8",
                "The near-infrared band for the NDVI computation. If not provided, the operator will try to find the best fitting band"));
        parameters.add(newParameter("redFactor", "PredFactor",
                Float.class,
                1f,
                "The value of the red source band is multiplied by this value"));
        parameters.add(newParameter("redSourceBand", "PredSourceBand",
                String.class,
                "B4",
                "The red band for the NDVI computation. If not provided, the operator will try to find the best fitting band"));
        SourceDescriptor sourceDescriptor = newSourceDescriptor("Ssource", DataFormat.RASTER);

        ProcessingComponent component = new ProcessingComponent();
        component.setId("snap-ndvi");
        component.setLabel("Normalized Difference Vegetation Index");
        component.setDescription("Normalized Difference Vegetation Index");
        component.setVersion("6.0.0");
        component.setAuthors("SNAP Team");
        component.setCopyright("(C) SNAP Team");
        component.setFileLocation("gpt.exe");
        component.setWorkingDirectory(".");
        component.setNodeAffinity("Any");
        component.setVisibility(ProcessingComponentVisibility.SYSTEM);
        component.addSource(sourceDescriptor);
        String rootPath = ConfigurationManager.getInstance().getValue("product.location");
        TargetDescriptor targetDescriptor = newTargetDescriptor("t", DataFormat.RASTER,
                Paths.get(rootPath).resolve("output_" + component.getId() + ".tiff").toUri().toString());
        component.addTarget(targetDescriptor);

        component.setParameterDescriptors(parameters);

        Template template = newTemplate("snap-ndvi.vm", "NdviOp", component, "=");

        component.setTemplateType(TemplateType.VELOCITY);
        component.setTemplate(template);
        component.setActive(true);
        return component;
    }
}
