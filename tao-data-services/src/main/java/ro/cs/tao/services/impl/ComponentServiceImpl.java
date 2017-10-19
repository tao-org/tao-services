package ro.cs.tao.services.impl;

import org.springframework.stereotype.Service;
import ro.cs.tao.component.ParameterDescriptor;
import ro.cs.tao.component.ProcessingComponent;
import ro.cs.tao.component.SourceDescriptor;
import ro.cs.tao.component.TaoComponent;
import ro.cs.tao.component.TargetDescriptor;
import ro.cs.tao.component.Variable;
import ro.cs.tao.component.constraints.SensorConstraint;
import ro.cs.tao.component.template.BasicTemplate;
import ro.cs.tao.component.template.Template;
import ro.cs.tao.component.template.TemplateException;
import ro.cs.tao.component.template.TemplateType;
import ro.cs.tao.component.validation.ValidationException;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.services.interfaces.ComponentService;

import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Cosmin Cara
 */
@Service("componentService")
public class ComponentServiceImpl
    extends ServiceBase<TaoComponent>
        implements ComponentService {

    private static final Map<String, TaoComponent> fakeComponents;

    static {
        fakeComponents = new HashMap<>();
        fakeComponents.put("segmentation-cc-1", newComponent("segmentation-cc-1", "First segmentation component"));
        fakeComponents.put("segmentation-cc-2", newComponent("segmentation-cc-2", "Second segmentation component"));
    }

    @Override
    public TaoComponent findById(String name) {
        return fakeComponents.get(name);
    }

    @Override
    public List<TaoComponent> list() {
        return new ArrayList<>(fakeComponents.values());
    }

    @Override
    public void save(TaoComponent component) {
        fakeComponents.put(component.getId(), component);
    }

    @Override
    public void update(TaoComponent component) {
        // TODO
    }

    @Override
    public void delete(String name) {
        fakeComponents.remove(name);
    }

    @Override
    public void validate(TaoComponent entity) throws ValidationException {
        if (entity instanceof DataSourceComponent) {
            throw new ValidationException("Data source components cannot be modified");
        }
        super.validate(entity);
    }

    @Override
    protected void validateFields(TaoComponent entity, List<String> errors) {
        String value = entity.getId();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[id] cannot be empty");
        }
        value = entity.getLabel();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[label] cannot be empty");
        }
        value = entity.getVersion();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[version] cannot be empty");
        }
        if (entity instanceof ProcessingComponent) {
            ProcessingComponent component = (ProcessingComponent) entity;
            value = component.getFileLocation();
            if (value == null || value.trim().isEmpty()) {
                errors.add("[fileLocation] cannot be empty");
            } else {
                try {
                    Paths.get(value);
                } catch (InvalidPathException e) {
                    errors.add("[fileLocation] has an invalid value");
                }
            }
            value = component.getWorkingDirectory();
            if (value == null || value.trim().isEmpty()) {
                errors.add("[workingDirectory] cannot be empty");
            } else {
                try {
                    Paths.get(value);
                } catch (InvalidPathException e) {
                    errors.add("[workingDirectory] has an invalid value");
                }
            }
            TemplateType templateType = component.getTemplateType();
            if (templateType == null) {
                errors.add("[templateType] cannot be determined");
            } else {
                Template template = component.getTemplate();
                if (template == null || template.getContents() == null || template.getContents().isEmpty()) {
                    errors.add("[template] cannot be empty");
                } else {
                    try {
                        component.getTemplateEngine().parse(template);
                    } catch (TemplateException e) {
                        errors.add("[template] has parsing errors: " + e.getMessage());
                    }
                }
            }
            List<ParameterDescriptor> parameterDescriptors = component.getParameterDescriptors();
            if (parameterDescriptors != null) {
                for (ParameterDescriptor descriptor : parameterDescriptors) {
                    String descriptorId = descriptor.getId();
                    if (descriptorId == null || descriptorId.trim().isEmpty()) {
                        errors.add("Invalid parameter found (missing id)");
                        continue;
                    }
                    value = descriptor.getLabel();
                    if (value == null || value.trim().isEmpty()) {
                        errors.add(String.format("[$%s] label cannot be empty", descriptorId));
                    }
                    Class<?> dataType = descriptor.getDataType();
                    if (dataType == null) {
                        errors.add(String.format("[$%s] cannot determine type", descriptorId));
                    }
                    if (Date.class.equals(dataType)) {
                        value = descriptor.getFormat();
                        if (value == null || value.trim().isEmpty()) {
                            errors.add(String.format("[$%s] format for date parameter not specified", descriptorId));
                        }
                    }
                    value = descriptor.getDefaultValue();
                    if (descriptor.isNotNull() && (value == null || value.trim().isEmpty())) {
                        errors.add(String.format("[$%s] is mandatory, but has no default value", descriptorId));
                    }
                }
            }
        }
    }

    private static ProcessingComponent newComponent(String id, String label) {
        ArrayList<ParameterDescriptor> parameters = new ArrayList<>();
        parameters.add(newParameter("outmode_string",
                                    String.class,
                                    "ulco",
                                    "This allows setting the writing behaviour for the output vector file. Please note that the actual behaviour depends on the file format."));
        parameters.add(newParameter("neighbor_bool",
                                    Boolean.class,
                                    Boolean.TRUE.toString(),
                                    "Activate 8-Neighborhood connectivity (default is 4)."));
        ArrayList<Variable> variables = new ArrayList<>();
        variables.add(new Variable("ITK_AUTOLOAD_PATH", "E:\\OTB\\bin"));
        Template template = new BasicTemplate();
        template.setName("segmentation-cc-template.vm");
        template.setTemplateType(TemplateType.VELOCITY);
        template.setContents("-in\n" +
                                     "$sourceProductFile\n" +
                                     "-filter.cc.expr\n" +
                                     "$expr_string\n" +
                                     "-mode.vector.out\n" +
                                     "$out_str\n" +
                                     "-mode.vector.outmode\n" +
                                     "$outmode_string\n" +
                                     "-mode.vector.neighbor\n" +
                                     "$neighbor_bool\n" +
                                     "-mode.vector.stitch\n" +
                                     "$stitch_bool\n" +
                                     "-mode.vector.minsize\n" +
                                     "$minsize_int\n" +
                                     "-mode.vector.simplify\n" +
                                     "$simplify_float\n" +
                                     "-mode.vector.layername\n" +
                                     "$layername_string\n" +
                                     "-mode.vector.fieldname\n" +
                                     "$fieldname_string\n" +
                                     "-mode.vector.tilesize\n" +
                                     "$tilesize_int\n" +
                                     "-mode.vector.startlabel\n" +
                                     "$startlabel_int", false);
        SourceDescriptor sourceDescriptor = new SourceDescriptor("sourceProductFile");
        sourceDescriptor.addConstraint(new SensorConstraint());
        ProcessingComponent component = new ProcessingComponent();
        component.setId(id);
        component.setLabel(label);
        component.setDescription("Performs segmentation of an image, and output either a raster or a vector file. In vector mode, large input datasets are supported.");
        component.setAuthors("King Arthur");
        component.setCopyright("(C) Camelot Productions");
        component.setFileLocation("E:\\OTB\\otbcli_Segmentation.bat");
        component.setWorkingDirectory("E:\\OTB");
        component.setNodeAffinity("Any");
        component.setSources(new SourceDescriptor[] {sourceDescriptor});
        component.setTargets(new TargetDescriptor[] { new TargetDescriptor("out_str") });
        component.setVersion("1.0");
        component.setParameterDescriptors(parameters);
        component.setVariables(variables);
        component.setTemplateType(TemplateType.VELOCITY);
        component.setTemplate(template);
        return component;
    }

    private static ParameterDescriptor newParameter(String name, Class<?> clazz, String defaultValue, String description) {
        ParameterDescriptor ret = new ParameterDescriptor(name);
        ret.setDataType(clazz);
        ret.setDefaultValue(defaultValue);
        ret.setDescription(description);
        return ret;
    }
}
