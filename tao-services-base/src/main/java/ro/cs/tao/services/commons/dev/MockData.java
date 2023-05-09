package ro.cs.tao.services.commons.dev;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ro.cs.tao.component.TargetDescriptor;
import ro.cs.tao.datasource.beans.Parameter;
import ro.cs.tao.workflow.WorkflowDescriptor;

import java.io.IOException;
import java.util.*;

public class MockData {
    private static final String WF1 = "{\"id\":1,\"name\":\"OTB Resample, NDVI, TNDVI and Concatenate\",\"created\":\"2018-09-17T17:53:50.854\",\"customValues\":[],\"userName\":\"admin\",\"visibility\":\"PRIVATE\",\"status\":\"DRAFT\",\"path\":null,\"active\":true,\"nodes\":[{\"id\":2,\"name\":\"OTB Resample\",\"created\":\"2018-09-17T17:53:51.064\",\"customValues\":[{\"parameterName\":\"transform_type_id_scalex_number\",\"parameterValue\":\"0.5\"},{\"parameterName\":\"transform_type_id_scaley_number\",\"parameterValue\":\"0.5\"}],\"componentId\":\"RigidTransformResample\",\"componentType\":\"PROCESSING\",\"xCoord\":300.0,\"yCoord\":150.0,\"level\":0,\"incomingLinks\":[],\"preserveOutput\":true,\"behavior\":\"FAIL_ON_ERROR\"},{\"id\":5,\"name\":\"OTB Concatenate\",\"created\":\"2018-09-17T17:53:51.557\",\"customValues\":[],\"componentId\":\"ConcatenateImages\",\"componentType\":\"PROCESSING\",\"xCoord\":900.0,\"yCoord\":150.0,\"level\":0,\"incomingLinks\":[{\"output\":{\"id\":\"a2e26539-0fde-4854-9d7d-131a776aec79\",\"parentId\":\"ConcatenateImages\",\"name\":\"il\",\"dataDescriptor\":{\"formatType\":\"RASTER\",\"geometry\":null,\"crs\":null,\"sensorType\":null,\"dimension\":null,\"location\":null},\"constraints\":null,\"cardinality\":0},\"input\":{\"id\":\"a1a7ed16-010f-4be5-b576-a251bab7c0c8\",\"parentId\":\"RadiometricIndices\",\"name\":\"out\",\"dataDescriptor\":{\"formatType\":\"RASTER\",\"geometry\":null,\"crs\":null,\"sensorType\":null,\"dimension\":null,\"location\":\"output_otbcli_RadiometricIndices_1.tif\"},\"constraints\":null,\"cardinality\":1},\"sourceNodeId\":3},{\"output\":{\"id\":\"a2e26539-0fde-4854-9d7d-131a776aec79\",\"parentId\":\"ConcatenateImages\",\"name\":\"il\",\"dataDescriptor\":{\"formatType\":\"RASTER\",\"geometry\":null,\"crs\":null,\"sensorType\":null,\"dimension\":null,\"location\":null},\"constraints\":null,\"cardinality\":0},\"input\":{\"id\":\"a1a7ed16-010f-4be5-b576-a251bab7c0c8\",\"parentId\":\"RadiometricIndices\",\"name\":\"out\",\"dataDescriptor\":{\"formatType\":\"RASTER\",\"geometry\":null,\"crs\":null,\"sensorType\":null,\"dimension\":null,\"location\":\"output_otbcli_RadiometricIndices_1.tif\"},\"constraints\":null,\"cardinality\":1},\"sourceNodeId\":4}],\"preserveOutput\":true,\"behavior\":\"FAIL_ON_ERROR\"},{\"id\":3,\"name\":\"OTB NDVI\",\"created\":\"2018-09-17T17:53:51.265\",\"customValues\":[{\"parameterName\":\"list_str\",\"parameterValue\":\"Vegetation:NDVI\"}],\"componentId\":\"RadiometricIndices\",\"componentType\":\"PROCESSING\",\"xCoord\":600.0,\"yCoord\":0.0,\"level\":0,\"incomingLinks\":[{\"output\":{\"id\":\"f8ddb265-1ee7-4e7e-bd36-1796aaddd9da\",\"parentId\":\"RadiometricIndices\",\"name\":\"in\",\"dataDescriptor\":{\"formatType\":\"RASTER\",\"geometry\":null,\"crs\":null,\"sensorType\":null,\"dimension\":null,\"location\":null},\"constraints\":null,\"cardinality\":1},\"input\":{\"id\":\"578b3eea-2ba6-426d-95a5-55ebac16adda\",\"parentId\":\"RigidTransformResample\",\"name\":\"out\",\"dataDescriptor\":{\"formatType\":\"RASTER\",\"geometry\":null,\"crs\":null,\"sensorType\":null,\"dimension\":null,\"location\":\"output_otbcli_RigidTransformResample.tif\"},\"constraints\":null,\"cardinality\":1},\"sourceNodeId\":2}],\"preserveOutput\":true,\"behavior\":\"FAIL_ON_ERROR\"},{\"id\":4,\"name\":\"OTB TNDVI\",\"created\":\"2018-09-17T17:53:51.484\",\"customValues\":[{\"parameterName\":\"list_str\",\"parameterValue\":\"Vegetation:TNDVI\"}],\"componentId\":\"RadiometricIndices\",\"componentType\":\"PROCESSING\",\"xCoord\":600.0,\"yCoord\":300.0,\"level\":0,\"incomingLinks\":[{\"output\":{\"id\":\"f8ddb265-1ee7-4e7e-bd36-1796aaddd9da\",\"parentId\":\"RadiometricIndices\",\"name\":\"in\",\"dataDescriptor\":{\"formatType\":\"RASTER\",\"geometry\":null,\"crs\":null,\"sensorType\":null,\"dimension\":null,\"location\":null},\"constraints\":null,\"cardinality\":1},\"input\":{\"id\":\"578b3eea-2ba6-426d-95a5-55ebac16adda\",\"parentId\":\"RigidTransformResample\",\"name\":\"out\",\"dataDescriptor\":{\"formatType\":\"RASTER\",\"geometry\":null,\"crs\":null,\"sensorType\":null,\"dimension\":null,\"location\":\"output_otbcli_RigidTransformResample.tif\"},\"constraints\":null,\"cardinality\":1},\"sourceNodeId\":2}],\"preserveOutput\":true,\"behavior\":\"FAIL_ON_ERROR\"}],\"xCoord\":0.0,\"yCoord\":0.0,\"zoom\":0.0}";
    private static final String WF2 = "{\"id\":2,\"name\":\"OTB Radiometric Indices + OTB RESAMPLE workflow\",\"created\":\"2018-09-17T17:53:51.679\",\"customValues\":[],\"userName\":\"admin\",\"visibility\":\"PRIVATE\",\"status\":\"DRAFT\",\"path\":null,\"active\":true,\"nodes\":[{\"id\":7,\"name\":\"OTB RI\",\"created\":\"2018-09-17T17:53:51.694\",\"customValues\":[{\"parameterName\":\"list_str\",\"parameterValue\":\"Vegetation:RVI\"}],\"componentId\":\"RadiometricIndices\",\"componentType\":\"PROCESSING\",\"xCoord\":300.0,\"yCoord\":150.0,\"level\":0,\"incomingLinks\":[],\"preserveOutput\":true,\"behavior\":\"FAIL_ON_ERROR\"},{\"id\":8,\"name\":\"OTB Resample\",\"created\":\"2018-09-17T17:53:51.726\",\"customValues\":[{\"parameterName\":\"transform_type_id_scalex_number\",\"parameterValue\":\"0.5\"},{\"parameterName\":\"transform_type_id_scaley_number\",\"parameterValue\":\"0.5\"}],\"componentId\":\"RigidTransformResample\",\"componentType\":\"PROCESSING\",\"xCoord\":600.0,\"yCoord\":150.0,\"level\":0,\"incomingLinks\":[{\"output\":{\"id\":\"7a99962a-a752-4376-bc4c-d6e5bea39d50\",\"parentId\":\"RigidTransformResample\",\"name\":\"in\",\"dataDescriptor\":{\"formatType\":\"RASTER\",\"geometry\":null,\"crs\":null,\"sensorType\":null,\"dimension\":null,\"location\":null},\"constraints\":null,\"cardinality\":1},\"input\":{\"id\":\"a1a7ed16-010f-4be5-b576-a251bab7c0c8\",\"parentId\":\"RadiometricIndices\",\"name\":\"out\",\"dataDescriptor\":{\"formatType\":\"RASTER\",\"geometry\":null,\"crs\":null,\"sensorType\":null,\"dimension\":null,\"location\":\"output_otbcli_RadiometricIndices_1.tif\"},\"constraints\":null,\"cardinality\":1},\"sourceNodeId\":7}],\"preserveOutput\":true,\"behavior\":\"FAIL_ON_ERROR\"}],\"xCoord\":0.0,\"yCoord\":0.0,\"zoom\":0.0}";
    private static final String WF1_Params_OTB_Resample = "[{\"name\":\"interpolator_bco_radius_number\",\"type\":\"int\",\"value\":\"2\",\"valueSet\":null},{\"name\":\"interpolator_str\",\"type\":\"string\",\"value\":\"bco\",\"valueSet\":[\"nn\",\"linear\",\"bco\"]},{\"name\":\"transform_type_id_scalex_number\",\"type\":\"float\",\"value\":\"0.5\",\"valueSet\":null},{\"name\":\"transform_type_id_scaley_number\",\"type\":\"float\",\"value\":\"0.5\",\"valueSet\":null},{\"name\":\"transform_type_rotation_angle_number\",\"type\":\"float\",\"value\":\"0.0\",\"valueSet\":null},{\"name\":\"transform_type_rotation_scalex_number\",\"type\":\"float\",\"value\":\"1.0\",\"valueSet\":null},{\"name\":\"transform_type_rotation_scaley_number\",\"type\":\"float\",\"value\":\"1.0\",\"valueSet\":null},{\"name\":\"transform_type_str\",\"type\":\"string\",\"value\":\"id\",\"valueSet\":[\"id\",\"translation\",\"rotation\"]},{\"name\":\"transform_type_translation_scalex_number\",\"type\":\"float\",\"value\":\"1.0\",\"valueSet\":null},{\"name\":\"transform_type_translation_scaley_number\",\"type\":\"float\",\"value\":\"1.0\",\"valueSet\":null},{\"name\":\"transform_type_translation_tx_number\",\"type\":\"float\",\"value\":\"0.0\",\"valueSet\":null},{\"name\":\"transform_type_translation_ty_number\",\"type\":\"float\",\"value\":\"0.0\",\"valueSet\":null}]";
    private static final String WF1_Params_OTB_NDVI = "[{\"name\":\"channels_blue_number\",\"type\":\"int\",\"value\":\"1\",\"valueSet\":null},{\"name\":\"channels_green_number\",\"type\":\"int\",\"value\":\"1\",\"valueSet\":null},{\"name\":\"channels_mir_number\",\"type\":\"int\",\"value\":\"1\",\"valueSet\":null},{\"name\":\"channels_nir_number\",\"type\":\"int\",\"value\":\"1\",\"valueSet\":null},{\"name\":\"channels_red_number\",\"type\":\"int\",\"value\":\"1\",\"valueSet\":null},{\"name\":\"list_str\",\"type\":\"string\",\"value\":\"Vegetation:NDVI\",\"valueSet\":[\"ndvi\",\"tndvi\",\"rvi\",\"savi\",\"tsavi\",\"msavi\",\"msavi2\",\"gemi\",\"ipvi\",\"ndwi\",\"ndwi2\",\"mndwi\",\"ndpi\",\"ndti\",\"ri\",\"ci\",\"bi\",\"bi2\"]}],\"OTB TNDVI\":[{\"name\":\"channels_blue_number\",\"type\":\"int\",\"value\":\"1\",\"valueSet\":null},{\"name\":\"channels_green_number\",\"type\":\"int\",\"value\":\"1\",\"valueSet\":null},{\"name\":\"channels_mir_number\",\"type\":\"int\",\"value\":\"1\",\"valueSet\":null},{\"name\":\"channels_nir_number\",\"type\":\"int\",\"value\":\"1\",\"valueSet\":null},{\"name\":\"channels_red_number\",\"type\":\"int\",\"value\":\"1\",\"valueSet\":null},{\"name\":\"list_str\",\"type\":\"string\",\"value\":\"Vegetation:TNDVI\",\"valueSet\":[\"ndvi\",\"tndvi\",\"rvi\",\"savi\",\"tsavi\",\"msavi\",\"msavi2\",\"gemi\",\"ipvi\",\"ndwi\",\"ndwi2\",\"mndwi\",\"ndpi\",\"ndti\",\"ri\",\"ci\",\"bi\",\"bi2\"]}]";
    private static final String WF2_Params_OTB_RI = "[{\"name\":\"channels_blue_number\",\"type\":\"int\",\"value\":\"1\",\"valueSet\":null},{\"name\":\"channels_green_number\",\"type\":\"int\",\"value\":\"1\",\"valueSet\":null},{\"name\":\"channels_mir_number\",\"type\":\"int\",\"value\":\"1\",\"valueSet\":null},{\"name\":\"channels_nir_number\",\"type\":\"int\",\"value\":\"1\",\"valueSet\":null},{\"name\":\"channels_red_number\",\"type\":\"int\",\"value\":\"1\",\"valueSet\":null},{\"name\":\"list_str\",\"type\":\"string\",\"value\":\"Vegetation:RVI\",\"valueSet\":[\"ndvi\",\"tndvi\",\"rvi\",\"savi\",\"tsavi\",\"msavi\",\"msavi2\",\"gemi\",\"ipvi\",\"ndwi\",\"ndwi2\",\"mndwi\",\"ndpi\",\"ndti\",\"ri\",\"ci\",\"bi\",\"bi2\"]}]";
    private static final String WF2_Params_OTB_Resample = "[{\"name\":\"interpolator_bco_radius_number\",\"type\":\"int\",\"value\":\"2\",\"valueSet\":null},{\"name\":\"interpolator_str\",\"type\":\"string\",\"value\":\"bco\",\"valueSet\":[\"nn\",\"linear\",\"bco\"]},{\"name\":\"transform_type_id_scalex_number\",\"type\":\"float\",\"value\":\"0.5\",\"valueSet\":null},{\"name\":\"transform_type_id_scaley_number\",\"type\":\"float\",\"value\":\"0.5\",\"valueSet\":null},{\"name\":\"transform_type_rotation_angle_number\",\"type\":\"float\",\"value\":\"0.0\",\"valueSet\":null},{\"name\":\"transform_type_rotation_scalex_number\",\"type\":\"float\",\"value\":\"1.0\",\"valueSet\":null},{\"name\":\"transform_type_rotation_scaley_number\",\"type\":\"float\",\"value\":\"1.0\",\"valueSet\":null},{\"name\":\"transform_type_str\",\"type\":\"string\",\"value\":\"id\",\"valueSet\":[\"id\",\"translation\",\"rotation\"]},{\"name\":\"transform_type_translation_scalex_number\",\"type\":\"float\",\"value\":\"1.0\",\"valueSet\":null},{\"name\":\"transform_type_translation_scaley_number\",\"type\":\"float\",\"value\":\"1.0\",\"valueSet\":null},{\"name\":\"transform_type_translation_tx_number\",\"type\":\"float\",\"value\":\"0.0\",\"valueSet\":null},{\"name\":\"transform_type_translation_ty_number\",\"type\":\"float\",\"value\":\"0.0\",\"valueSet\":null}]";
    private static final String WF1_Output = "[{\"id\": \"9e3910c9-1ca3-4e7a-a888-844699b32022\",\"parentId\": \"ConcatenateImages\",\"name\": \"out\",\"dataDescriptor\": {\"formatType\": \"RASTER\",\"geometry\": null,\"crs\": null,\"sensorType\": null,\"dimension\": null,\"location\": \"output_otbcli_ConcatenateImages.tif\"},\"constraints\": null,\"cardinality\": 1}]";
    private static final String WF2_Output = "[{\"id\": \"da36f738-97da-4129-8ff2-670d85ae828f\",\"parentId\": \"RigidTransformResample\",\"name\": \"out\",\"dataDescriptor\": {\"formatType\": \"RASTER\",\"geometry\": null,\"crs\": null,\"sensorType\": null,\"dimension\": null,\"location\": \"output_otbcli_RigidTransformResample.tif\"},\"constraints\": null,\"cardinality\": 1}]";

