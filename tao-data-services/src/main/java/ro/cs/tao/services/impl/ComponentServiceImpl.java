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
import ro.cs.tao.component.template.TemplateType;
import ro.cs.tao.services.interfaces.ComponentService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Cosmin Cara
 */
@Service("componentService")
public class ComponentServiceImpl implements ComponentService {

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
