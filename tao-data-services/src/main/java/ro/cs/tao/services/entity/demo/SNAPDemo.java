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

import ro.cs.tao.component.*;
import ro.cs.tao.component.enums.ProcessingComponentVisibility;
import ro.cs.tao.component.template.Template;
import ro.cs.tao.component.template.TemplateType;
import ro.cs.tao.eodata.enums.DataFormat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

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
        SourceDescriptor sourceDescriptor = newSourceDescriptor("in", DataFormat.RASTER);

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

        TargetDescriptor targetDescriptor = newTargetDescriptor("out", DataFormat.RASTER,
                "file:///D:/img/out/output_" + component.getId() + ".tif");
        component.addTarget(targetDescriptor);

        Template template = newTemplate("otb-rigid-transform.vm", component);

        component.setParameterDescriptors(parameters);
        component.setVariables(variables);
        component.setTemplateType(TemplateType.VELOCITY);
        component.setTemplate(template);
        component.setActive(true);
        return component;
    }

}
