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
import ro.cs.tao.component.enums.ParameterType;
import ro.cs.tao.component.template.BasicTemplate;
import ro.cs.tao.component.template.Template;
import ro.cs.tao.component.template.TemplateType;
import ro.cs.tao.eodata.enums.DataFormat;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class DemoBase {

    protected static <T> ParameterDescriptor newParameter(String name, String label, Class<T> clazz, T defaultValue, String description, T... values) {
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

    protected static SourceDescriptor newSourceDescriptor(String name, DataFormat format) {
        SourceDescriptor sourceDescriptor = new SourceDescriptor();
        sourceDescriptor.setId(UUID.randomUUID().toString());
        sourceDescriptor.setName(name);
        DataDescriptor sourceData = new DataDescriptor();
        sourceData.setFormatType(format);
        sourceDescriptor.setDataDescriptor(sourceData);
        return sourceDescriptor;
    }

    protected static TargetDescriptor newTargetDescriptor(String name, DataFormat format, String uriLocation) {
        TargetDescriptor targetDescriptor = new TargetDescriptor();
        targetDescriptor.setId(UUID.randomUUID().toString());
        targetDescriptor.setName(name);
        DataDescriptor targetData = new DataDescriptor();
        targetData.setFormatType(format);
        targetData.setLocation(uriLocation);
        targetDescriptor.setDataDescriptor(targetData);
        return targetDescriptor;
    }

    public static Template newTemplate(String name, String opName, ProcessingComponent component, String valueSeparator) {
        Template template = new BasicTemplate();
        template.setName(name);
        template.setTemplateType(TemplateType.VELOCITY);
        StringBuilder builder = new StringBuilder();
        if (opName != null) {
            builder.append(opName).append("\n");
        }
        List<SourceDescriptor> sources = component.getSources();
        if (sources != null) {
            for (SourceDescriptor srcDescriptor : sources) {
                builder.append("-").append(srcDescriptor.getName()).append(valueSeparator)
                        .append("$").append(srcDescriptor.getName()).append("\n");
            }
        }
        for (ParameterDescriptor parameter : component.getParameterDescriptors()) {
            builder.append("-").append(parameter.getLabel()).append(valueSeparator)
                    .append("$").append(parameter.getId()).append("\n");
        }
        List<TargetDescriptor> targets = component.getTargets();
        if (targets != null) {
            for (TargetDescriptor targetDescriptor : targets) {
                builder.append("-").append(targetDescriptor.getName()).append("\n")
                        //.append(Paths.get(URI.create(targetDescriptor.getDataDescriptor().getLocation())).toString());
                        .append("$").append(targetDescriptor.getName()).append("\n");
            }
        }
        if (opName != null) {
            builder.append("-f\nGeoTIFF\n");
        }
        template.setContents(builder.toString(), false);
        return template;
    }
}
