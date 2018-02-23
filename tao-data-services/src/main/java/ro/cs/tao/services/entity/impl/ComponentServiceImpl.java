package ro.cs.tao.services.entity.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.component.*;
import ro.cs.tao.component.constraints.ConstraintFactory;
import ro.cs.tao.component.template.BasicTemplate;
import ro.cs.tao.component.template.Template;
import ro.cs.tao.component.template.TemplateException;
import ro.cs.tao.component.template.TemplateType;
import ro.cs.tao.component.validation.ValidationException;
import ro.cs.tao.eodata.enums.DataFormat;
import ro.cs.tao.eodata.enums.SensorType;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.serialization.MediaType;
import ro.cs.tao.serialization.SerializationException;
import ro.cs.tao.serialization.Serializer;
import ro.cs.tao.serialization.SerializerFactory;
import ro.cs.tao.services.interfaces.ComponentService;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author Cosmin Cara
 */
@Service("componentService")
public class ComponentServiceImpl
    extends EntityService<ProcessingComponent>
        implements ComponentService {


    private static final Map<String, ProcessingComponent> fakeComponents;

    static {
        fakeComponents = new HashMap<>();
        fakeComponents.put("segmentation-cc-1", newComponent("segmentation-cc-1", "First segmentation component"));
        fakeComponents.put("segmentation-cc-2", newComponent("segmentation-cc-2", "Second segmentation component"));
        fakeComponents.put("segmentation-cc-3", newComponent("segmentation-cc-3", "Third segmentation component"));
        fakeComponents.put("segmentation-cc-4", newComponent("segmentation-cc-4", "Fourth segmentation component"));
        fakeComponents.put("segmentation-cc-5", newComponent("segmentation-cc-5", "Fifth segmentation component"));
        fakeComponents.put("segmentation-cc-6", newComponent("segmentation-cc-6", "Sixth segmentation component"));
    }

    @Autowired
    private PersistenceManager persistenceManager;
    private Logger logger = Logger.getLogger(ComponentService.class.getName());

    @Override
    public ProcessingComponent findById(String id) {
        return fakeComponents.get(id);
        /*ProcessingComponent component = null;
        try {
            component = persistenceManager.getProcessingComponentById(id);
        } catch (PersistenceException e) {
            logger.severe(e.getMessage());
        }
        return component;*/
    }

    @Override
    public List<ProcessingComponent> list() {
        //return new ArrayList<>(fakeComponents.values());
        List<ProcessingComponent> components = null;
        try {
            components = persistenceManager.getProcessingComponents();
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
        return components;
    }

    @Override
    public void save(ProcessingComponent component) {
        //fakeComponents.put(component.getId(), component);\
        if (component != null) {

            if(persistenceManager.checkIfExistsComponentById(component.getId()))
            {
                update(component);
            }
            else
            {
                try {
                    persistenceManager.saveProcessingComponent(component);
                } catch (PersistenceException e) {
                    logger.severe(e.getMessage());
                }
            }
        }
    }

    @Override
    public void update(ProcessingComponent component) {
        //fakeComponents.put(component.getId(), component);
        if (component != null) {
            try {
                persistenceManager.updateProcessingComponent(component);
            } catch (PersistenceException e) {
                logger.severe(e.getMessage());
            }
        }
    }

    @Override
    public void delete(String name) {
        //fakeComponents.remove(name);
        try {
            persistenceManager.deleteProcessingComponent(name);
        } catch (PersistenceException e) {
            logger.severe(e.getMessage());
        }
    }

    @Override
    public void validate(ProcessingComponent entity) throws ValidationException {
        super.validate(entity);
    }

    @Override
    public ProcessingComponent importFrom(MediaType mediaType, String data) throws SerializationException {
        Serializer<ProcessingComponent, String> serializer = SerializerFactory.create(ProcessingComponent.class, mediaType);
        return serializer.deserialize(new StreamSource(new StringReader(data)));
    }

    @Override
    public String exportTo(MediaType mediaType, ProcessingComponent component) throws SerializationException {
        Serializer<ProcessingComponent, String> serializer = SerializerFactory.create(ProcessingComponent.class, mediaType);
        return serializer.serialize(component);
    }

    @Override
    public List<String> getAvailableConstraints() {
        return ConstraintFactory.getAvailableConstraints();
    }

    @Override
    protected void validateFields(ProcessingComponent entity, List<String> errors) {
        String value = entity.getId();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[id] cannot be empty");
        }
        value = entity.getContainerId();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[containerId] cannot be empty");
        } else {
            try {
                if (persistenceManager.getContainerById(value) == null) {
                    errors.add("[containerId] points to a non-existing container");
                }
            } catch (PersistenceException e) {
                logger.warning(e.getMessage());
            }
        }
        value = entity.getLabel();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[label] cannot be empty");
        }
        value = entity.getVersion();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[version] cannot be empty");
        }
        value = entity.getFileLocation();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[fileLocation] cannot be empty");
        } else {
            try {
                Paths.get(value);
            } catch (InvalidPathException e) {
                errors.add("[fileLocation] has an invalid value");
            }
        }
        value = entity.getWorkingDirectory();
        if (value == null || value.trim().isEmpty()) {
            errors.add("[workingDirectory] cannot be empty");
        } else {
            try {
                Paths.get(value);
            } catch (InvalidPathException e) {
                errors.add("[workingDirectory] has an invalid value");
            }
        }
        TemplateType templateType = entity.getTemplateType();
        if (templateType == null) {
            errors.add("[templateType] cannot be determined");
        } else {
            Template template = entity.getTemplate();
            if (template == null || template.getContents() == null || template.getContents().isEmpty()) {
                errors.add("[template] cannot be empty");
            } else {
                try {
                    entity.getTemplateEngine().parse(template);
                } catch (TemplateException e) {
                    errors.add("[template] has parsing errors: " + e.getMessage());
                }
            }
        }
        List<ParameterDescriptor> parameterDescriptors = entity.getParameterDescriptors();
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

    static ProcessingComponent newComponent(String id, String label) {
        ArrayList<ParameterDescriptor> parameters = new ArrayList<>();
        parameters.add(newParameter("outmode_string",
                                    String.class,
                                    "ulco",
                                    "This allows setting the writing behaviour for the output vector file. Please note that the actual behaviour depends on the file format."));
        parameters.add(newParameter("neighbor_bool",
                                    Boolean.class,
                                    Boolean.TRUE.toString(),
                                    "Activate 8-Neighborhood connectivity (default is 4)."));
        Set<Variable> variables = new HashSet<>();
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
        ProcessingComponent component = new ProcessingComponent();
        component.setId(id);
        component.setLabel(label);
        component.setDescription("Performs segmentation of an image, and output either a raster or a vector file. In vector mode, large input datasets are supported.");
        component.setAuthors("King Arthur");
        component.setCopyright("(C) Camelot Productions");
        component.setFileLocation("E:\\OTB\\otbcli_Segmentation.bat");
        component.setWorkingDirectory("E:\\OTB");
        component.setNodeAffinity("Any");
        SourceDescriptor sourceDescriptor = new SourceDescriptor("sourceProductFile");
        DataDescriptor sourceData = new DataDescriptor();
        sourceData.setFormatType(DataFormat.RASTER);
        sourceData.setSensorType(SensorType.OPTICAL);
        sourceDescriptor.setDataDescriptor(sourceData);
        component.addSource(sourceDescriptor);
        TargetDescriptor targetDescriptor = new TargetDescriptor("out_str");
        DataDescriptor targetData = new DataDescriptor();
        targetData.setFormatType(DataFormat.RASTER);
        targetDescriptor.setDataDescriptor(targetData);
        component.addTarget(targetDescriptor);
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
        ret.setLabel(name);
        return ret;
    }
}
