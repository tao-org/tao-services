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

package ro.cs.tao.component;

import ro.cs.tao.component.template.Template;
import ro.cs.tao.component.template.TemplateException;
import ro.cs.tao.component.template.TemplateType;
import ro.cs.tao.component.template.engine.EngineFactory;
import ro.cs.tao.component.template.engine.TemplateEngine;
import ro.cs.tao.component.validation.ValidationException;
import ro.cs.tao.eodata.EOData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Cosmin Cara
 */
public class ComponentDescriptor extends Identifiable {

    private String label;
    private String version;
    private String description;
    private String authors;
    private String copyright;
    private String fileLocation;
    private String workingDirectory;
    private TemplateType templateType;
    private TemplateEngine templateEngine;
    private Template template;
    private List<Variable> variables;
    private List<ParameterDescriptor> parameters;
    private SourceDescriptor[] sources;
    private TargetDescriptor[] targets;

    public ComponentDescriptor() {
        super();
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public String getCopyright() {
        return copyright;
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    public String getFileLocation() {
        return fileLocation;
    }

    public void setFileLocation(String fileLocation) {
        this.fileLocation = fileLocation;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public Template getTemplate() {
        return template;
    }

    public void setTemplate(Template template) throws TemplateException {
        if (template != null) {
            if (!getTemplateType().equals(template.getType())) {
                throw new TemplateException("Incompatible template type");
            }
            this.template = template;
            this.template.associateWith(getTemplateEngine());
        }
    }

    public List<Variable> getVariables() {
        return variables;
    }

    public void setVariables(List<Variable> variables) {
        this.variables = variables;
    }

    public List<ParameterDescriptor> getParameterDescriptors() {
        if (this.parameters == null) {
            this.parameters = new ArrayList<>();
        }
        return this.parameters;
    }

    public void setParameters(List<ParameterDescriptor> parameters) {
        this.parameters = parameters;
    }

    public SourceDescriptor[] getSources() {
        return sources;
    }

    public void setSourcesCount(int value) {
        if (this.sources == null) {
            this.sources = new SourceDescriptor[value];
        } else {
            this.sources = Arrays.copyOf(this.sources, value);
        }
    }

    public void addSource(SourceDescriptor source) {
        if (this.sources != null) {
            this.sources = Arrays.copyOf(this.sources, this.sources.length + 1);
            this.sources[this.sources.length - 1] = source;
        } else {
            this.sources = new SourceDescriptor[] { source };
        }
    }

    public void removeSource(TargetDescriptor source) {
        if (this.sources != null) {
            this.sources = Arrays.stream(this.sources)
                    .filter(s -> {
                        EOData src = s.getSource();
                        EOData ref = source.getSource();
                        return (src.getId() != null && !src.getId().equals(ref.getId())) ||
                                (src.getName() != null && !src.getName().equals(ref.getName()));
                    }).toArray(SourceDescriptor[]::new);
        }
    }

    public TargetDescriptor[] getTargets() {
        return targets;
    }

    public void setTargetCount(int value) {
        if (this.targets == null) {
            this.targets = new TargetDescriptor[value];
        } else {
            this.targets = Arrays.copyOf(this.targets, value);
        }
    }

    public void addTarget(TargetDescriptor target) {
        if (this.targets != null) {
            this.targets = Arrays.copyOf(this.targets, this.targets.length + 1);
            this.targets[this.targets.length - 1] = target;
        } else {
            this.targets = new TargetDescriptor[] { target };
        }
    }

    public void removeTarget(TargetDescriptor target) {
        if (this.targets != null) {
            this.targets = Arrays.stream(this.targets)
                    .filter(t -> {
                        EOData tar = t.getSource();
                        EOData ref = target.getSource();
                        return (tar.getId() != null && !tar.getId().equals(ref.getId())) ||
                                (tar.getName() != null && !tar.getName().equals(ref.getName()));
                    })
                    .toArray(TargetDescriptor[]::new);
        }
    }

    public TemplateType getTemplateType() {
        return templateType != null ? templateType : TemplateType.VELOCITY;
    }

    public void setTemplateType(TemplateType templateType) {
        this.templateType = templateType;
    }

    public TemplateEngine getTemplateEngine() {
        if (this.templateEngine == null) {
            this.templateEngine = EngineFactory.createInstance(getTemplateType());
        }
        return this.templateEngine;
    }

    public void validate(Map<String, Object> parameterValues) throws ValidationException {
        if (parameterValues != null) {
            final List<ParameterDescriptor> parameterDescriptors =
                    getParameterDescriptors().stream()
                            .filter(d -> parameterValues.containsKey(d.getName()))
                            .collect(Collectors.toList());
            for (ParameterDescriptor descriptor : parameterDescriptors) {
                descriptor.validate(parameterValues.get(descriptor.getName()));
            }
        }
    }

    @Override
    public String defaultName() {
        return "NewComponent";
    }

    @Override
    public ComponentDescriptor copy() {
        ComponentDescriptor newDescriptor = new ComponentDescriptor();
        newDescriptor.label = this.label;
        newDescriptor.version = this.version;
        newDescriptor.description = this.description;
        newDescriptor.authors = this.authors;
        newDescriptor.copyright = this.copyright;
        newDescriptor.fileLocation = this.fileLocation;
        newDescriptor.workingDirectory = this.workingDirectory;
        newDescriptor.templateType = this.templateType;
        if (this.template != null) {
            newDescriptor.template = this.template.copy();
        }
        if (this.variables != null) {
            newDescriptor.variables = this.variables.stream().map(Variable::copy).collect(Collectors.toList());
        }
        if (this.parameters != null) {
            newDescriptor.parameters = this.parameters.stream().map(p -> {
                ParameterDescriptor parameter = p.copy();
                parameter.setName(p.getName());
                return p;
            }).collect(Collectors.toList());
        }
        if (this.sources != null) {
            newDescriptor.sources = Arrays.copyOf(this.sources, this.sources.length);
        }
        if (this.targets != null) {
            newDescriptor.targets = Arrays.copyOf(this.targets, this.targets.length);
        }
        return newDescriptor;
    }
}