    private static final List<WorkflowDescriptor> mockWorkflows;
    private static final Map<Long, Map<String, List<Parameter>>> mockParameters;
    private static final Map<Long, List<TargetDescriptor>> mockTargets;

    static {
        ObjectMapper objectMapper = new ObjectMapper();
        Hibernate5Module hibernate5Module = new Hibernate5Module();
        hibernate5Module.disable(Hibernate5Module.Feature.USE_TRANSIENT_ANNOTATION);
        hibernate5Module.enable(Hibernate5Module.Feature.FORCE_LAZY_LOADING);
        objectMapper.registerModule(hibernate5Module);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        ObjectReader reader = objectMapper.reader().forType(WorkflowDescriptor.class);
        mockWorkflows = new ArrayList<>();
        mockParameters = new HashMap<>();
        mockTargets = new HashMap<>();
        try {
            mockWorkflows.add(reader.readValue(WF1));
            mockWorkflows.add(reader.readValue(WF2));
            reader = objectMapper.reader().forType(Parameter[].class);
            LinkedHashMap<String, List<Parameter>> parameters = new LinkedHashMap<>();
            List<Parameter> parameterList = new ArrayList<>();
            Collections.addAll(parameterList, reader.readValue(WF1_Params_OTB_Resample));
            parameters.put("OTB Resample", parameterList);
            parameterList = new ArrayList<>();
            Collections.addAll(parameterList, reader.readValue(WF1_Params_OTB_NDVI));
            parameters.put("OTB NDVI", parameterList);
            parameters.put("OTB Concatenate", new ArrayList<>());
            mockParameters.put(1L, parameters);

            parameters = new LinkedHashMap<>();
            parameterList = new ArrayList<>();
            Collections.addAll(parameterList, reader.readValue(WF2_Params_OTB_RI));
            parameters.put("OTB RI", parameterList);
            parameterList = new ArrayList<>();
            Collections.addAll(parameterList, reader.readValue(WF2_Params_OTB_Resample));
            parameters.put("OTB Resample", parameterList);

            mockParameters.put(2L, parameters);
            List<TargetDescriptor> targets = new ArrayList<>();
            reader = objectMapper.reader().forType(TargetDescriptor[].class);
            Collections.addAll(targets, reader.readValue(WF1_Output));
            mockTargets.put(1L, targets);
            targets = new ArrayList<>();
            Collections.addAll(targets, reader.readValue(WF2_Output));
            mockTargets.put(2L, targets);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static List<WorkflowDescriptor> getMockWorkflows() { return mockWorkflows; }

    public static Map<Long, Map<String, List<Parameter>>> getMockParameters() { return mockParameters; }

    public static Map<Long, List<TargetDescriptor>> getMockOutputs() { return mockTargets; }
}
