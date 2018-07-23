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
import ro.cs.tao.eodata.enums.DataFormat;

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
                "None",
                "If selected bands differ in size, the resample method used before computing the index",
                "None", "Lowest resolution", "Highest resolution"));
        parameters.add(newParameter("upsampling", "Pupsampling",
                String.class,
                "Nearest",
                "The method used for interpolation (upsampling to a finer resolution)",
                "Nearest", "Bilinear", "Bicubic"));
        SourceDescriptor sourceDescriptor = newSourceDescriptor("Ssource", DataFormat.RASTER, 1);

        ProcessingComponent component = new ProcessingComponent();
        component.setId("snap-s2rep");
        component.setLabel("SNAP S2Rep Index");
        component.setDescription("Sentinel-2 red-edge position index");
        component.setVersion("6.0.0");
        component.setAuthors("SNAP Team");
        component.setCopyright("(C) SNAP Team");
        component.setFileLocation("gpt");
        component.setWorkingDirectory(".");
        component.setNodeAffinity("Any");
        component.setVisibility(ProcessingComponentVisibility.SYSTEM);
        component.addSource(sourceDescriptor);
        TargetDescriptor targetDescriptor = newTargetDescriptor("t", DataFormat.RASTER,
                                                                "output_" + component.getId() + ".tif");
        component.addTarget(targetDescriptor);

        component.setParameterDescriptors(parameters);

        Template template = newTemplate("snap-s2rep.vm", "S2repOp", component, "=");

        component.setTemplateType(TemplateType.VELOCITY);
        component.setTemplate(template);
        component.setActive(true);
        return component;
    }

    public static ProcessingComponent msavi() {
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
                "The near-infrared band for the MSAVI computation. If not provided, the operator will try to find the best fitting band"));
        parameters.add(newParameter("redFactor", "PredFactor",
                Float.class,
                1f,
                "The value of the red source band is multiplied by this value"));
        parameters.add(newParameter("redSourceBand", "PredSourceBand",
                String.class,
                "B4",
                "The red band for the MSAVI computation. If not provided, the operator will try to find the best fitting band"));
        parameters.add(newParameter("slope", "Pslope",
                Float.class,
                0.5f,
                "The soil line slope"));
        parameters.add(newParameter("resampleType", "PresampleType",
                String.class,
                "None",
                "If selected bands differ in size, the resample method used before computing the index",
                "None", "Lowest resolution", "Highest resolution"));
        parameters.add(newParameter("upsampling", "Pupsampling",
                String.class,
                "Nearest",
                "The method used for interpolation (upsampling to a finer resolution)",
                "Nearest", "Bilinear", "Bicubic"));
        SourceDescriptor sourceDescriptor = newSourceDescriptor("Ssource", DataFormat.RASTER, 1);

        ProcessingComponent component = new ProcessingComponent();
        component.setId("snap-msavi");
        component.setLabel("SNAP MSAVI Index");
        component.setDescription("Retrieves the Modified Soil Adjusted Vegetation Index (MSAVI)");
        component.setVersion("6.0.0");
        component.setAuthors("SNAP Team");
        component.setCopyright("(C) SNAP Team");
        component.setFileLocation("gpt");
        component.setWorkingDirectory(".");
        component.setNodeAffinity("Any");
        component.setVisibility(ProcessingComponentVisibility.SYSTEM);
        component.addSource(sourceDescriptor);
        TargetDescriptor targetDescriptor = newTargetDescriptor("t", DataFormat.RASTER,
                                                                "output_" + component.getId() + ".tif");
        component.addTarget(targetDescriptor);

        component.setParameterDescriptors(parameters);

        Template template = newTemplate("snap-msavi.vm", "MsaviOp", component, "=");

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
        SourceDescriptor sourceDescriptor = newSourceDescriptor("Ssource", DataFormat.RASTER, 1);

        ProcessingComponent component = new ProcessingComponent();
        component.setId("snap-ndvi");
        component.setLabel("SNAP NDVI");
        component.setDescription("Computes Normalized Difference Vegetation Index");
        component.setVersion("6.0.0");
        component.setAuthors("SNAP Team");
        component.setCopyright("(C) SNAP Team");
        component.setFileLocation("gpt");
        component.setWorkingDirectory(".");
        component.setNodeAffinity("Any");
        component.setVisibility(ProcessingComponentVisibility.SYSTEM);
        component.addSource(sourceDescriptor);
        TargetDescriptor targetDescriptor = newTargetDescriptor("t", DataFormat.RASTER,
                                                                "output_" + component.getId() + ".tif");
        component.addTarget(targetDescriptor);

        component.setParameterDescriptors(parameters);

        Template template = newTemplate("snap-ndvi.vm", "NdviOp", component, "=");

        component.setTemplateType(TemplateType.VELOCITY);
        component.setTemplate(template);
        component.setActive(true);
        return component;
    }

    public static ProcessingComponent resample() {
        ArrayList<ParameterDescriptor> parameters = new ArrayList<>();
        parameters.add(newParameter("targetResolution", "PtargetResolution",
                Integer.class,
                60,
                "The resolution that all bands of the target product shall have. The same value will be applied to scale image widths and heights"));
        parameters.add(newParameter("downsampling", "Pdownsampling",
                String.class,
                "First",
                "The method used for aggregation (downsampling to a coarser resolution)",
                "First", "Min", "Max", "Mean", "Median"));
        parameters.add(newParameter("upsampling", "Pupsampling",
                String.class,
                "Nearest",
                "The method used for interpolation (upsampling to a finer resolution)",
                "Nearest", "Bilinear", "Bicubic"));
        SourceDescriptor sourceDescriptor = newSourceDescriptor("SsourceProduct", DataFormat.RASTER, 1);

        ProcessingComponent component = new ProcessingComponent();
        component.setId("snap-resample");
        component.setLabel("SNAP Resample");
        component.setDescription("Resampling of a multi-size source product to a single-size target product");
        component.setVersion("6.0.0");
        component.setAuthors("SNAP Team");
        component.setCopyright("(C) SNAP Team");
        component.setFileLocation("gpt");
        component.setWorkingDirectory(".");
        component.setNodeAffinity("Any");
        component.setVisibility(ProcessingComponentVisibility.SYSTEM);
        component.addSource(sourceDescriptor);
        TargetDescriptor targetDescriptor = newTargetDescriptor("t", DataFormat.RASTER,
                                                                "output_" + component.getId() + ".tif");
        component.addTarget(targetDescriptor);

        component.setParameterDescriptors(parameters);

        Template template = newTemplate("snap-resample.vm", "Resample", component, "=");

        component.setTemplateType(TemplateType.VELOCITY);
        component.setTemplate(template);
        component.setActive(true);
        return component;
    }
}
