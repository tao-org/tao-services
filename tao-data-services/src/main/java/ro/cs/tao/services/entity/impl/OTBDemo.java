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

package ro.cs.tao.services.entity.impl;

import ro.cs.tao.component.*;
import ro.cs.tao.component.enums.ProcessingComponentVisibility;
import ro.cs.tao.component.template.BasicTemplate;
import ro.cs.tao.component.template.Template;
import ro.cs.tao.component.template.TemplateType;
import ro.cs.tao.eodata.enums.DataFormat;

import java.net.URI;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class OTBDemo {

    public static ProcessingComponent rigidTransform() {
        ArrayList<ParameterDescriptor> parameters = new ArrayList<>();
        parameters.add(newParameter("transformType", "transform.type",
                                    String.class,
                                    "id",
                                    "Type of transformation [id/translation/rotation]",
                                    "id", "translation", "rotation"));
        parameters.add(newParameter("transformTypeIdScaleX", "transform.type.id.scalex",
                                    Float.class,
                                    1f,
                                    "X scaling"));
        parameters.add(newParameter("transformTypeIdScaleY", "transform.type.id.scaley",
                                    Float.class,
                                    1f,
                                    "Y scaling"));
        parameters.add(newParameter("transformTypeTranslationTX", "transform.type.translation.tx",
                                    Float.class,
                                    0f,
                                    "The X translation (in physical units)"));
        parameters.add(newParameter("transformTypeTranslationTY", "transform.type.translation.ty",
                                    Float.class,
                                    0f,
                                    "The Y translation (in physical units)"));
        parameters.add(newParameter("transformTypeTranslationScaleX", "transform.type.translation.scalex",
                                    Float.class,
                                    1f,
                                    "X scaling"));
        parameters.add(newParameter("transformTypeTranslationScaleY", "transform.type.translation.scaley",
                                    Float.class,
                                    1f,
                                    "Y scaling"));
        parameters.add(newParameter("transformTypeRotationAngle", "transform.type.rotation.angle",
                                    Float.class,
                                    0f,
                                    "Rotation angle"));
        parameters.add(newParameter("transformTypeRotationScaleX", "transform.type.rotation.scalex",
                                    Float.class,
                                    1f,
                                    "X scaling"));
        parameters.add(newParameter("transformTypeRotationScaleY", "transform.type.rotation.scaley",
                                    Float.class,
                                    1f,
                                    "Y scaling"));
        parameters.add(newParameter("interpolator", "interpolator",
                                    String.class,
                                    "bco",
                                    "Interpolation [nn/linear/bco]",
                                    "nn", "linear", "bco"));
        parameters.add(newParameter("interpolatorBcoRadius", "interpolator.bco.radius",
                                    Integer.class,
                                    2,
                                    "Radius for bicubic interpolation"));
        SourceDescriptor sourceDescriptor = new SourceDescriptor();
        sourceDescriptor.setId(UUID.randomUUID().toString());
        sourceDescriptor.setName("in");
        DataDescriptor sourceData = new DataDescriptor();
        sourceData.setFormatType(DataFormat.RASTER);
        sourceDescriptor.setDataDescriptor(sourceData);

        Set<Variable> variables = new HashSet<>();
        variables.add(new Variable("ITK_AUTOLOAD_PATH", "C:\\Tools\\OTB-6.4.0\\bin"));

        ProcessingComponent component = new ProcessingComponent();
        component.setId("otb-rigid-transform");
        component.setLabel("OTB Rigid Transform Resample");
        component.setDescription("Resamples an image with a rigid transform");
        component.setVersion("6.4.0");
        component.setAuthors("OTB Team");
        component.setCopyright("(C) OTB Team");
        component.setFileLocation("C:\\Tools\\OTB-6.4.0\\bin\\otbcli_RigidTransformResample.bat");
        component.setWorkingDirectory(".");
        component.setNodeAffinity("Any");
        component.setContainerId("DummyTestDockerContainer");
        component.setVisibility(ProcessingComponentVisibility.SYSTEM);
        component.addSource(sourceDescriptor);

        TargetDescriptor targetDescriptor = new TargetDescriptor();
        targetDescriptor.setId(UUID.randomUUID().toString());
        targetDescriptor.setName("out");
        DataDescriptor targetData = new DataDescriptor();
        targetData.setFormatType(DataFormat.RASTER);
        targetData.setLocation("file:///D:/img/out/output_" + component.getId() + ".tif");
        targetDescriptor.setDataDescriptor(targetData);

        component.addTarget(targetDescriptor);

        Template template = new BasicTemplate();
        template.setName("otb-rigid-transform.vm");
        template.setTemplateType(TemplateType.VELOCITY);
        StringBuilder builder = new StringBuilder();
        builder.append("-").append(sourceDescriptor.getName()).append("\n")
                .append("$").append(sourceDescriptor.getName()).append("\n");
        for (ParameterDescriptor parameter : parameters) {
            builder.append("-").append(parameter.getLabel()).append("\n")
                    .append("$").append(parameter.getId()).append("\n");
        }
        builder.append("-").append(targetDescriptor.getName()).append("\n")
                .append(Paths.get(URI.create(targetData.getLocation())).toString());
        template.setContents(builder.toString(), false);

        component.setParameterDescriptors(parameters);
        component.setVariables(variables);
        component.setTemplateType(TemplateType.VELOCITY);
        component.setTemplate(template);
        component.setActive(true);
        return component;
    }

    public static ProcessingComponent radiometricIndices() {
        ArrayList<ParameterDescriptor> parameters = new ArrayList<>();
        parameters.add(newParameter("channelsBlue", "channels.blue", Integer.class, 1, "Blue Channel"));
        parameters.add(newParameter("channelsGreen", "channels.green", Integer.class, 1, "Green Channel"));
        parameters.add(newParameter("channelsRed", "channels.red", Integer.class, 1, "Red Channel"));
        parameters.add(newParameter("channelsNir", "channels.nir", Integer.class, 1, "NIR Channel"));
        parameters.add(newParameter("channelsMir", "channels.mir", Integer.class, 1, "MIR Channel"));
        parameters.add(newParameter("list", "list", String.class, "Vegetation:NDVI", "Available Radiometric Indices",
                "Vegetation:NDVI", "Vegetation:TNDVI", "Vegetation:RVI", "Vegetation:SAVI", "Vegetation:TSAVI",
                "Vegetation:MSAVI", "Vegetation:MSAVI2", "Vegetation:GEMI", "Vegetation:IPVI", "Vegetation:LAIFromNDVILog",
                "Vegetation:LAIFromReflLinear", "Vegetation:LAIFromNDVIFormo", "Water:NDWI", "Water:NDWI2",
                "Water:MNDWI", "Water:NDPI", "Water:NDTI", "Soil:RI", "Soil:CI", "Soil:BI", "Soil:BI"));

        SourceDescriptor sourceDescriptor = new SourceDescriptor();
        sourceDescriptor.setId(UUID.randomUUID().toString());
        sourceDescriptor.setName("in");
        DataDescriptor sourceData = new DataDescriptor();
        sourceData.setFormatType(DataFormat.RASTER);
        sourceDescriptor.setDataDescriptor(sourceData);

        Set<Variable> variables = new HashSet<>();
        variables.add(new Variable("ITK_AUTOLOAD_PATH", "C:\\Tools\\OTB-6.4.0\\bin"));

        ProcessingComponent component = new ProcessingComponent();
        component.setId("otb-radiometric-indices");
        component.setLabel("OTB Radiometric Indies");
        component.setDescription("Computes radiometric indices");
        component.setVersion("6.4.0");
        component.setAuthors("OTB Team");
        component.setCopyright("(C) OTB Team");
        component.setFileLocation("C:\\Tools\\OTB-6.4.0\\bin\\otbcli_RadiometricIndices.bat");
        component.setWorkingDirectory(".");
        component.setNodeAffinity("Any");
        component.setContainerId("DummyTestDockerContainer");
        component.setVisibility(ProcessingComponentVisibility.SYSTEM);
        component.addSource(sourceDescriptor);

        TargetDescriptor targetDescriptor = new TargetDescriptor();
        targetDescriptor.setId(UUID.randomUUID().toString());
        targetDescriptor.setName("out");
        DataDescriptor targetData = new DataDescriptor();
        targetData.setFormatType(DataFormat.RASTER);
        targetData.setLocation("file:///D:/img/out/output_" + component.getId() + ".tif");
        targetDescriptor.setDataDescriptor(targetData);
        component.addTarget(targetDescriptor);

        Template template = new BasicTemplate();
        template.setName("otb-radiometric-indices.vm");
        template.setTemplateType(TemplateType.VELOCITY);
        StringBuilder builder = new StringBuilder();
        builder.append("-").append(sourceDescriptor.getName()).append("\n")
                .append("$").append(sourceDescriptor.getName()).append("\n");
        for (ParameterDescriptor parameter : parameters) {
            builder.append("-").append(parameter.getLabel()).append("\n")
                    .append("$").append(parameter.getId()).append("\n");
        }
        builder.append("-").append(targetDescriptor.getName()).append("\n")
                .append(Paths.get(URI.create(targetData.getLocation())).toString());
        template.setContents(builder.toString(), false);

        component.setParameterDescriptors(parameters);
        component.setVariables(variables);
        component.setTemplateType(TemplateType.VELOCITY);
        component.setTemplate(template);
        component.setActive(true);
        return component;
    }

    private static <T> ParameterDescriptor newParameter(String name, String label, Class<T> clazz, T defaultValue, String description, T... values) {
        ParameterDescriptor ret = new ParameterDescriptor(name);
        ret.setType(ParameterType.REGULAR);
        ret.setDataType(clazz);
        ret.setDefaultValue(String.valueOf(defaultValue));
        ret.setDescription(description);
        ret.setLabel(label);
        if (values != null) {
            String[] set = new String[values.length];
            Arrays.stream(values).map(String::valueOf).collect(Collectors.toList()).toArray(set);
            ret.setValueSet(set);
        }
        return ret;
    }
}
